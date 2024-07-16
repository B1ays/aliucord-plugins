package ru.blays.plugins.translate

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.api.commands.ApplicationCommandType
import com.discord.databinding.WidgetChatListActionsBinding
import com.discord.utilities.textprocessing.node.EditedMessageNode
import com.discord.utilities.view.text.SimpleDraweeSpanTextView
import com.discord.widgets.chat.list.WidgetChatList
import com.discord.widgets.chat.list.actions.WidgetChatListActions
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.MessageEntry
import com.facebook.drawee.span.DraweeSpanStringBuilder
import com.lytefast.flexinput.R
import org.json.JSONArray
import ru.blays.plugins.translate.utils.TranslateUnescaper
import ru.blays.plugins.translate.utils.getStrings
import ru.blays.plugins.translate.utils.rerenderMessage
import java.lang.reflect.Field

@AliucordPlugin
class Translate : Plugin() {
    private var pluginIcon: Drawable? = null
    private val translatedMessages = mutableMapOf<Long, TranslateResult.Success>()
    private var chatList: WidgetChatList? = null

    init {
        settingsTab = SettingsTab(PluginSettings::class.java).withArgs(settings)
    }

    override fun load(ctx: Context) {
        pluginIcon = ContextCompat.getDrawable(ctx, R.e.ic_locale_24dp)!!
    }

