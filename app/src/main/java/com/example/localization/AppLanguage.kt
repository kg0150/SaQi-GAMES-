package com.example.localization

enum class AppLanguage(val code: String, val nativeLabel: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी"),
    URDU("ur", "اردو");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
