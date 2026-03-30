package com.printer.playingcards2

data class GameCard(
    val originalCard: Card,
    var currentHealth: Int,
    var currentAttack: Int,
    var currentDefense: Int,
    var isAlive: Boolean = true,
    var extraAttackUsed: Boolean = false,
    var isStunned: Boolean = false,
    var damageReductionUsed: Boolean = false,
    var attackBuffPermanent: Boolean = false,  // От "Единение с природой"
    var globalAttackBuffApplied: Boolean = false  // От "Роланд Азер"
) {
    constructor(card: Card) : this(
        originalCard = card,
        currentHealth = card.health,
        currentAttack = card.attack,
        currentDefense = card.defense,
        isAlive = true,
        extraAttackUsed = false,
        isStunned = false,
        damageReductionUsed = false,
        attackBuffPermanent = false,
        globalAttackBuffApplied = false
    )

    fun resetExtraAttackFlag() {
        extraAttackUsed = false
    }

    fun resetStun() {
        isStunned = false
    }

    fun resetDamageReductionFlag() {
        damageReductionUsed = false
    }

    fun resetGlobalAttackBuffFlag() {
        globalAttackBuffApplied = false
    }
}