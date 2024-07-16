package ru.blays.plugins.translate.utils

import android.annotation.SuppressLint
import android.content.Context
import ru.blays.plugins.translate.strings.IStrings
import ru.blays.plugins.translate.strings.StringsEn
import ru.blays.plugins.translate.strings.StringsRu

@SuppressLint("DiscouragedApi")
fun Context.getStrings(): IStrings {
    return when(currentLanguage) {
        "ru" -> StringsRu
        "en" -> StringsEn
        else -> StringsEn
    }
}

private inline val Context.currentLanguage: String
    get() = resources.configuration.locales[0].language