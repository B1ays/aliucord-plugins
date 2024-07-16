package ru.blays.plugins.translate.strings

object StringsEn: IStrings {
    override val plugin_name: String = "Translate"

    override val messageTranslated: String = "Translated message"
    override val translated: String = "Translated:"

    override val action_translate: String = "Translate text"
    override val action_showOriginal: String = "Show original"
    override val action_save: String = "Save"

    override val plugin_settings_defaultLanguage: String = "Language to translate messages to by default:"
    override val plugin_settings_defaultLanguage_saved: String = "Saved translate settings"
    override val plugin_settings_supportedLanguageCodes: String = "Supported language:"
    override val plugin_settings_cleanText: String = "Clean text from markup"
}