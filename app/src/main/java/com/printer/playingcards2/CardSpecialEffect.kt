package com.printer.playingcards2

import kotlin.random.Random

object CardSpecialEffect {

    data class SpecialEffectInfo(
        val iconResId: Int = R.drawable.ic_sword,
        val description: String,
        val cardId: Int
    )

    enum class EffectType {
        EXTRA_ATTACK,      // Повторная атака
        STUN,              // Оглушение
        DOUBLE_ATTACK,     // Удвоение атаки
        DAMAGE_REDUCTION,  // Снижение получаемого урона
        ATTACK_BUFF,       // Бафф атаки
        HEAL,              // Лечение
        NATURE_BUFF,       // Бафф природы
        GLOBAL_ATTACK_BUFF, // Глобальный бафф атаки
        TEA_BUFF           // Бафф чая
    }

    data class TriggeredEffect(
        val type: EffectType,
        val info: SpecialEffectInfo,
        val targetCard: GameCard? = null,
        val attackMultiplier: Int = 1,
        val attackBonus: Int = 0,
        val damageReduction: Float = 1f,
        val healAmount: Int = 0
    )

    /**
     * Проверяет сработавшие эффекты при атаке
     */
    fun checkExtraAttackTriggers(attacker: GameCard, allCards: List<GameCard>): List<SpecialEffectInfo> {
        val triggeredEffects = mutableListOf<SpecialEffectInfo>()

        // 1. "Горохострел" (id=1) - повторная атака
        if (attacker.originalCard.id == 1 && !attacker.extraAttackUsed) {
            val chance = Random.nextInt(100)
            if (chance < 20) {
                triggeredEffects.add(SpecialEffectInfo(
                    iconResId = R.drawable.ic_sword,
                    description = "С вероятностью 20% атакует повторно (Горохострел)",
                    cardId = 1
                ))
                attacker.extraAttackUsed = true
            }
        }

        // 2. "Древний сожитель" (id=2) - повторная атака для всех
        val ancientCompanion = allCards.find { it.originalCard.id == 2 && it.isAlive && !it.extraAttackUsed }
        if (ancientCompanion != null) {
            val chance = Random.nextInt(100)
            if (chance < 20) {
                triggeredEffects.add(SpecialEffectInfo(
                    iconResId = R.drawable.ic_sword,
                    description = "С вероятностью 20% все карты атакуют повторно (Древний сожитель)",
                    cardId = 2
                ))
                ancientCompanion.extraAttackUsed = true
            }
        }

        return triggeredEffects
    }

    /**
     * Проверяет эффект оглушения при атаке
     */
    fun checkStunTrigger(attacker: GameCard, allEnemyCards: List<GameCard>): GameCard? {
        if (attacker.originalCard.id == 3 && attacker.isAlive) {
            val chance = Random.nextInt(100)
            if (chance < 35) {
                val aliveEnemies = allEnemyCards.filter { it.isAlive }
                if (aliveEnemies.isNotEmpty()) {
                    return aliveEnemies.random()
                }
            }
        }
        return null
    }

    /**
     * Проверяет эффект удвоения атаки для "Красный дьявол" (id=4)
     */
    fun checkDoubleAttackTrigger(attacker: GameCard): Pair<Int, Boolean> {
        var attackMultiplier = 1
        var defenseChanged = false

        if (attacker.originalCard.id == 4 && attacker.isAlive) {
            val chance = Random.nextInt(100)
            if (chance < 50) {
                attackMultiplier = 2
                attacker.currentDefense = maxOf(attacker.currentDefense - 5, 0)
                defenseChanged = true
            }
        }

        return Pair(attackMultiplier, defenseChanged)
    }

    /**
     * Проверяет эффект снижения урона для "Сын депутата" (id=5)
     */
    fun checkDamageReduction(target: GameCard): Float {
        var damageMultiplier = 1f

        if (target.originalCard.id == 5 && target.isAlive && !target.damageReductionUsed) {
            val chance = Random.nextInt(100)
            if (chance < 80) {
                damageMultiplier = 0.5f
                target.isStunned = true
                target.damageReductionUsed = true
            }
        }

        return damageMultiplier
    }

