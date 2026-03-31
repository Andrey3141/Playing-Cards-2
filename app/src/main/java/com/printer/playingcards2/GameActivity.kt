package com.printer.playingcards2

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
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
    lateinit var turnIndicator: ImageView
    lateinit var battleStatusText: TextView

    private lateinit var enemyCardsRecyclerView: RecyclerView
    private lateinit var playerCardsRecyclerView: RecyclerView

    lateinit var playerCards: MutableList<GameCard>
    lateinit var enemyCards: MutableList<GameCard>
    private lateinit var allCardsDatabase: List<Card>

    lateinit var playerAdapter: GameCardAdapter
    lateinit var enemyAdapter: GameCardAdapter

    var isBattleActive = false
    var currentTurn = 0 // 0 - игрок, 1 - враг
    var selectedPlayerCard: GameCard? = null
    var isWaitingForTarget = false

    private var isProcessingExtraAttack = false
    private var isRematch = false
    private var isAnimating = false

    lateinit var damageCalculator: DamageCalculator

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
        loadAllCards()

        damageCalculator = DamageCalculator(this)

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

    fun performAttack(attacker: GameCard, target: GameCard, isEnemyAttack: Boolean = false) {
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

        val attackerViewHolder = if (isPlayerAttacking) {
            playerCardsRecyclerView.findViewHolderForAdapterPosition(attackerPos)
        } else {
            enemyCardsRecyclerView.findViewHolderForAdapterPosition(attackerPos)
        }

        val targetViewHolder = if (isPlayerAttacking) {
            enemyCardsRecyclerView.findViewHolderForAdapterPosition(targetPos)
        } else {
            playerCardsRecyclerView.findViewHolderForAdapterPosition(targetPos)
        }

        val attackerView = attackerViewHolder?.itemView
        val targetView = targetViewHolder?.itemView

        if (attackerView == null || targetView == null) {
            applyDamage(attacker, target, isPlayerAttacking, isEnemyAttack)
        } else {
            animateAttack(attackerView, targetView) {
                applyDamage(attacker, target, isPlayerAttacking, isEnemyAttack)
            }
        }
    }

    private fun applyDamage(attacker: GameCard, target: GameCard, isPlayerAttacking: Boolean, isEnemyAttack: Boolean) {
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
                    damageCalculator.applyDamage(attacker, newTarget, isPlayerAttacking, isEnemyAttack, teaAttackBonus)
                }
            } else {
                damageCalculator.applyDamage(attacker, newTarget, isPlayerAttacking, isEnemyAttack, teaAttackBonus)
            }
            return
        }

        damageCalculator.applyDamage(attacker, target, isPlayerAttacking, isEnemyAttack, teaAttackBonus)
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

    fun clearSelection() {
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

    fun enemyTurn() {
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

        Handler(Looper.getMainLooper()).postDelayed({
            performAttack(attacker, target, true)
        }, 100)
    }

    fun startPlayerTurn() {
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

    fun endBattle(playerWon: Boolean) {
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