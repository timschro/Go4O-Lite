package com.go4o.lite.data.model

enum class OverlayStyle { TEXT, SMILEY }

enum class Language(val displayName: String) {
    EN("English"),
    DE("Deutsch"),
    FR("Français"),
    NL("Nederlands"),
    ES("Español"),
    IT("Italiano"),
    CS("Čeština")
}

data class AppSettings(
    val overlayDurationSeconds: Int = 10,
    val overlayStyle: OverlayStyle = OverlayStyle.TEXT,
    val language: Language = Language.EN,
    val keepScreenOn: Boolean = true,
    val smileyPass: String = "\uD83D\uDE0A",
    val smileyFail: String = "\uD83D\uDE1E"
)
