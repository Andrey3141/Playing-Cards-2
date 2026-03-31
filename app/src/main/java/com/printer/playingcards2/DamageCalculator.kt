package com.printer.playingcards2

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.animation.doOnEnd

class DamageCalculator(private val activity: GameActivity) {

    private var darkOverlayTop: View? = null
    private var darkOverlayBottom: View? = null

    fun applyDamage(
        attacker: GameCard,
        target: GameCard,
        isPlayerAttacking: Boolean,
        isEnemyAttack: Boolean,
        teaAttackBonus: Int
    ) {
        // Лечение "Мастер-класс" (id=7)
        val healAmount = CardSpecialEffect.checkHealTrigger(attacker)
        if (healAmount > 0) {
            val maxHealth = attacker.originalCard.health
            val oldHealth = attacker.currentHealth

            if (oldHealth >= maxHealth) {
                activity.battleStatusText.text = "💚 ${attacker.originalCard.name} уже имеет максимальное здоровье! 💚"
                Toast.makeText(activity, "💚 Лечение не нужно — здоровье уже максимально! 💚", Toast.LENGTH_SHORT).show()
            } else {
                val newHealth = (oldHealth + healAmount).coerceAtMost(maxHealth)
                val actualHeal = newHealth - oldHealth
                attacker.currentHealth = newHealth
                activity.battleStatusText.text = "💚 ${attacker.originalCard.name} восстановил $actualHeal здоровья! 💚"
                Toast.makeText(activity, "💚 ${attacker.originalCard.name} лечится на $actualHeal HP, но пропускает ход! 💚", Toast.LENGTH_LONG).show()
            }

            val attackerPos = if (isPlayerAttacking) activity.playerCards.indexOf(attacker) else activity.enemyCards.indexOf(attacker)
            if (attackerPos != -1) {
                if (isPlayerAttacking) activity.playerAdapter.notifyItemChanged(attackerPos)
                else activity.enemyAdapter.notifyItemChanged(attackerPos)
            }

            if (teaAttackBonus > 0) attacker.currentAttack -= teaAttackBonus

            activity.clearSelection()
            if (isPlayerAttacking) {
                CardSpecialEffect.resetAllFlags(activity.playerCards)
                activity.currentTurn = 1
                activity.turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                activity.battleStatusText.text = activity.getString(R.string.enemy_turn)
                Handler(Looper.getMainLooper()).postDelayed({ activity.enemyTurn() }, 800)
            } else {
                CardSpecialEffect.resetAllFlags(activity.enemyCards)
                activity.currentTurn = 0
                activity.startPlayerTurn()
            }
            activity.isWaitingForTarget = false
            activity.selectedPlayerCard = null
            return
        }

        // Удвоение атаки "Красный дьявол" (id=4)
        val (attackMultiplier, defenseChanged) = CardSpecialEffect.checkDoubleAttackTrigger(attacker)
        val originalAttack = attacker.currentAttack
        if (attackMultiplier == 2) {
            attacker.currentAttack *= 2
            activity.battleStatusText.text = "🔥 ${attacker.originalCard.name} активировал ярость! Атака x2! 🔥"
            Toast.makeText(activity, "🔥 ${attacker.originalCard.name} атакует с удвоенной силой! 🔥", Toast.LENGTH_SHORT).show()
        }

        // Бафф атаки "Мини Пекка" (id=6)
        val (attackBonus, defenseReduced) = CardSpecialEffect.checkAttackBuffTrigger(attacker)
        val originalAttackForBuff = attacker.currentAttack
        if (attackBonus > 0) {
            attacker.currentAttack += attackBonus
            activity.battleStatusText.text = "⚔️ ${attacker.originalCard.name} усилился! Атака +10! ⚔️"
            Toast.makeText(activity, "⚔️ ${attacker.originalCard.name} атакует с бонусом +10! ⚔️", Toast.LENGTH_SHORT).show()
        }

        // Снижение урона "Сын депутата" (id=5)
        val damageMultiplier = CardSpecialEffect.checkDamageReduction(target)
        var reducedDamageFlag = false
        if (damageMultiplier < 1f) {
            reducedDamageFlag = true
            activity.battleStatusText.text = "🛡️ ${target.originalCard.name} игнорирует 50% урона! 🛡️"
            Toast.makeText(activity, "🛡️ ${target.originalCard.name} игнорирует половину урона, но пропускает ход! 🛡️", Toast.LENGTH_LONG).show()
        }

        // Оглушение "Племя потерянных" (id=3)
        val allEnemyCards = if (isPlayerAttacking) activity.enemyCards else activity.playerCards
        val stunnedTarget = CardSpecialEffect.checkStunTrigger(attacker, allEnemyCards)
        if (stunnedTarget != null) {
            stunnedTarget.isStunned = true
            activity.battleStatusText.text = "🔥 ${attacker.originalCard.name} оглушил ${stunnedTarget.originalCard.name} палкой! 🔥"
            Toast.makeText(activity, "🔥 ${stunnedTarget.originalCard.name} пропустит следующий ход! 🔥", Toast.LENGTH_LONG).show()
            val stunnedPos = allEnemyCards.indexOf(stunnedTarget)
            if (stunnedPos != -1) {
                if (isPlayerAttacking) activity.enemyAdapter.notifyItemChanged(stunnedPos)
                else activity.playerAdapter.notifyItemChanged(stunnedPos)
            }
        }

        // Эффект "Стример-неудачник" (id=13) - иллюзия
        val (illusionBonus, isIllusion, _) = CardSpecialEffect.checkIllusionTrigger(attacker, target)
        val originalIllusionAttack = attacker.currentAttack

        // 1. Применяем ОСНОВНОЙ урон (без бонуса)
        var baseDamage = attacker.currentAttack
        var finalDamage = (baseDamage * damageMultiplier).toInt()
        finalDamage = maxOf(finalDamage, 1)

        var remainingDamage = finalDamage
        var damageToHealth = 0
        var shieldDamage = 0

        if (target.currentDefense > 0) {
            shieldDamage = minOf(remainingDamage, target.currentDefense)
            target.currentDefense -= shieldDamage
            remainingDamage -= shieldDamage
        }

        if (remainingDamage > 0) {
            damageToHealth = remainingDamage
            target.currentHealth = (target.currentHealth - damageToHealth).coerceAtLeast(0)
        }

        val displayDamage = finalDamage

        activity.battleStatusText.text = when {
            reducedDamageFlag -> "${attacker.originalCard.name} → $displayDamage урона (50% поглощено!)"
            damageToHealth > 0 && target.currentDefense == 0 -> "${attacker.originalCard.name} → $displayDamage урона (щит сломан!)"
            damageToHealth == 0 && target.currentDefense > 0 -> "${attacker.originalCard.name} → $displayDamage урона (щит поглотил урон)"
            attackMultiplier == 2 -> "${attacker.originalCard.name} → $displayDamage урона (x2 урон!)"
            attackBonus > 0 -> "${attacker.originalCard.name} → $displayDamage урона (+10 к атаке!)"
            teaAttackBonus > 0 -> "${attacker.originalCard.name} → $displayDamage урона (+18 к атаке!)"
            else -> "${attacker.originalCard.name} → $displayDamage урона"
        }

        // 2. Сохраняем чекпоинт ПОСЛЕ основного урона
        val checkpointHealth = target.currentHealth
        val checkpointDefense = target.currentDefense

        // 3. Если есть бонус +12
        if (illusionBonus > 0) {
            var bonusRemaining = illusionBonus
            var bonusDamageToHealth = 0
            var bonusShieldDamage = 0

            if (target.currentDefense > 0) {
                bonusShieldDamage = minOf(bonusRemaining, target.currentDefense)
                target.currentDefense -= bonusShieldDamage
                bonusRemaining -= bonusShieldDamage
            }

            if (bonusRemaining > 0) {
                bonusDamageToHealth = bonusRemaining
                target.currentHealth = (target.currentHealth - bonusDamageToHealth).coerceAtLeast(0)
            }

            activity.battleStatusText.text = "${attacker.originalCard.name} → +$illusionBonus дополнительного урона! 🎭"

            // 4. Если это иллюзия — возвращаем чекпоинт
            if (isIllusion) {
                activity.battleStatusText.text = "😵💫 ${attacker.originalCard.name} осознаёт, что это была иллюзия! 😵💫"
                Toast.makeText(activity, "😵💫 Это была иллюзия! Дополнительный урон был только в воображении! 😵💫", Toast.LENGTH_LONG).show()

                animateIllusion {
                    target.currentHealth = checkpointHealth
                    target.currentDefense = checkpointDefense

                    if (isPlayerAttacking) {
                        val targetPos = activity.enemyCards.indexOf(target)
                        if (targetPos != -1) activity.enemyAdapter.notifyItemChanged(targetPos)
                    } else {
                        val targetPos = activity.playerCards.indexOf(target)
                        if (targetPos != -1) activity.playerAdapter.notifyItemChanged(targetPos)
                    }

                    activity.battleStatusText.text = "✨ Иллюзия рассеялась! Дополнительный урон был только в воображении... ✨"

                    if (illusionBonus > 0) {
                        attacker.currentAttack = originalIllusionAttack
                    }

                    if (isPlayerAttacking) {
                        CardSpecialEffect.resetAllFlags(activity.playerCards)
                        activity.currentTurn = 1
                        activity.isWaitingForTarget = false
                        activity.selectedPlayerCard = null
                        activity.turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                        activity.battleStatusText.text = activity.getString(R.string.enemy_turn)
                        Handler(Looper.getMainLooper()).postDelayed({ activity.enemyTurn() }, 800)
                    } else {
                        CardSpecialEffect.resetAllFlags(activity.enemyCards)
                        activity.currentTurn = 0
                        activity.startPlayerTurn()
                    }
                }
                return
            }
        }

        // Восстанавливаем атаку после эффекта
        if (attackMultiplier == 2) attacker.currentAttack = originalAttack
        if (attackBonus > 0) attacker.currentAttack = originalAttackForBuff
        if (teaAttackBonus > 0) attacker.currentAttack -= teaAttackBonus
        if (illusionBonus > 0 && !isIllusion) attacker.currentAttack = originalIllusionAttack

        // Смерть цели
        if (target.currentHealth <= 0) {
            target.isAlive = false
            val index = if (isPlayerAttacking) activity.enemyCards.indexOf(target) else activity.playerCards.indexOf(target)
            if (index != -1) {
                if (isPlayerAttacking) {
                    activity.enemyCards.removeAt(index)
                    activity.enemyAdapter.notifyItemRemoved(index)
                } else {
                    activity.playerCards.removeAt(index)
                    activity.playerAdapter.notifyItemRemoved(index)
                }
                activity.battleStatusText.text = "${target.originalCard.name} повержен!"
                if (if (isPlayerAttacking) activity.enemyCards.isEmpty() else activity.playerCards.isEmpty()) {
                    activity.endBattle(isPlayerAttacking)
                    return
                }
            }
            activity.clearSelection()
            if (isPlayerAttacking) {
                CardSpecialEffect.resetAllFlags(activity.playerCards)
                activity.currentTurn = 1
                activity.turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                activity.battleStatusText.text = activity.getString(R.string.enemy_turn)
                Handler(Looper.getMainLooper()).postDelayed({ activity.enemyTurn() }, 800)
            } else {
                CardSpecialEffect.resetAllFlags(activity.enemyCards)
                activity.currentTurn = 0
                activity.startPlayerTurn()
            }
            activity.isWaitingForTarget = false
            activity.selectedPlayerCard = null
            return
        }

        if (isPlayerAttacking) {
            val targetPos = activity.enemyCards.indexOf(target)
            if (targetPos != -1) activity.enemyAdapter.notifyItemChanged(targetPos)
        } else {
            val targetPos = activity.playerCards.indexOf(target)
            if (targetPos != -1) activity.playerAdapter.notifyItemChanged(targetPos)
        }

        activity.clearSelection()

        val allSideCards = if (isPlayerAttacking) activity.playerCards else activity.enemyCards
        val triggeredEffects = CardSpecialEffect.checkExtraAttackTriggers(attacker, allSideCards)

        if (triggeredEffects.isNotEmpty()) {
            val effectMessages = triggeredEffects.map { effect ->
                when (effect.cardId) {
                    1 -> "Горохострел"
                    2 -> "Древний сожитель"
                    else -> "Особенность"
                }
            }
            val sideName = if (isPlayerAttacking) "Ваша" else "Вражеская"
            val effectText = effectMessages.joinToString(" и ")
            val toastMessage = if (triggeredEffects.size == 1) {
                "🔥 $sideName карта \"$effectText\" дает повторную атаку! 🔥"
            } else {
                "🔥 $sideName карты \"$effectText\" дают повторную атаку! 🔥"
            }

            activity.battleStatusText.text = "🔥 Сработал эффект: $effectText! Повторная атака! 🔥"
            Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show()

            Handler(Looper.getMainLooper()).postDelayed({
                val aliveTargets = if (isPlayerAttacking) activity.enemyCards.filter { it.isAlive } else activity.playerCards.filter { it.isAlive }
                if (aliveTargets.isNotEmpty()) {
                    activity.performAttack(attacker, aliveTargets.random(), !isPlayerAttacking)
                } else {
                    activity.endBattle(isPlayerAttacking)
                }
            }, 800)
            return
        }

        val (natureAttackBonus, natureDefenseReduced, natureHealthReduced) = CardSpecialEffect.checkNatureBuffTrigger(attacker)
        if (natureAttackBonus > 0) {
            activity.battleStatusText.text = "🌿 ${attacker.originalCard.name} сливается с природой! Атака +10 навсегда! 🌿"
            Toast.makeText(activity, "🌿 ${attacker.originalCard.name} получил +10 к атаке! 🌿", Toast.LENGTH_SHORT).show()
            if (natureDefenseReduced || natureHealthReduced) {
                val debuffMsg = mutableListOf<String>()
                if (natureDefenseReduced) debuffMsg.add("защита -5")
                if (natureHealthReduced) debuffMsg.add("здоровье -5")
                activity.battleStatusText.text = "${activity.battleStatusText.text} Но природа забирает: ${debuffMsg.joinToString(", ")}!"
                Toast.makeText(activity, "⚠️ ${attacker.originalCard.name} теряет ${debuffMsg.joinToString(", ")}! ⚠️", Toast.LENGTH_SHORT).show()
            }
            val attackerPos = if (isPlayerAttacking) activity.playerCards.indexOf(attacker) else activity.enemyCards.indexOf(attacker)
            if (attackerPos != -1) {
                if (isPlayerAttacking) activity.playerAdapter.notifyItemChanged(attackerPos)
                else activity.enemyAdapter.notifyItemChanged(attackerPos)
            }
        }

        val globalBuffTriggered = CardSpecialEffect.checkGlobalAttackBuffTrigger(attacker, allSideCards)
        if (globalBuffTriggered) {
            val sideName = if (isPlayerAttacking) "Ваши" else "Вражеские"
            activity.battleStatusText.text = "👑 ${attacker.originalCard.name} вдохновляет армию! $sideName карты получают +39% к атаке! 👑"
            Toast.makeText(activity, "👑 $sideName карты усилены на 39%! 👑", Toast.LENGTH_LONG).show()
            if (isPlayerAttacking) activity.playerAdapter.notifyDataSetChanged()
            else activity.enemyAdapter.notifyDataSetChanged()
        }

        val allEnemyCardsForKill = if (isPlayerAttacking) activity.enemyCards else activity.playerCards
        val (strongestCard, shouldKillSelf) = CardSpecialEffect.checkKillStrongestTrigger(attacker, allEnemyCardsForKill)

        if (strongestCard != null && shouldKillSelf) {
            activity.battleStatusText.text = "💀 ${attacker.originalCard.name} жертвует собой, чтобы убить ${strongestCard.originalCard.name}! 💀"
            Toast.makeText(activity, "💀 ${attacker.originalCard.name} уничтожает сильнейшего врага ценой своей жизни! 💀", Toast.LENGTH_LONG).show()

            strongestCard.currentHealth = 0
            strongestCard.isAlive = false

            val strongestIndex = if (isPlayerAttacking) activity.enemyCards.indexOf(strongestCard) else activity.playerCards.indexOf(strongestCard)
            if (strongestIndex != -1) {
                if (isPlayerAttacking) {
                    activity.enemyCards.removeAt(strongestIndex)
                    activity.enemyAdapter.notifyItemRemoved(strongestIndex)
                } else {
                    activity.playerCards.removeAt(strongestIndex)
                    activity.playerAdapter.notifyItemRemoved(strongestIndex)
                }
            }

            attacker.currentHealth = 0
            attacker.isAlive = false

            val attackerIndex = if (isPlayerAttacking) activity.playerCards.indexOf(attacker) else activity.enemyCards.indexOf(attacker)
            if (attackerIndex != -1) {
                if (isPlayerAttacking) {
                    activity.playerCards.removeAt(attackerIndex)
                    activity.playerAdapter.notifyItemRemoved(attackerIndex)
                } else {
                    activity.enemyCards.removeAt(attackerIndex)
                    activity.enemyAdapter.notifyItemRemoved(attackerIndex)
                }
            }

            if (if (isPlayerAttacking) activity.enemyCards.isEmpty() else activity.playerCards.isEmpty()) {
                activity.endBattle(isPlayerAttacking)
                return
            }
            if (if (isPlayerAttacking) activity.playerCards.isEmpty() else activity.enemyCards.isEmpty()) {
                activity.endBattle(!isPlayerAttacking)
                return
            }

            activity.clearSelection()

            if (isPlayerAttacking) {
                CardSpecialEffect.resetAllFlags(activity.playerCards)
                activity.currentTurn = 1
                activity.turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                activity.battleStatusText.text = activity.getString(R.string.enemy_turn)
                Handler(Looper.getMainLooper()).postDelayed({ activity.enemyTurn() }, 800)
            } else {
                CardSpecialEffect.resetAllFlags(activity.enemyCards)
                activity.currentTurn = 0
                activity.startPlayerTurn()
            }
            return
        }

        val (dreamerAttackBonus, dreamerSkipTurn, dreamerShouldStun) = CardSpecialEffect.checkDreamerTrigger(attacker)
        val originalDreamerAttack = attacker.currentAttack
        if (dreamerAttackBonus > 0) {
            attacker.currentAttack += dreamerAttackBonus
            activity.battleStatusText.text = "💭 ${attacker.originalCard.name} фантазирует! Атака +20! 💭"
            Toast.makeText(activity, "💭 ${attacker.originalCard.name} мечтает о большой атаке! +20! 💭", Toast.LENGTH_SHORT).show()
        }

        if (dreamerAttackBonus > 0 && dreamerShouldStun) {
            activity.battleStatusText.text = "😵 ${attacker.originalCard.name} замечтался и пропускает ход! 😵"
            Toast.makeText(activity, "😵 ${attacker.originalCard.name} ушёл в мечты и пропускает ход! 😵", Toast.LENGTH_SHORT).show()

            if (isPlayerAttacking) {
                CardSpecialEffect.resetAllFlags(activity.playerCards)
                activity.currentTurn = 1
                activity.turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                activity.battleStatusText.text = activity.getString(R.string.enemy_turn)
                Handler(Looper.getMainLooper()).postDelayed({ activity.enemyTurn() }, 800)
            } else {
                CardSpecialEffect.resetAllFlags(activity.enemyCards)
                activity.currentTurn = 0
                activity.startPlayerTurn()
            }
            activity.isWaitingForTarget = false
            activity.selectedPlayerCard = null
            return
        }

        if (dreamerAttackBonus > 0) {
            attacker.currentAttack = originalDreamerAttack
        }

        if (isPlayerAttacking) {
            CardSpecialEffect.resetAllFlags(activity.playerCards)
            activity.currentTurn = 1
            activity.isWaitingForTarget = false
            activity.selectedPlayerCard = null
            activity.turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
            activity.battleStatusText.text = activity.getString(R.string.enemy_turn)
            Handler(Looper.getMainLooper()).postDelayed({ activity.enemyTurn() }, 800)
        } else {
            CardSpecialEffect.resetAllFlags(activity.enemyCards)
            activity.currentTurn = 0
            activity.startPlayerTurn()
        }
    }

