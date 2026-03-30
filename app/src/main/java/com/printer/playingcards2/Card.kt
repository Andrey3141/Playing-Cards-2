package com.printer.playingcards2

data class Card(
    val id: Int,
    val name: String,
    val photoResId: Int,
    val rarity: Rarity,
    val description: String,
    val specialFeature: String,
    val health: Int,
    val attack: Int,
    val defense: Int,
    val category: CardCategory,
    val isUnlocked: Boolean = true,
    var animationActivated: Boolean = false  // Для Lottie анимации
)

enum class Rarity(val displayName: String, val colorResId: Int) {
    COMMON("Обычный", R.color.rarity_common),
    RARE("Редкий", R.color.rarity_rare),
    SUPER_RARE("Сверхредкий", R.color.rarity_super_rare),
    EPIC("Эпический", R.color.rarity_epic),
    MYTHIC("Мифический", R.color.rarity_mythic),
    LEGENDARY("Легендарный", R.color.rarity_legendary),
    CUSTOM("Пользовательский", R.color.rarity_custom)
}

enum class CardCategory {
    DECK,           // Колода
    CATEGORY,       // Карты
    UNAVAILABLE     // Недоступно
}