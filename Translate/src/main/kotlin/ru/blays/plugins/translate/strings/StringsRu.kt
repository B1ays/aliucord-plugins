package ru.blays.plugins.translate.strings

object StringsRu: IStrings {
    override val plugin_name: String = "Переводчик"

    override val messageTranslated: String = "Сообщение переведено"
    override val translated: String = "Переведено:"

    override val action_translate: String = "Перевести текст"
    override val action_showOriginal: String = "Показать оригинал"
    override val action_save: String = "Сохранить"

    override val plugin_settings_defaultLanguage: String = "Язык перевода по умолчанию:"
    override val plugin_settings_defaultLanguage_saved: String = "Настройки перевода сохраненны"
    override val plugin_settings_supportedLanguageCodes: String = "Поддерживаемые языки:"
    override val plugin_settings_cleanText: String = "Очищать текст от разметки"
}