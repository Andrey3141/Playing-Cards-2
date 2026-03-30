package com.printer.playingcards2

data class PromoCode(
    val code: String,
    val description: String,
    val isActive: Boolean = true,
    val reward: String
)