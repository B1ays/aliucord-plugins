package ru.blays.plugins.translate.strings

interface IStrings {
    val plugin_name: String

    val messageTranslated: String
    val translated: String

    val action_translate: String
    val action_showOriginal: String
    val action_save: String

    val plugin_settings_defaultLanguage: String
    val plugin_settings_defaultLanguage_saved: String
    val plugin_settings_supportedLanguageCodes: String
    val plugin_settings_cleanText: String
}