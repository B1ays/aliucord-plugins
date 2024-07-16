package ru.blays.plugins.translate

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.discord.utilities.color.ColorCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lytefast.flexinput.R
import ru.blays.plugins.translate.utils.getStrings


class PluginSettings(private val settings: SettingsAPI) : SettingsPage() {
    override fun onViewBound(view: View?) {
        super.onViewBound(view)
        val ctx = requireContext()
        val strings = ctx.getStrings()
        setActionBarTitle(
            strings.plugin_name
        )

        val textColor = ColorCompat.getThemedColor(ctx, R.b.colorOnPrimary)
        val backgroundColor = ColorCompat.getThemedColor(ctx, R.b.colorSurface)

        val cleanTextSwitch = SwitchMaterial(ctx).apply {
            setPadding(0, 0, 0, 40)
            text = strings.plugin_settings_cleanText
            setTextColor(textColor)
            isChecked = settings.getBool(SETTINGS_KEY_CLEAN_TEXT, true)
            setOnCheckedChangeListener { _, isChecked ->
                settings.setBool(SETTINGS_KEY_CLEAN_TEXT, isChecked)
            }
        }

        val card = CardView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(40, 40, 40, 40)
            setContentPadding(20, 20, 20, 20)
            setCardBackgroundColor(backgroundColor)
            radius = 20F
            addView(
                TextView(ctx).apply {
                    text = "${strings.plugin_settings_defaultLanguage} " +
                        settings.getString(SETTINGS_KEY_DEFAULT_LANGUAGE, "en")
                    setTextColor(textColor)
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                }
            )
        }

        val titleText = TextView(ctx).apply {
            text = strings.plugin_settings_supportedLanguageCodes
            setPadding(0, 30, 0, 30)
            setTextColor(textColor)
        }

        addView(cleanTextSwitch)
        addView(card)
        //addView(button)
        addView(titleText)
        languageCodes.forEach { (name, code) ->
            addView(
                TextView(ctx).apply {
                    text = name
                    setPadding(0, 15, 0, 15)
                    setTextColor(textColor)
                    //setBackgroundColor(textBackground)
                    setOnClickListener {
                        settings.setString("defaultLanguage", code)
                        Utils.showToast(
                            strings.plugin_settings_defaultLanguage_saved
                        )
                        close()
                    }
                }
            )
            addView(
                View(ctx).apply {
                    setPadding(0, 6, 0, 6)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        2
                    )
                    setBackgroundColor(backgroundColor)
                }
            )
        }
    }
}