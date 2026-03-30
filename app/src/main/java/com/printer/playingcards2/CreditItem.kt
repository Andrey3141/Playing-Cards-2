package com.printer.playingcards2

data class CreditItem(
    val role: String,
    val name: String,
    val iconResId: Int,
    val link: String? = null  // Добавляем поле для ссылки
)