    override fun start(context: Context) {
        patchMessageContextMenu()
        patchProcessMessageText()
        commands.registerCommand(
            "translate",
            "Translates text from one language to another, sends by default",
            listOf(
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    KEY_TEXT,
                    "The text to translate"
                ),
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    KEY_TO,
                    "The language to translate to (default en, must be a language code described in plugin settings)",
                    choices = languageCodeChoices
                ),
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    KEY_FROM,
                    "The language to translate from (default auto, must be a language code described in plugin settings)",
                    choices = languageCodeChoices
                ),
                Utils.createCommandOption(
                    ApplicationCommandType.BOOLEAN,
                    KEY_SEND,
                    "Whether or not to send the message in chat (default true)"
                )
            )
        ) { commandContext ->
            val translateResult = translateMessage(
                commandContext.getRequiredString(KEY_TEXT),
                commandContext.getString(KEY_FROM),
                commandContext.getString(KEY_TO)
            )
            return@registerCommand when (translateResult) {
                is TranslateResult.Error -> {
                    CommandsAPI.CommandResult(
                        translateResult.toString(),
                        null,
                        false
                    )
                }

                is TranslateResult.Success -> {
                    CommandsAPI.CommandResult(
                        translateResult.translatedText,
                        null,
                        commandContext.getBoolOrDefault(KEY_SEND, true)
                    )
                }
            }
        }
    }

    private fun DraweeSpanStringBuilder.setTranslated(
        translateData: TranslateResult.Success,
        context: Context
    ) {
        val strings = context.getStrings()
        this.replace(
            0,
            length - 1,
            translateData.translatedText
        )
        val textEnd = this.length
        this.append(" (${strings.translated} ${translateData.sourceLanguage} -> ${translateData.translatedLanguage})")
        this.setSpan(
            RelativeSizeSpan(0.75f),
            textEnd,
            this.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (textEnd != this.length) {
            this.setSpan(
                EditedMessageNode.Companion.`access$getForegroundColorSpan`(
                    EditedMessageNode.Companion,
                    context
                ),
                textEnd,
                this.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun patchProcessMessageText() {
        patcher.patch(
            WidgetChatList::class.java.getDeclaredConstructor(),
            Hook {
                chatList = it.thisObject as WidgetChatList
            }
        )

        val mDraweeStringBuilder: Field =
            SimpleDraweeSpanTextView::class.java.getDeclaredField("mDraweeStringBuilder")
        mDraweeStringBuilder.isAccessible = true
        patcher.patch(
            WidgetChatListAdapterItemMessage::class.java,
            "processMessageText",
            arrayOf(SimpleDraweeSpanTextView::class.java, MessageEntry::class.java),
            Hook {
                val messageEntry = it.args[1] as MessageEntry
                val message = messageEntry.message ?: return@Hook
                val id = message.id
                val translateData = translatedMessages[id] ?: return@Hook
                if (translateData.showingOriginal) return@Hook
                if (translateData.sourceText != message.content) {
                    translatedMessages.remove(id)
                    return@Hook
                }
                val textView = it.args[0] as SimpleDraweeSpanTextView
                val builder = mDraweeStringBuilder[textView] as? DraweeSpanStringBuilder
                    ?: return@Hook
                val context = textView.context
                builder.setTranslated(translateData, context)
                textView.setDraweeSpanStringBuilder(builder)
            }
        )
    }

    private fun patchMessageContextMenu() {
        val viewId = View.generateViewId()
        val messageContextMenu = WidgetChatListActions::class.java
        val getBinding =
            messageContextMenu.getDeclaredMethod("getBinding").apply { isAccessible = true }

        patcher.patch(
            messageContextMenu.getDeclaredMethod(
                "configureUI",
                WidgetChatListActions.Model::class.java
            ),
            Hook { hookParam ->
                val menu = hookParam.thisObject as WidgetChatListActions
                val binding = getBinding.invoke(menu) as WidgetChatListActionsBinding
                val translateButton = binding.a.findViewById<TextView>(viewId)
                translateButton.setOnClickListener { view ->
                    val message = (hookParam.args[0] as WidgetChatListActions.Model).message
                    val translationEntry = translatedMessages[message.id]
                    val strings = view.context.getStrings()

                    if (translationEntry == null) {
                        // If not translated yet, fetch and cache the translation, then rerender the message
                        Utils.threadPool.execute {
                            when (
                                val translateResult = translateMessage(message.content)
                            ) {
                                is TranslateResult.Error -> {
                                    Utils.showToast(translateResult.toString(), true)
                                    return@execute
                                }

                                is TranslateResult.Success -> {
                                    translatedMessages[message.id] = translateResult
                                    chatList?.rerenderMessage(message.id)
                                    Utils.showToast(strings.messageTranslated)
                                    menu.dismiss()
                                }
                            }
                        }
                    } else {
                        // If translated, then no need to translate anything, so just flip the showingOriginal property and rerender
                        translationEntry.showingOriginal = !translationEntry.showingOriginal
                        chatList?.rerenderMessage(message.id)
                        menu.dismiss()
                    }
                }
            }
        )

        patcher.patch(
            messageContextMenu,
            "onViewCreated",
            arrayOf(View::class.java, Bundle::class.java),
            Hook {
                val linearLayout = (it.args[0] as NestedScrollView).getChildAt(0) as LinearLayout
                val context = linearLayout.context
                val strings = context.getStrings()
                val messageId =
                    WidgetChatListActions.`access$getMessageId$p`(it.thisObject as WidgetChatListActions)
                linearLayout.addView(
                    TextView(
                        context,
                        null,
                        0,
                        R.i.UiKit_Settings_Item_Icon
                    ).apply {
                        val translationEntry = translatedMessages[messageId]
                        id = viewId
                        text = if (translationEntry == null || translationEntry.showingOriginal) {
                            strings.action_translate
                        } else {
                            strings.action_showOriginal
                        }
                        setCompoundDrawablesRelativeWithIntrinsicBounds(
                            pluginIcon,
                            null,
                            null,
                            null
                        )
                    }
                )
            }
        )
    }

    override fun stop(context: Context?) = patcher.unpatchAll()

    private fun translateMessage(
        text: String,
        from: String? = null,
        to: String? = null
    ): TranslateResult {
        val textToTranslate = if(settings.getBool(SETTINGS_KEY_CLEAN_TEXT, true)) {
            cleanTextForTranslation(text)
        } else {
            text
        }

        val toLang = to ?: settings.getString("defaultLanguage", "en")
        val fromLang = from ?: "auto"
        val queryBuilder = Http.QueryBuilder(TRANSLATE_API_URL).apply {
            append("client", "gtx")
            append("sl", fromLang)
            append("tl", toLang)
            append("dt", "t")
            append("q", textToTranslate)
        }
        val translatedJsonReq = Http.Request(queryBuilder.toString(), "GET").apply {
            setHeader("Content-Type", "application/json")
            setHeader("User-Agent", USER_AGENT)
        }.execute()

        if (!translatedJsonReq.ok()) {
            return when (translatedJsonReq.statusCode) {
                429 -> TranslateResult.Error(
                    errorCode = 429,
                    errorText = "Translate API ratelimit reached. Please try again later."
                )

                else -> TranslateResult.Error(
                    errorCode = translatedJsonReq.statusCode,
                    errorText = "An unknown error occurred. Please report this to the developer of Translate."
                )
            }
        }

        val jsonText = translatedJsonReq.text()
        val parsedJson = JSONArray(jsonText)
        val translatedSections = parsedJson.getJSONArray(0)

        val stringBuilder = StringBuilder()
        for (index in 0 until translatedSections.length()) {
            stringBuilder.append(
                translatedSections.getJSONArray(index).getString(0)
            )
        }

        val translatedText = stringBuilder.toString()
        val unescapedText = translatedText.unescape()

        return TranslateResult.Success(
            sourceLanguage = parsedJson.getString(2),
            translatedLanguage = toLang,
            sourceText = text,
            translatedText = unescapedText
        )
    }

    private fun String.unescape(): String {
        return TranslateUnescaper.unescape(this)
    }

    private fun cleanTextForTranslation(text: String): String {
        return text.replace(cleanTextRegex, "").trim()
    }

    companion object {
        private val cleanTextRegex = Regex("<[^>]*>")
    }
}