    /**
     * Проверяет эффект усиления атаки для "Мини Пекка" (id=6)
     */
    fun checkAttackBuffTrigger(attacker: GameCard): Pair<Int, Boolean> {
        var attackBonus = 0
        var defenseReduced = false

        if (attacker.originalCard.id == 6 && attacker.isAlive) {
            val chance = Random.nextInt(100)
            if (chance < 65) {
                attackBonus = 10
                attacker.currentDefense = maxOf(attacker.currentDefense - 5, 0)
                defenseReduced = true
            }
        }

        return Pair(attackBonus, defenseReduced)
    }

    /**
     * Проверяет эффект лечения для "Мастер-класс" (id=7)
     */
    fun checkHealTrigger(attacker: GameCard): Int {
        var healAmount = 0

        if (attacker.originalCard.id == 7 && attacker.isAlive) {
            val chance = Random.nextInt(100)
            if (chance < 80) {
                healAmount = 15
                attacker.isStunned = true
            }
        }

        return healAmount
    }

    /**
     * Проверяет эффект "Единение с природой" (id=8)
     */
    fun checkNatureBuffTrigger(attacker: GameCard): Triple<Int, Boolean, Boolean> {
        var attackBonus = 0
        var defenseReduced = false
        var healthReduced = false

        if (attacker.originalCard.id == 8 && attacker.isAlive) {
            val buffChance = Random.nextInt(100)
            if (buffChance < 50) {
                attackBonus = 10
                attacker.currentAttack += attackBonus
                attacker.attackBuffPermanent = true

                val debuffChance = Random.nextInt(100)
                if (debuffChance < 40) {
                    val newDefense = attacker.currentDefense - 5
                    attacker.currentDefense = maxOf(newDefense, 0)
                    defenseReduced = true

                    val newHealth = attacker.currentHealth - 5
                    attacker.currentHealth = maxOf(newHealth, 1)
                    healthReduced = true
                }
            }
        }

        return Triple(attackBonus, defenseReduced, healthReduced)
    }

    /**
     * Проверяет эффект "Роланд Азер" (id=9) - глобальный бафф атаки
     */
    fun checkGlobalAttackBuffTrigger(attacker: GameCard, allCards: List<GameCard>): Boolean {
        var triggered = false

        if (attacker.originalCard.id == 9 && attacker.isAlive) {
            val chance = Random.nextInt(100)
            if (chance < 60) {
                triggered = true
                allCards.forEach { card ->
                    if (card.isAlive && !card.globalAttackBuffApplied) {
                        val bonus = (card.originalCard.attack * 0.39).toInt()
                        card.currentAttack += bonus
                        card.globalAttackBuffApplied = true
                    }
                }
            }
        }

        return triggered
    }

    /**
     * Проверяет эффект "Чай" (id=10)
     * @return Triple(attackBonus, hitAlly, allyTarget)
     * attackBonus - бонус к атаке (0 или 18)
     * hitAlly - true если атакует своего
     * allyTarget - карта союзника, если hitAlly = true
     */
    fun checkTeaTrigger(attacker: GameCard, allAllies: List<GameCard>): Triple<Int, Boolean, GameCard?> {
        var attackBonus = 0
        var hitAlly = false
        var allyTarget: GameCard? = null

        if (attacker.originalCard.id == 10 && attacker.isAlive) {
            // 65% шанс на бафф
            val buffChance = Random.nextInt(100)
            if (buffChance < 65) {
                attackBonus = 18

                // 60% шанс на дружественный огонь
                val friendlyFireChance = Random.nextInt(100)
                if (friendlyFireChance < 60) {
                    hitAlly = true
                    val possibleTargets = allAllies.filter { it.isAlive && it != attacker }
                    if (possibleTargets.isNotEmpty()) {
                        allyTarget = possibleTargets.random()
                    }
                }
            }
        }

        return Triple(attackBonus, hitAlly, allyTarget)
    }

