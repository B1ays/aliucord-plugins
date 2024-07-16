package ru.blays.plugins.translate

sealed class TranslateResult {
    data class Success(
        val sourceLanguage: String,
        val translatedLanguage: String,
        val sourceText: String,
        val translatedText: String,
        var showingOriginal: Boolean = false
    ) : TranslateResult()

    data class Error(
        val errorCode: Int? = null,
        val errorText: String
    ) : TranslateResult() {
        override fun toString(): String {
            var result = errorText
            if(errorCode != null) {
                result += " ($errorCode)"
            }
            return result
        }
    }
}