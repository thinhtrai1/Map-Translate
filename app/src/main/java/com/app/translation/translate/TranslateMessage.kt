package com.app.translation.translate

import java.util.*

class TranslateMessage(
    val source: String,
    val target: String,
    val china: String,
    val japan: String,
    val sourceLocale: Locale,
    val targetLocale: Locale
)