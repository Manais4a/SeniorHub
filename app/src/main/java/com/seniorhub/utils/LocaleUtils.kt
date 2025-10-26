package com.seniorhub.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleUtils {

    fun getLanguageCodeFromPrefs(): String {
        return PreferenceManager.getInstance().language
    }

    fun applyAppLocale(languageCode: String) {
        val normalized = when (languageCode) {
            "tl" -> "tl"
            "ceb" -> "ceb"
            "en", "" -> "en"
            else -> languageCode
        }
        val locales = LocaleListCompat.forLanguageTags(normalized)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}


