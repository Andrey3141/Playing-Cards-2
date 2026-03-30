package com.printer.playingcards2

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var backButton: MaterialCardView
    private lateinit var glassCard: CardView
    private lateinit var decorSquare1: View
    private lateinit var decorSquare2: View
    private lateinit var whoStartsText: TextView
    private lateinit var turnIndicator: ImageView
    private lateinit var battleStatusText: TextView

    private lateinit var enemyCardsRecyclerView: RecyclerView
    private lateinit var playerCardsRecyclerView: RecyclerView

    private lateinit var playerCards: MutableList<GameCard>
    private lateinit var enemyCards: MutableList<GameCard>
    private lateinit var allCardsDatabase: List<Card>

    private lateinit var playerAdapter: GameCardAdapter
    private lateinit var enemyAdapter: GameCardAdapter

    private var isBattleActive = false
    private var currentTurn = 0 // 0 - игрок, 1 - враг
    private var selectedPlayerCard: GameCard? = null
    private var isWaitingForTarget = false

    private var isProcessingExtraAttack = false
    private var isRematch = false
    private var isAnimating = false

    // Оверлеи для иллюзии
    private lateinit var darkOverlayTop: View
    private lateinit var darkOverlayBottom: View

    private val STATE_PLAYER_CARDS = "player_cards"
    private val STATE_ENEMY_CARDS = "enemy_cards"
    private val STATE_IS_BATTLE_ACTIVE = "is_battle_active"
    private val STATE_CURRENT_TURN = "current_turn"
    private val STATE_SELECTED_CARD_ID = "selected_card_id"
    private val STATE_IS_WAITING_FOR_TARGET = "is_waiting_for_target"
    private val STATE_IS_REMATCH = "is_rematch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        hideSystemUI()
        initViews()
        initOverlays()
        loadAllCards()

        if (savedInstanceState != null) {
            isRematch = savedInstanceState.getBoolean(STATE_IS_REMATCH, false)
            if (isRematch) {
                isRematch = false
                loadPlayerCardsFromInventory()
                if (!checkPlayerHasCards()) return
                setupEnemyCards()
                setupRecyclerViews()
                startEntranceAnimation()
                setupBackButton()
                determineWhoStarts()
            } else {
                restoreState(savedInstanceState)
                setupRecyclerViews()
                startEntranceAnimation()
                setupBackButton()
                if (isBattleActive && currentTurn == 0 && selectedPlayerCard != null) {
                    restoreSelection()
                }
                if (isBattleActive && currentTurn == 1 && !isWaitingForTarget) {
                    Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 1000)
                }
            }
        } else {
            loadPlayerCardsFromInventory()
            if (!checkPlayerHasCards()) return
            setupEnemyCards()
            setupRecyclerViews()
            startEntranceAnimation()
            setupBackButton()
            determineWhoStarts()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        outState.putString(STATE_PLAYER_CARDS, gson.toJson(playerCards))
        outState.putString(STATE_ENEMY_CARDS, gson.toJson(enemyCards))
        outState.putBoolean(STATE_IS_BATTLE_ACTIVE, isBattleActive)
        outState.putInt(STATE_CURRENT_TURN, currentTurn)
        outState.putInt(STATE_SELECTED_CARD_ID, selectedPlayerCard?.originalCard?.id ?: -1)
        outState.putBoolean(STATE_IS_WAITING_FOR_TARGET, isWaitingForTarget)
        outState.putBoolean(STATE_IS_REMATCH, isRematch)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        val gson = Gson()
        val playerType = object : TypeToken<MutableList<GameCard>>() {}.type
        val enemyType = object : TypeToken<MutableList<GameCard>>() {}.type
        playerCards = gson.fromJson(savedInstanceState.getString(STATE_PLAYER_CARDS), playerType)
        enemyCards = gson.fromJson(savedInstanceState.getString(STATE_ENEMY_CARDS), enemyType)
        isBattleActive = savedInstanceState.getBoolean(STATE_IS_BATTLE_ACTIVE)
        currentTurn = savedInstanceState.getInt(STATE_CURRENT_TURN)
        isWaitingForTarget = savedInstanceState.getBoolean(STATE_IS_WAITING_FOR_TARGET)
        val selectedCardId = savedInstanceState.getInt(STATE_SELECTED_CARD_ID)
        selectedPlayerCard = if (selectedCardId != -1) playerCards.find { it.originalCard.id == selectedCardId } else null
        turnIndicator.setImageResource(if (currentTurn == 0) R.drawable.ic_player_turn else R.drawable.ic_enemy_turn)
        if (isBattleActive) {
            battleStatusText.text = when {
                currentTurn == 0 && selectedPlayerCard != null -> getString(R.string.select_target)
                currentTurn == 0 -> getString(R.string.your_turn)
                else -> getString(R.string.enemy_turn)
            }
        }
    }

    private fun restoreSelection() {
        val playerPos = playerCards.indexOf(selectedPlayerCard)
        playerCards.forEachIndexed { index, _ ->
            val view = playerCardsRecyclerView.findViewHolderForAdapterPosition(index)?.itemView
            if (index == playerPos) {
                view?.animate()?.scaleX(1.1f)?.scaleY(1.1f)?.setDuration(200)?.start()
                view?.alpha = 1f
            } else {
                view?.animate()?.alpha(0.5f)?.setDuration(200)?.start()
            }
        }
        enemyCards.forEachIndexed { index, _ ->
            enemyCardsRecyclerView.findViewHolderForAdapterPosition(index)?.itemView?.animate()?.alpha(1f)?.setDuration(200)?.start()
        }
        isWaitingForTarget = true
    }

    private fun determineWhoStarts() {
        val startsFirst = Random.nextBoolean()
        currentTurn = if (startsFirst) 0 else 1
        if (startsFirst) {
            whoStartsText.text = "Вы начинаете первым!"
            battleStatusText.text = getString(R.string.your_turn)
            turnIndicator.setImageResource(R.drawable.ic_player_turn)
            isBattleActive = true
            Handler(Looper.getMainLooper()).postDelayed({ startPlayerTurn() }, 500)
        } else {
            whoStartsText.text = "Противник начинает первым!"
            battleStatusText.text = getString(R.string.enemy_turn)
            turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
            isBattleActive = true
            Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 1500)
        }
        whoStartsText.alpha = 0f
        whoStartsText.animate().alpha(1f).setDuration(800).setStartDelay(500).withEndAction {
            whoStartsText.animate().alpha(0f).setDuration(800).setStartDelay(1500).withEndAction {
                whoStartsText.visibility = View.GONE
            }.start()
        }.start()
    }

    private fun loadAllCards() {
        allCardsDatabase = listOf(
            Card(1, "Горохострел", R.drawable.student_1, Rarity.RARE, "", "", 45, 30, 5, CardCategory.CATEGORY),
            Card(2, "Древний сожитель", R.drawable.student_2, Rarity.COMMON, "", "", 30, 1, 20, CardCategory.CATEGORY),
            Card(3, "Племя потерянных", R.drawable.student_3, Rarity.EPIC, "", "", 110, 4, 0, CardCategory.CATEGORY),
            Card(4, "Красный дьявол", R.drawable.student_4, Rarity.LEGENDARY, "", "", 95, 20, 6, CardCategory.CATEGORY),
            Card(5, "Сын депутата", R.drawable.student_5, Rarity.RARE, "", "", 35, 10, 40, CardCategory.CATEGORY),
            Card(6, "Мини Пекка", R.drawable.student_6, Rarity.MYTHIC, "", "", 30, 25, 50, CardCategory.CATEGORY),
            Card(7, "Мастер-класс", R.drawable.student_7, Rarity.COMMON, "", "", 5, 25, 10, CardCategory.CATEGORY),
            Card(8, "Единение с природой", R.drawable.student_8, Rarity.SUPER_RARE, "", "", 65, 20, 0, CardCategory.CATEGORY),
            Card(9, "Роланд Азер", R.drawable.student_9, Rarity.LEGENDARY, "", "", 30, 40, 20, CardCategory.CATEGORY),
            Card(10, "Чай", R.drawable.student_10, Rarity.EPIC, "", "", 30, 30, 20, CardCategory.CATEGORY)
        )
    }

    private fun loadPlayerCardsFromInventory() {
        val sharedPreferences = getSharedPreferences("CardGamePrefs", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("cards_state", null)
        val type = object : TypeToken<MutableList<Card>>() {}.type
        val allCards = if (json != null) gson.fromJson<MutableList<Card>>(json, type) else mutableListOf()
        playerCards = allCards.filter { it.category == CardCategory.DECK }.map { GameCard(it) }.toMutableList()
        CardSpecialEffect.resetAllFlags(playerCards)
    }

    private fun checkPlayerHasCards(): Boolean {
        if (playerCards.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Нет карт")
                .setMessage("Добавьте карты в колоду в инвентаре")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return false
        }
        return true
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        glassCard = findViewById(R.id.glassCard)
        decorSquare1 = findViewById(R.id.decorSquare1)
        decorSquare2 = findViewById(R.id.decorSquare2)
        whoStartsText = findViewById(R.id.whoStartsText)
        turnIndicator = findViewById(R.id.turnIndicator)
        enemyCardsRecyclerView = findViewById(R.id.enemyCardsRecyclerView)
        playerCardsRecyclerView = findViewById(R.id.playerCardsRecyclerView)
        battleStatusText = findViewById(R.id.battleStatusText)
    }

    private fun initOverlays() {
        darkOverlayTop = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                0
            )
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }

        darkOverlayBottom = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                0
            )
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }

        val rootLayout = findViewById<FrameLayout>(android.R.id.content)
        rootLayout.addView(darkOverlayTop)
        rootLayout.addView(darkOverlayBottom)
    }

    private fun animateIllusion(callback: () -> Unit) {
        val screenHeight = resources.displayMetrics.heightPixels
        val halfScreen = screenHeight / 2

        // Анимация появления шторок
        val animTop = ObjectAnimator.ofInt(darkOverlayTop.layoutParams.height, 0, halfScreen).apply {
            addUpdateListener {
                darkOverlayTop.layoutParams.height = it.animatedValue as Int
                darkOverlayTop.requestLayout()
            }
            duration = 400
        }

        val animBottom = ObjectAnimator.ofInt(darkOverlayBottom.layoutParams.height, 0, halfScreen).apply {
            addUpdateListener {
                darkOverlayBottom.layoutParams.height = it.animatedValue as Int
                darkOverlayBottom.requestLayout()
            }
            duration = 400
        }

        darkOverlayTop.visibility = View.VISIBLE
        darkOverlayBottom.visibility = View.VISIBLE

        AnimatorSet().apply {
            playTogether(animTop, animBottom)
            start()
        }

        // Задержка перед обратной анимацией
        Handler(Looper.getMainLooper()).postDelayed({
            // Анимация закрытия шторок
            val closeAnimTop = ObjectAnimator.ofInt(darkOverlayTop.layoutParams.height, halfScreen, 0).apply {
                addUpdateListener {
                    darkOverlayTop.layoutParams.height = it.animatedValue as Int
                    darkOverlayTop.requestLayout()
                }
                duration = 400
            }

            val closeAnimBottom = ObjectAnimator.ofInt(darkOverlayBottom.layoutParams.height, halfScreen, 0).apply {
                addUpdateListener {
                    darkOverlayBottom.layoutParams.height = it.animatedValue as Int
                    darkOverlayBottom.requestLayout()
                }
                duration = 400
            }

            AnimatorSet().apply {
                playTogether(closeAnimTop, closeAnimBottom)
                start()
                doOnEnd {
                    darkOverlayTop.visibility = View.GONE
                    darkOverlayBottom.visibility = View.GONE
                    callback()
                }
            }
        }, 800)
    }

    private fun setupEnemyCards() {
        val availableCards = allCardsDatabase.toMutableList()
        enemyCards = mutableListOf()
        repeat(3) {
            if (availableCards.isNotEmpty()) {
                val randomIndex = Random.nextInt(availableCards.size)
                val selectedCard = availableCards.removeAt(randomIndex)
                enemyCards.add(GameCard(selectedCard))
            }
        }
    }

    private fun setupRecyclerViews() {
        playerAdapter = GameCardAdapter(playerCards, true, { card ->
            if (isBattleActive && currentTurn == 0 && card.isAlive && !isAnimating && !isProcessingExtraAttack) {
                if (selectedPlayerCard != null && isWaitingForTarget) {
                    clearSelection()
                    selectPlayerCard(card)
                } else if (selectedPlayerCard == null && !isWaitingForTarget) {
                    selectPlayerCard(card)
                }
            }
        }, playerCards)

        enemyAdapter = GameCardAdapter(enemyCards, false, { card ->
            if (isBattleActive && currentTurn == 0 && selectedPlayerCard != null && isWaitingForTarget && card.isAlive && !isAnimating && !isProcessingExtraAttack) {
                performAttack(selectedPlayerCard!!, card, false)
            }
        }, enemyCards)

        playerCardsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        playerCardsRecyclerView.adapter = playerAdapter
        enemyCardsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        enemyCardsRecyclerView.adapter = enemyAdapter
    }

    private fun startEntranceAnimation() {
        glassCard.alpha = 0f
        glassCard.scaleX = 0.5f
        glassCard.scaleY = 0.5f
        ObjectAnimator.ofFloat(glassCard, "alpha", 0f, 0.4f).apply { duration = 600; start() }
        ObjectAnimator.ofFloat(glassCard, "scaleX", 0.5f, 1f).apply { duration = 600; interpolator = AnticipateOvershootInterpolator(); start() }
        ObjectAnimator.ofFloat(glassCard, "scaleY", 0.5f, 1f).apply { duration = 600; interpolator = AnticipateOvershootInterpolator(); start() }
        val squares = listOf(decorSquare1, decorSquare2)
        squares.forEachIndexed { index, square ->
            square.alpha = 0f
            square.translationY = 50f
            square.animate().alpha(0.06f).translationY(0f).setDuration(500).setStartDelay(index * 100L).setInterpolator(DecelerateInterpolator()).start()
        }
        backButton.alpha = 0f
        backButton.translationX = -80f
        backButton.animate().alpha(1f).translationX(0f).setDuration(400).start()
        turnIndicator.alpha = 0f
        turnIndicator.scaleX = 0f
        turnIndicator.scaleY = 0f
        turnIndicator.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).setStartDelay(200).setInterpolator(AnticipateOvershootInterpolator()).start()
        battleStatusText.alpha = 0f
        battleStatusText.translationY = 30f
        battleStatusText.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(400).start()
    }

    private fun selectPlayerCard(card: GameCard) {
        selectedPlayerCard = card
        isWaitingForTarget = true
        battleStatusText.text = getString(R.string.select_target)
        val playerPos = playerCards.indexOf(card)
        playerCards.forEachIndexed { index, _ ->
            val view = playerCardsRecyclerView.findViewHolderForAdapterPosition(index)?.itemView
            if (index == playerPos) {
                view?.animate()?.scaleX(1.1f)?.scaleY(1.1f)?.setDuration(200)?.start()
                view?.alpha = 1f
            } else {
                view?.animate()?.alpha(0.5f)?.setDuration(200)?.start()
            }
        }
        enemyCards.forEachIndexed { index, _ ->
            enemyCardsRecyclerView.findViewHolderForAdapterPosition(index)?.itemView?.animate()?.alpha(1f)?.setDuration(200)?.start()
        }
    }

    private fun animateAttack(attackerView: View, targetView: View, onComplete: () -> Unit) {
        isAnimating = true
        val originalScaleX = attackerView.scaleX
        val originalScaleY = attackerView.scaleY
        val originalAttackerX = attackerView.translationX
        val originalAttackerY = attackerView.translationY
        val attackerLocation = IntArray(2)
        val targetLocation = IntArray(2)
        attackerView.getLocationOnScreen(attackerLocation)
        targetView.getLocationOnScreen(targetLocation)
        val deltaX = (targetLocation[0] - attackerLocation[0]).toFloat()
        val deltaY = (targetLocation[1] - attackerLocation[1]).toFloat()

        val flyToTargetX = ObjectAnimator.ofFloat(attackerView, "translationX", originalAttackerX, originalAttackerX + deltaX).apply { duration = 180; interpolator = AccelerateDecelerateInterpolator() }
        val flyToTargetY = ObjectAnimator.ofFloat(attackerView, "translationY", originalAttackerY, originalAttackerY + deltaY).apply { duration = 180; interpolator = AccelerateDecelerateInterpolator() }
        val scaleUpAttacker = ObjectAnimator.ofPropertyValuesHolder(attackerView, PropertyValuesHolder.ofFloat("scaleX", originalScaleX, 1.25f), PropertyValuesHolder.ofFloat("scaleY", originalScaleY, 1.25f)).apply { duration = 150; interpolator = AccelerateDecelerateInterpolator() }
        val flyBackX = ObjectAnimator.ofFloat(attackerView, "translationX", originalAttackerX + deltaX, originalAttackerX).apply { duration = 200; interpolator = BounceInterpolator() }
        val flyBackY = ObjectAnimator.ofFloat(attackerView, "translationY", originalAttackerY + deltaY, originalAttackerY).apply { duration = 200; interpolator = BounceInterpolator() }
        val scaleDownAttacker = ObjectAnimator.ofPropertyValuesHolder(attackerView, PropertyValuesHolder.ofFloat("scaleX", 1.25f, originalScaleX), PropertyValuesHolder.ofFloat("scaleY", 1.25f, originalScaleY)).apply { duration = 150; interpolator = BounceInterpolator() }
        val targetShake1 = ObjectAnimator.ofPropertyValuesHolder(targetView, PropertyValuesHolder.ofFloat("scaleX", 1f, 1.2f), PropertyValuesHolder.ofFloat("scaleY", 1f, 1.2f)).apply { duration = 60; interpolator = AccelerateDecelerateInterpolator() }
        val targetShake2 = ObjectAnimator.ofPropertyValuesHolder(targetView, PropertyValuesHolder.ofFloat("scaleX", 1.2f, 0.9f), PropertyValuesHolder.ofFloat("scaleY", 1.2f, 0.9f)).apply { duration = 60; interpolator = AccelerateDecelerateInterpolator() }
        val targetShake3 = ObjectAnimator.ofPropertyValuesHolder(targetView, PropertyValuesHolder.ofFloat("scaleX", 0.9f, 1f), PropertyValuesHolder.ofFloat("scaleY", 0.9f, 1f)).apply { duration = 80; interpolator = BounceInterpolator() }
        val targetCard = targetView as? MaterialCardView
        val originalTargetColor = targetCard?.cardBackgroundColor?.defaultColor ?: android.graphics.Color.argb(51, 255, 255, 255)
        val targetRedColor = ObjectAnimator.ofArgb(targetCard, "cardBackgroundColor", originalTargetColor, android.graphics.Color.argb(255, 220, 60, 60)).apply { duration = 100 }
        val targetColorReset = ObjectAnimator.ofArgb(targetCard, "cardBackgroundColor", android.graphics.Color.argb(255, 220, 60, 60), originalTargetColor).apply { duration = 150; startDelay = 50 }

        val flySet = AnimatorSet().apply { playTogether(flyToTargetX, flyToTargetY, scaleUpAttacker) }
        val hitSet = AnimatorSet().apply { playSequentially(targetShake1, targetShake2, targetShake3) }
        val returnSet = AnimatorSet().apply { playTogether(flyBackX, flyBackY, scaleDownAttacker) }

        flySet.start()
        flySet.doOnEnd {
            hitSet.start()
            if (targetCard != null) { targetRedColor.start(); targetColorReset.start() }
            hitSet.doOnEnd {
                returnSet.start()
                returnSet.doOnEnd {
                    isAnimating = false
                    onComplete()
                }
            }
        }
    }

    private fun performAttack(attacker: GameCard, target: GameCard, isEnemyAttack: Boolean = false) {
        if (attacker.isStunned) {
            battleStatusText.text = "${attacker.originalCard.name} оглушен и пропускает ход!"
            attacker.isStunned = false
            if (isEnemyAttack) {
                endTurnAfterStun(false)
            } else {
                endTurnAfterStun(true)
            }
            return
        }

        if (!attacker.isAlive || !target.isAlive || isAnimating) return

        val isPlayerAttacking = !isEnemyAttack
        val attackerPos = if (isPlayerAttacking) playerCards.indexOf(attacker) else enemyCards.indexOf(attacker)
        val targetPos = if (isPlayerAttacking) enemyCards.indexOf(target) else playerCards.indexOf(target)

        if (attackerPos == -1 || targetPos == -1) return

        val attackerView = if (isPlayerAttacking) playerCardsRecyclerView.findViewHolderForAdapterPosition(attackerPos)?.itemView
        else enemyCardsRecyclerView.findViewHolderForAdapterPosition(attackerPos)?.itemView
        val targetView = if (isPlayerAttacking) enemyCardsRecyclerView.findViewHolderForAdapterPosition(targetPos)?.itemView
        else playerCardsRecyclerView.findViewHolderForAdapterPosition(targetPos)?.itemView

        if (attackerView == null || targetView == null) {
            applyDamage(attacker, target, isPlayerAttacking, isEnemyAttack)
        } else {
            animateAttack(attackerView, targetView) {
                applyDamage(attacker, target, isPlayerAttacking, isEnemyAttack)
            }
        }
    }

    private fun applyDamage(attacker: GameCard, target: GameCard, isPlayerAttacking: Boolean, isEnemyAttack: Boolean) {
        // Эффект "Чай" (id=10)
        val allAllies = if (isPlayerAttacking) playerCards else enemyCards
        val (teaAttackBonus, teaHitAlly, teaAllyTarget) = CardSpecialEffect.checkTeaTrigger(attacker, allAllies)

        var originalTeaAttack = attacker.currentAttack
        if (teaAttackBonus > 0) {
            attacker.currentAttack += teaAttackBonus
            battleStatusText.text = "🍵 ${attacker.originalCard.name} заварил чай! Атака +18! 🍵"
            Toast.makeText(this, "🍵 ${attacker.originalCard.name} атакует с бонусом +18! 🍵", Toast.LENGTH_SHORT).show()
        }

        if (teaHitAlly && teaAllyTarget != null) {
            battleStatusText.text = "💥 ${attacker.originalCard.name} задел своего! Атака на ${teaAllyTarget.originalCard.name}! 💥"
            Toast.makeText(this, "💥 ${attacker.originalCard.name} случайно атакует союзника! 💥", Toast.LENGTH_LONG).show()

            val newTarget = teaAllyTarget
            val newTargetPos = if (isPlayerAttacking) playerCards.indexOf(newTarget) else enemyCards.indexOf(newTarget)

            val newTargetView = if (isPlayerAttacking) {
                playerCardsRecyclerView.findViewHolderForAdapterPosition(newTargetPos)?.itemView
            } else {
                enemyCardsRecyclerView.findViewHolderForAdapterPosition(newTargetPos)?.itemView
            }

            val attackerPos = if (isPlayerAttacking) playerCards.indexOf(attacker) else enemyCards.indexOf(attacker)
            val attackerView = if (isPlayerAttacking) {
                playerCardsRecyclerView.findViewHolderForAdapterPosition(attackerPos)?.itemView
            } else {
                enemyCardsRecyclerView.findViewHolderForAdapterPosition(attackerPos)?.itemView
            }

            if (attackerView != null && newTargetView != null) {
                animateAttack(attackerView, newTargetView) {
                    applyDamageToTarget(attacker, newTarget, isPlayerAttacking, isEnemyAttack, teaAttackBonus)
                }
            } else {
                applyDamageToTarget(attacker, newTarget, isPlayerAttacking, isEnemyAttack, teaAttackBonus)
            }
            return
        }

        applyDamageToTarget(attacker, target, isPlayerAttacking, isEnemyAttack, teaAttackBonus)
    }

    private fun applyDamageToTarget(attacker: GameCard, target: GameCard, isPlayerAttacking: Boolean, isEnemyAttack: Boolean, teaAttackBonus: Int) {
        // Эффект "Стример-неудачник" (id=13) - иллюзия
        val (illusionBonus, isIllusion, originalHealthBeforeBonus) = CardSpecialEffect.checkIllusionTrigger(attacker, target)
        val originalIllusionAttack = attacker.currentAttack
        if (illusionBonus > 0) {
            attacker.currentAttack += illusionBonus
            battleStatusText.text = "🎭 ${attacker.originalCard.name} чувствует прилив сил! Атака +12! 🎭"
            Toast.makeText(this, "🎭 ${attacker.originalCard.name} верит в свою силу! +12! 🎭", Toast.LENGTH_SHORT).show()
        }

        // Лечение "Мастер-класс" (id=7)
        val healAmount = CardSpecialEffect.checkHealTrigger(attacker)
        if (healAmount > 0) {
            val maxHealth = attacker.originalCard.health
            val oldHealth = attacker.currentHealth
            attacker.currentHealth = minOf(attacker.currentHealth + healAmount, maxHealth)
            val actualHeal = attacker.currentHealth - oldHealth

            battleStatusText.text = "💚 ${attacker.originalCard.name} восстановил $actualHeal здоровья! 💚"
            Toast.makeText(this, "💚 ${attacker.originalCard.name} лечится на $actualHeal HP, но пропускает ход! 💚", Toast.LENGTH_LONG).show()

            val attackerPos = if (isPlayerAttacking) playerCards.indexOf(attacker) else enemyCards.indexOf(attacker)
            if (attackerPos != -1) {
                if (isPlayerAttacking) playerAdapter.notifyItemChanged(attackerPos)
                else enemyAdapter.notifyItemChanged(attackerPos)
            }

            if (teaAttackBonus > 0) attacker.currentAttack -= teaAttackBonus
            if (illusionBonus > 0) attacker.currentAttack = originalIllusionAttack

            clearSelection()
            if (isPlayerAttacking) {
                CardSpecialEffect.resetAllFlags(playerCards)
                currentTurn = 1
                turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                battleStatusText.text = getString(R.string.enemy_turn)
                Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 800)
            } else {
                CardSpecialEffect.resetAllFlags(enemyCards)
                currentTurn = 0
                startPlayerTurn()
            }
            isWaitingForTarget = false
            selectedPlayerCard = null
            return
        }

        // Удвоение атаки "Красный дьявол" (id=4)
        val (attackMultiplier, defenseChanged) = CardSpecialEffect.checkDoubleAttackTrigger(attacker)
        val originalAttack = attacker.currentAttack
        if (attackMultiplier == 2) {
            attacker.currentAttack *= 2
            battleStatusText.text = "🔥 ${attacker.originalCard.name} активировал ярость! Атака x2! 🔥"
            Toast.makeText(this, "🔥 ${attacker.originalCard.name} атакует с удвоенной силой! 🔥", Toast.LENGTH_SHORT).show()
        }

        // Бафф атаки "Мини Пекка" (id=6)
        val (attackBonus, defenseReduced) = CardSpecialEffect.checkAttackBuffTrigger(attacker)
        val originalAttackForBuff = attacker.currentAttack
        if (attackBonus > 0) {
            attacker.currentAttack += attackBonus
            battleStatusText.text = "⚔️ ${attacker.originalCard.name} усилился! Атака +10! ⚔️"
            Toast.makeText(this, "⚔️ ${attacker.originalCard.name} атакует с бонусом +10! ⚔️", Toast.LENGTH_SHORT).show()
        }

        // Снижение урона "Сын депутата" (id=5)
        val damageMultiplier = CardSpecialEffect.checkDamageReduction(target)
        var reducedDamageFlag = false
        if (damageMultiplier < 1f) {
            reducedDamageFlag = true
            battleStatusText.text = "🛡️ ${target.originalCard.name} игнорирует 50% урона! 🛡️"
            Toast.makeText(this, "🛡️ ${target.originalCard.name} игнорирует половину урона, но пропускает ход! 🛡️", Toast.LENGTH_LONG).show()
        }

        // Оглушение "Племя потерянных" (id=3)
        val allEnemyCards = if (isPlayerAttacking) enemyCards else playerCards
        val stunnedTarget = CardSpecialEffect.checkStunTrigger(attacker, allEnemyCards)
        if (stunnedTarget != null) {
            stunnedTarget.isStunned = true
            battleStatusText.text = "🔥 ${attacker.originalCard.name} оглушил ${stunnedTarget.originalCard.name} палкой! 🔥"
            Toast.makeText(this, "🔥 ${stunnedTarget.originalCard.name} пропустит следующий ход! 🔥", Toast.LENGTH_LONG).show()
            val stunnedPos = allEnemyCards.indexOf(stunnedTarget)
            if (stunnedPos != -1) {
                if (isPlayerAttacking) enemyAdapter.notifyItemChanged(stunnedPos)
                else playerAdapter.notifyItemChanged(stunnedPos)
            }
        }

        // Расчет урона
        var baseDamage = attacker.currentAttack
        var finalDamage = (baseDamage * damageMultiplier).toInt()
        finalDamage = maxOf(finalDamage, 1)

        var remainingDamage = finalDamage
        var damageToHealth = 0

        if (target.currentDefense > 0) {
            val shieldDamage = minOf(remainingDamage, target.currentDefense)
            target.currentDefense -= shieldDamage
            remainingDamage -= shieldDamage
        }

        if (remainingDamage > 0) {
            damageToHealth = remainingDamage
            target.currentHealth -= damageToHealth
        }

        val displayDamage = finalDamage

        battleStatusText.text = when {
            reducedDamageFlag -> "${attacker.originalCard.name} → $displayDamage урона (50% поглощено!)"
            damageToHealth > 0 && target.currentDefense == 0 -> "${attacker.originalCard.name} → $displayDamage урона (щит сломан!)"
            damageToHealth == 0 && target.currentDefense > 0 -> "${attacker.originalCard.name} → $displayDamage урона (щит поглотил урон)"
            attackMultiplier == 2 -> "${attacker.originalCard.name} → $displayDamage урона (x2 урон!)"
            attackBonus > 0 -> "${attacker.originalCard.name} → $displayDamage урона (+10 к атаке!)"
            teaAttackBonus > 0 -> "${attacker.originalCard.name} → $displayDamage урона (+18 к атаке!)"
            illusionBonus > 0 -> "${attacker.originalCard.name} → $displayDamage урона (+12 к атаке!)"
            else -> "${attacker.originalCard.name} → $displayDamage урона"
        }

        // Восстанавливаем атаку
        if (attackMultiplier == 2) attacker.currentAttack = originalAttack
        if (attackBonus > 0) attacker.currentAttack = originalAttackForBuff
        if (teaAttackBonus > 0) attacker.currentAttack -= teaAttackBonus

        // ЭФФЕКТ ИЛЛЮЗИИ
        if (illusionBonus > 0 && isIllusion) {
            battleStatusText.text = "😵💫 ${attacker.originalCard.name} осознаёт, что это была иллюзия! 😵💫"
            Toast.makeText(this, "😵💫 Это была иллюзия! Здоровье врага возвращается! 😵💫", Toast.LENGTH_LONG).show()

            // Анимация затемнения
            animateIllusion {
                // Восстанавливаем здоровье цели к значению ДО атаки с бонусом
                target.currentHealth = originalHealthBeforeBonus

                // Обновляем UI
                if (isPlayerAttacking) {
                    val targetPos = enemyCards.indexOf(target)
                    if (targetPos != -1) enemyAdapter.notifyItemChanged(targetPos)
                } else {
                    val targetPos = playerCards.indexOf(target)
                    if (targetPos != -1) playerAdapter.notifyItemChanged(targetPos)
                }

                battleStatusText.text = "✨ Иллюзия рассеялась! Урон был только в воображении... ✨"

                // Восстанавливаем атаку после эффекта
                if (illusionBonus > 0) {
                    attacker.currentAttack = originalIllusionAttack
                }

                // Продолжаем ход
                if (isPlayerAttacking) {
                    CardSpecialEffect.resetAllFlags(playerCards)
                    currentTurn = 1
                    isWaitingForTarget = false
                    selectedPlayerCard = null
                    turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                    battleStatusText.text = getString(R.string.enemy_turn)
                    Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 800)
                } else {
                    CardSpecialEffect.resetAllFlags(enemyCards)
                    currentTurn = 0
                    startPlayerTurn()
                }
            }
            return
        }

        // Восстанавливаем атаку после эффекта
        if (illusionBonus > 0) {
            attacker.currentAttack = originalIllusionAttack
        }

        // Смерть цели
        if (target.currentHealth <= 0) {
            target.isAlive = false
            val index = if (isPlayerAttacking) enemyCards.indexOf(target) else playerCards.indexOf(target)
            if (index != -1) {
                if (isPlayerAttacking) {
                    enemyCards.removeAt(index)
                    enemyAdapter.notifyItemRemoved(index)
                } else {
                    playerCards.removeAt(index)
                    playerAdapter.notifyItemRemoved(index)
                }
                battleStatusText.text = "${target.originalCard.name} повержен!"
                if (if (isPlayerAttacking) enemyCards.isEmpty() else playerCards.isEmpty()) {
                    endBattle(isPlayerAttacking)
                    return
                }
            }
            clearSelection()
            if (isPlayerAttacking) {
                CardSpecialEffect.resetAllFlags(playerCards)
                currentTurn = 1
                turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                battleStatusText.text = getString(R.string.enemy_turn)
                Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 800)
            } else {
                CardSpecialEffect.resetAllFlags(enemyCards)
                currentTurn = 0
                startPlayerTurn()
            }
            isWaitingForTarget = false
            selectedPlayerCard = null
            return
        }

        // Обновляем UI цели
        if (isPlayerAttacking) {
            val targetPos = enemyCards.indexOf(target)
            if (targetPos != -1) enemyAdapter.notifyItemChanged(targetPos)
        } else {
            val targetPos = playerCards.indexOf(target)
            if (targetPos != -1) playerAdapter.notifyItemChanged(targetPos)
        }

        clearSelection()

        // Проверяем повторную атаку
        val allSideCards = if (isPlayerAttacking) playerCards else enemyCards
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

            battleStatusText.text = "🔥 Сработал эффект: $effectText! Повторная атака! 🔥"
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()

            Handler(Looper.getMainLooper()).postDelayed({
                val aliveTargets = if (isPlayerAttacking) enemyCards.filter { it.isAlive } else playerCards.filter { it.isAlive }
                if (aliveTargets.isNotEmpty()) {
                    performAttack(attacker, aliveTargets.random(), !isPlayerAttacking)
                } else {
                    endBattle(isPlayerAttacking)
                }
            }, 800)
            return
        }

        // Эффект "Единение с природой" (id=8)
        val (natureAttackBonus, natureDefenseReduced, natureHealthReduced) = CardSpecialEffect.checkNatureBuffTrigger(attacker)
        if (natureAttackBonus > 0) {
            battleStatusText.text = "🌿 ${attacker.originalCard.name} сливается с природой! Атака +10 навсегда! 🌿"
            Toast.makeText(this, "🌿 ${attacker.originalCard.name} получил +10 к атаке! 🌿", Toast.LENGTH_SHORT).show()
            if (natureDefenseReduced || natureHealthReduced) {
                val debuffMsg = mutableListOf<String>()
                if (natureDefenseReduced) debuffMsg.add("защита -5")
                if (natureHealthReduced) debuffMsg.add("здоровье -5")
                battleStatusText.text = "${battleStatusText.text} Но природа забирает: ${debuffMsg.joinToString(", ")}!"
                Toast.makeText(this, "⚠️ ${attacker.originalCard.name} теряет ${debuffMsg.joinToString(", ")}! ⚠️", Toast.LENGTH_SHORT).show()
            }
            val attackerPos = if (isPlayerAttacking) playerCards.indexOf(attacker) else enemyCards.indexOf(attacker)
            if (attackerPos != -1) {
                if (isPlayerAttacking) playerAdapter.notifyItemChanged(attackerPos)
                else enemyAdapter.notifyItemChanged(attackerPos)
            }
        }

        // Эффект "Роланд Азер" (id=9) - глобальный бафф
        val globalBuffTriggered = CardSpecialEffect.checkGlobalAttackBuffTrigger(attacker, allSideCards)
        if (globalBuffTriggered) {
            val sideName = if (isPlayerAttacking) "Ваши" else "Вражеские"
            battleStatusText.text = "👑 ${attacker.originalCard.name} вдохновляет армию! $sideName карты получают +39% к атаке! 👑"
            Toast.makeText(this, "👑 $sideName карты усилены на 39%! 👑", Toast.LENGTH_LONG).show()
            if (isPlayerAttacking) playerAdapter.notifyDataSetChanged()
            else enemyAdapter.notifyDataSetChanged()
        }

        // Эффект "Курсовая" (id=11)
        val allEnemyCardsForKill = if (isPlayerAttacking) enemyCards else playerCards
        val (strongestCard, shouldKillSelf) = CardSpecialEffect.checkKillStrongestTrigger(attacker, allEnemyCardsForKill)

        if (strongestCard != null && shouldKillSelf) {
            battleStatusText.text = "💀 ${attacker.originalCard.name} жертвует собой, чтобы убить ${strongestCard.originalCard.name}! 💀"
            Toast.makeText(this, "💀 ${attacker.originalCard.name} уничтожает сильнейшего врага ценой своей жизни! 💀", Toast.LENGTH_LONG).show()

            strongestCard.currentHealth = 0
            strongestCard.isAlive = false

            val strongestIndex = if (isPlayerAttacking) enemyCards.indexOf(strongestCard) else playerCards.indexOf(strongestCard)
            if (strongestIndex != -1) {
                if (isPlayerAttacking) {
                    enemyCards.removeAt(strongestIndex)
                    enemyAdapter.notifyItemRemoved(strongestIndex)
                } else {
                    playerCards.removeAt(strongestIndex)
                    playerAdapter.notifyItemRemoved(strongestIndex)
                }
            }

            attacker.currentHealth = 0
            attacker.isAlive = false

            val attackerIndex = if (isPlayerAttacking) playerCards.indexOf(attacker) else enemyCards.indexOf(attacker)
            if (attackerIndex != -1) {
                if (isPlayerAttacking) {
                    playerCards.removeAt(attackerIndex)
                    playerAdapter.notifyItemRemoved(attackerIndex)
                } else {
                    enemyCards.removeAt(attackerIndex)
                    enemyAdapter.notifyItemRemoved(attackerIndex)
                }
            }

            if (if (isPlayerAttacking) enemyCards.isEmpty() else playerCards.isEmpty()) {
                endBattle(isPlayerAttacking)
                return
            }
            if (if (isPlayerAttacking) playerCards.isEmpty() else enemyCards.isEmpty()) {
                endBattle(!isPlayerAttacking)
                return
            }

            clearSelection()

            if (isPlayerAttacking) {
                CardSpecialEffect.resetAllFlags(playerCards)
                currentTurn = 1
                turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                battleStatusText.text = getString(R.string.enemy_turn)
                Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 800)
            } else {
                CardSpecialEffect.resetAllFlags(enemyCards)
                currentTurn = 0
                startPlayerTurn()
            }
            return
        }

        // Эффект "Фантазер" (id=12)
        val (dreamerAttackBonus, dreamerSkipTurn, dreamerShouldStun) = CardSpecialEffect.checkDreamerTrigger(attacker)
        val originalDreamerAttack = attacker.currentAttack
        if (dreamerAttackBonus > 0) {
            attacker.currentAttack += dreamerAttackBonus
            battleStatusText.text = "💭 ${attacker.originalCard.name} фантазирует! Атака +20! 💭"
            Toast.makeText(this, "💭 ${attacker.originalCard.name} мечтает о большой атаке! +20! 💭", Toast.LENGTH_SHORT).show()
        }

        if (dreamerAttackBonus > 0 && dreamerShouldStun) {
            battleStatusText.text = "😵 ${attacker.originalCard.name} замечтался и пропускает ход! 😵"
            Toast.makeText(this, "😵 ${attacker.originalCard.name} ушёл в мечты и пропускает ход! 😵", Toast.LENGTH_SHORT).show()

            if (isPlayerAttacking) {
                CardSpecialEffect.resetAllFlags(playerCards)
                currentTurn = 1
                turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
                battleStatusText.text = getString(R.string.enemy_turn)
                Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 800)
            } else {
                CardSpecialEffect.resetAllFlags(enemyCards)
                currentTurn = 0
                startPlayerTurn()
            }
            isWaitingForTarget = false
            selectedPlayerCard = null
            return
        }

        if (dreamerAttackBonus > 0) {
            attacker.currentAttack = originalDreamerAttack
        }

        // Завершение хода
        if (isPlayerAttacking) {
            CardSpecialEffect.resetAllFlags(playerCards)
            currentTurn = 1
            isWaitingForTarget = false
            selectedPlayerCard = null
            turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
            battleStatusText.text = getString(R.string.enemy_turn)
            Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 800)
        } else {
            CardSpecialEffect.resetAllFlags(enemyCards)
            currentTurn = 0
            startPlayerTurn()
        }
    }

    private fun endTurnAfterStun(isPlayerTurn: Boolean) {
        if (isPlayerTurn) {
            CardSpecialEffect.resetAllFlags(playerCards)
            currentTurn = 1
            turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
            battleStatusText.text = getString(R.string.enemy_turn)
            Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 800)
        } else {
            CardSpecialEffect.resetAllFlags(enemyCards)
            currentTurn = 0
            startPlayerTurn()
        }
        isWaitingForTarget = false
        selectedPlayerCard = null
    }

    private fun clearSelection() {
        selectedPlayerCard = null
        isWaitingForTarget = false
        playerCards.forEachIndexed { index, _ ->
            playerCardsRecyclerView.findViewHolderForAdapterPosition(index)?.itemView?.animate()?.alpha(1f)?.scaleX(1f)?.scaleY(1f)?.setDuration(200)?.start()
        }
        enemyCards.forEachIndexed { index, _ ->
            enemyCardsRecyclerView.findViewHolderForAdapterPosition(index)?.itemView?.animate()?.alpha(1f)?.setDuration(200)?.start()
        }
        battleStatusText.text = getString(R.string.your_turn)
    }

    private fun enemyTurn() {
        enemyCards.forEach { it.resetStun() }
        enemyAdapter.notifyDataSetChanged()

        val aliveEnemy = enemyCards.filter { it.isAlive }
        val alivePlayer = playerCards.filter { it.isAlive }

        if (aliveEnemy.isEmpty() || alivePlayer.isEmpty()) {
            endBattle(alivePlayer.isNotEmpty())
            return
        }

        val playableEnemy = aliveEnemy.filter { !it.isStunned }

        if (playableEnemy.isEmpty()) {
            battleStatusText.text = "Все карты противника оглушены! Ход пропускается."
            Toast.makeText(this, "😵 Вражеские карты оглушены! Ход пропущен.", Toast.LENGTH_SHORT).show()
            currentTurn = 0
            startPlayerTurn()
            return
        }

        val attacker = playableEnemy.random()
        val target = alivePlayer.random()
        performAttack(attacker, target, true)
    }

    private fun startPlayerTurn() {
        playerCards.forEach { it.resetStun() }
        playerAdapter.notifyDataSetChanged()

        val alivePlayer = playerCards.filter { it.isAlive }
        val playablePlayer = alivePlayer.filter { !it.isStunned }

        if (alivePlayer.isEmpty()) {
            endBattle(false)
            return
        }

        if (playablePlayer.isEmpty()) {
            battleStatusText.text = "Все ваши карты оглушены! Ход пропускается."
            Toast.makeText(this, "😵 Все карты оглушены! Ход пропущен.", Toast.LENGTH_SHORT).show()
            currentTurn = 1
            turnIndicator.setImageResource(R.drawable.ic_enemy_turn)
            battleStatusText.text = getString(R.string.enemy_turn)
            isWaitingForTarget = false
            selectedPlayerCard = null
            Handler(Looper.getMainLooper()).postDelayed({ enemyTurn() }, 800)
            return
        }

        battleStatusText.text = getString(R.string.your_turn)
    }

    private fun endBattle(playerWon: Boolean) {
        isBattleActive = false
        isAnimating = false
        isProcessingExtraAttack = false
        val message = if (playerWon) getString(R.string.victory) else getString(R.string.defeat)
        val statsPrefs = getSharedPreferences("GameStats", MODE_PRIVATE)
        val totalGames = statsPrefs.getInt("total_games", 0) + 1
        val wins = statsPrefs.getInt("wins", 0) + if (playerWon) 1 else 0
        val defeats = statsPrefs.getInt("defeats", 0) + if (!playerWon) 1 else 0
        statsPrefs.edit().putInt("total_games", totalGames).putInt("wins", wins).putInt("defeats", defeats).apply()
        AlertDialog.Builder(this).setTitle(message).setPositiveButton(getString(R.string.exit)) { _, _ -> finish() }
            .setNegativeButton(getString(R.string.rematch)) { _, _ -> isRematch = true; recreate() }.setCancelable(false).show()
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            AlertDialog.Builder(this).setTitle(getString(R.string.exit_battle)).setMessage(getString(R.string.exit_warning))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    val statsPrefs = getSharedPreferences("GameStats", MODE_PRIVATE)
                    statsPrefs.edit().putInt("total_games", statsPrefs.getInt("total_games", 0) + 1)
                        .putInt("technical_defeats", statsPrefs.getInt("technical_defeats", 0) + 1).apply()
                    finish()
                }.setNegativeButton(getString(R.string.no), null).show()
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this).setTitle(getString(R.string.exit_battle)).setMessage(getString(R.string.exit_warning))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                val statsPrefs = getSharedPreferences("GameStats", MODE_PRIVATE)
                statsPrefs.edit().putInt("total_games", statsPrefs.getInt("total_games", 0) + 1)
                    .putInt("technical_defeats", statsPrefs.getInt("technical_defeats", 0) + 1).apply()
                super.onBackPressed()
            }.setNegativeButton(getString(R.string.no), null).show()
    }
}