    fun animateIllusion(callback: () -> Unit) {
        val screenWidth = activity.resources.displayMetrics.widthPixels
        val screenHeight = activity.resources.displayMetrics.heightPixels

        if (darkOverlayTop == null) {
            darkOverlayTop = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.TRANSPARENT)
                visibility = View.GONE
            }

            darkOverlayBottom = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.TRANSPARENT)
                visibility = View.GONE
            }

            val rootLayout = activity.findViewById<FrameLayout>(android.R.id.content)
            rootLayout.addView(darkOverlayTop)
            rootLayout.addView(darkOverlayBottom)
        }

        val animTop = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                darkOverlayTop?.background = createArcDrawable(progress, isTop = true, screenWidth, screenHeight)
            }
        }

        val animBottom = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                darkOverlayBottom?.background = createArcDrawable(progress, isTop = false, screenWidth, screenHeight)
            }
        }

        darkOverlayTop?.visibility = View.VISIBLE
        darkOverlayBottom?.visibility = View.VISIBLE

        AnimatorSet().apply {
            playTogether(animTop, animBottom)
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val closeAnimTop = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    darkOverlayTop?.background = createArcDrawable(progress, isTop = true, screenWidth, screenHeight)
                }
            }

            val closeAnimBottom = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    darkOverlayBottom?.background = createArcDrawable(progress, isTop = false, screenWidth, screenHeight)
                }
            }

            AnimatorSet().apply {
                playTogether(closeAnimTop, closeAnimBottom)
                start()
                doOnEnd {
                    darkOverlayTop?.visibility = View.GONE
                    darkOverlayBottom?.visibility = View.GONE
                    callback()
                }
            }
        }, 500)
    }

    private fun createArcDrawable(progress: Float, isTop: Boolean, screenWidth: Int, screenHeight: Int): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val paint = android.graphics.Paint().apply {
                color = Color.BLACK
                style = android.graphics.Paint.Style.FILL
            }

            override fun draw(canvas: android.graphics.Canvas) {
                val width = screenWidth.toFloat()
                val height = screenHeight.toFloat()
                val path = android.graphics.Path()

                if (isTop) {
                    val arcHeight = height * 0.35f * progress
                    val controlPointY = arcHeight * 1.5f
                    path.moveTo(0f, 0f)
                    path.lineTo(width, 0f)
                    path.lineTo(width, arcHeight)
                    path.quadTo(width / 2f, controlPointY, 0f, arcHeight)
                    path.close()
                } else {
                    val arcHeight = height * 0.35f * progress
                    val controlPointY = height - arcHeight * 1.5f
                    val endY = height - arcHeight
                    path.moveTo(0f, height)
                    path.lineTo(width, height)
                    path.lineTo(width, endY)
                    path.quadTo(width / 2f, controlPointY, 0f, endY)
                    path.close()
                }

                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
            override fun getOpacity(): Int = android.graphics.PixelFormat.OPAQUE
        }
    }
}