    /**
     * Получает список иконок особенностей для карты
     */
    fun getSpecialIcons(card: GameCard, allCards: List<GameCard>): List<SpecialEffectInfo> {
        val icons = mutableListOf<SpecialEffectInfo>()

        // "Горохострел" (id=1)
        if (card.originalCard.id == 1) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_sword,
                description = "20% атакует повторно",
                cardId = 1
            ))
        }

        // "Древний сожитель" (id=2)
        val hasAncientCompanion = allCards.any { it.originalCard.id == 2 && it.isAlive }
        if (hasAncientCompanion) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_sword,
                description = "20% все карты атакуют повторно",
                cardId = 2
            ))
        }

        // "Племя потерянных" (id=3)
        if (card.originalCard.id == 3) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_stun,
                description = "35% случайная карта пропускает ход",
                cardId = 3
            ))
        }

        // "Красный дьявол" (id=4)
        if (card.originalCard.id == 4) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_double_attack,
                description = "50% атака x2, но защита -5",
                cardId = 4
            ))
        }

        // "Сын депутата" (id=5)
        if (card.originalCard.id == 5) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_shield,
                description = "80% -50% урона, но пропуск хода",
                cardId = 5
            ))
        }

        // "Мини Пекка" (id=6)
        if (card.originalCard.id == 6) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_double_attack,
                description = "65% атака +10, но защита -5",
                cardId = 6
            ))
        }

        // "Мастер-класс" (id=7)
        if (card.originalCard.id == 7) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_heal,
                description = "80% +15 здоровья, но пропуск хода",
                cardId = 7
            ))
        }

        // "Единение с природой" (id=8)
        if (card.originalCard.id == 8) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_nature,
                description = "50% +10 к атаке, но 40% -5 к защите и здоровью",
                cardId = 8
            ))
        }

        // "Роланд Азер" (id=9)
        if (card.originalCard.id == 9) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_nature,
                description = "60% +39% урона всем картам",
                cardId = 9
            ))
        }

        // "Чай" (id=10)
        if (card.originalCard.id == 10) {
            icons.add(SpecialEffectInfo(
                iconResId = R.drawable.ic_nature,
                description = "65% атака +18, но 60% шанс задеть своих",
                cardId = 10
            ))
        }

        return icons
    }

    /**
     * Отмечает, что повторная атака была использована
     */
    fun markExtraAttackUsed(attacker: GameCard, allCards: List<GameCard>) {
        allCards.filter { it.originalCard.id == 2 && it.isAlive }
            .forEach { it.extraAttackUsed = true }

        if (attacker.originalCard.id == 1) {
            attacker.extraAttackUsed = true
        }
    }

    /**
     * Сбрасывает флаги повторных атак и защиты для всех карт
     */
    fun resetAllFlags(allCards: List<GameCard>) {
        allCards.forEach {
            it.resetExtraAttackFlag()
            it.resetDamageReductionFlag()
            it.resetGlobalAttackBuffFlag()
        }
    }

    /**
     * Получает описание особенности для карты
     */
    fun getSpecialFeatureDescription(card: Card): String {
        return when (card.id) {
            1 -> "20% атакует повторно"
            2 -> "20% все карты атакуют повторно"
            3 -> "35% случайная карта пропускает ход"
            4 -> "50% атака x2, но защита -5"
            5 -> "80% -50% урона, но пропуск хода"
            6 -> "65% атака +10, но защита -5"
            7 -> "80% +15 здоровья, но пропуск хода"
            8 -> "50% +10 к атаке на весь бой, но 40% -5 к защите и здоровью"
            9 -> "60% +39% урона всем картам"
            10 -> "65% атака +18, но 60% шанс задеть своих"
            else -> card.specialFeature
        }
    }
}