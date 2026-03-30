package com.printer.playingcards2

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.roundToInt
import kotlin.random.Random

class SettingsActivity : AppCompatActivity() {

    private lateinit var backButton: MaterialCardView
    private lateinit var titleText: TextView
    private lateinit var tabPromo: TextView
    private lateinit var tabCredits: TextView
    private lateinit var tabStats: TextView
    private lateinit var promoContainer: LinearLayout
    private lateinit var creditsContainer: LinearLayout
    private lateinit var statsContainer: LinearLayout
    private lateinit var promoEditText: EditText
    private lateinit var activateButton: MaterialButton
    private lateinit var promoResult: TextView
    private lateinit var creditsRecyclerView: RecyclerView
    private lateinit var creditsAdapter: CreditsAdapter
    private lateinit var balloonContainer: FrameLayout

    // Элементы статистики
    private lateinit var winRateCircle: View
    private lateinit var winRatePercent: TextView
    private lateinit var totalGamesText: TextView
    private lateinit var winsText: TextView
    private lateinit var defeatsText: TextView
    private lateinit var technicalDefeatsText: TextView
    private lateinit var cardsCollectedText: TextView
    private lateinit var refreshButton: MaterialCardView

    // Для трехцветного круга
    private lateinit var winsSector: View
    private lateinit var defeatsSector: View
    private lateinit var technicalSector: View

    private var currentTab = 0
    private lateinit var statsPrefs: SharedPreferences
    private lateinit var gamePrefs: SharedPreferences
    private var hackMode = false

    // Список промокодов для отображения
    private val availablePromoCodes = listOf(
        "31.03.2026",
        "hack",
        "normal",
        "reset",
        "comment"
    )

    private val creditsList = listOf(
        CreditItem("Разработчик", "Скачков Андрей Юрьевич", R.drawable.ic_dev),
        CreditItem("Дизайнер", "Скачков Андрей Юрьевич", R.drawable.ic_design),
        CreditItem("Художник", "Скачков Андрей Юрьевич", R.drawable.ic_artist),
        CreditItem("Музыка (которой нету)", "Скачков Андрей Юрьевич", R.drawable.ic_music),
        CreditItem("Тестировщик", "Сиваков Сергей Владимирович\nСкачков Андрей Юрьевич\nЧаюков Дмитрий Сергеевич", R.drawable.ic_test),
        CreditItem("Особая благодарность", "Хочу выразить благодарность самому себе, что вместо подготовки к гос экзамену делал эту игру, а также Чаюкову Дмитрию и Сивакову Сергею за активное участие в тестирование приложения", R.drawable.ic_thanks),
        CreditItem("Гитхаб", "Перейти на github разработчика", R.drawable.ic_github, "https://github.com/Andrey3141"),
        CreditItem("Версия", "1.2.0", R.drawable.ic_version)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        hideSystemUI()

        statsPrefs = getSharedPreferences("GameStats", MODE_PRIVATE)
        gamePrefs = getSharedPreferences("CardGamePrefs", MODE_PRIVATE)
        hackMode = statsPrefs.getBoolean("hack_mode", false)

        initViews()
        setupTabs()
        setupPromoCode()
        setupCredits()
        setupStats()
        setupBackButton()
        animateEntrance()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        titleText = findViewById(R.id.titleText)
        tabPromo = findViewById(R.id.tabPromo)
        tabCredits = findViewById(R.id.tabCredits)
        tabStats = findViewById(R.id.tabStats)
        promoContainer = findViewById(R.id.promoContainer)
        creditsContainer = findViewById(R.id.creditsContainer)
        statsContainer = findViewById(R.id.statsContainer)
        promoEditText = findViewById(R.id.promoEditText)
        activateButton = findViewById(R.id.activateButton)
        promoResult = findViewById(R.id.promoResult)
        creditsRecyclerView = findViewById(R.id.creditsRecyclerView)
        balloonContainer = findViewById(R.id.balloonContainer)

        // Статистика
        winRateCircle = findViewById(R.id.winRateCircle)
        winRatePercent = findViewById(R.id.winRatePercent)
        totalGamesText = findViewById(R.id.totalGamesText)
        winsText = findViewById(R.id.winsText)
        defeatsText = findViewById(R.id.defeatsText)
        technicalDefeatsText = findViewById(R.id.technicalDefeatsText)
        cardsCollectedText = findViewById(R.id.cardsCollectedText)
        refreshButton = findViewById(R.id.refreshButton)

        // Трехцветные сектора
        winsSector = findViewById(R.id.winsSector)
        defeatsSector = findViewById(R.id.defeatsSector)
        technicalSector = findViewById(R.id.technicalSector)

        setupPromoCodesList()
    }

    private fun setupPromoCodesList() {
        val promoListContainer = findViewById<LinearLayout>(R.id.promoListContainer)
        promoListContainer?.removeAllViews()

        for (code in availablePromoCodes) {
            val codeView = TextView(this).apply {
                text = code
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@SettingsActivity, android.R.color.white))
                setPadding(16, 12, 16, 12)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#33FFFFFF"))
                    cornerRadius = 8f
                    setStroke(1, Color.parseColor("#80FFFFFF"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
                gravity = android.view.Gravity.CENTER
            }
            promoListContainer?.addView(codeView)
        }
    }

    private fun setupTabs() {
        tabPromo.setOnClickListener {
            if (currentTab != 0) {
                currentTab = 0
                updateTabSelection()
                showPromoContainer()
                animateTabChange(tabPromo)
            }
        }

        tabCredits.setOnClickListener {
            if (currentTab != 1) {
                currentTab = 1
                updateTabSelection()
                showCreditsContainer()
                animateTabChange(tabCredits)
            }
        }

        tabStats.setOnClickListener {
            if (currentTab != 2) {
                currentTab = 2
                updateTabSelection()
                showStatsContainer()
                animateTabChange(tabStats)
            }
        }

        updateTabSelection()
        showPromoContainer()
    }

    private fun updateTabSelection() {
        val selectedColor = ContextCompat.getColor(this, android.R.color.white)
        val unselectedColor = ContextCompat.getColor(this, android.R.color.white).let { it and 0x80FFFFFF.toInt() }

        if (currentTab == 0) {
            tabPromo.setBackgroundResource(R.drawable.bg_tab_selected_vertical)
            tabPromo.setTextColor(selectedColor)
            tabCredits.setBackgroundResource(R.drawable.bg_tab_default_vertical)
            tabCredits.setTextColor(unselectedColor)
            tabStats.setBackgroundResource(R.drawable.bg_tab_default_vertical)
            tabStats.setTextColor(unselectedColor)
        } else if (currentTab == 1) {
            tabCredits.setBackgroundResource(R.drawable.bg_tab_selected_vertical)
            tabCredits.setTextColor(selectedColor)
            tabPromo.setBackgroundResource(R.drawable.bg_tab_default_vertical)
            tabPromo.setTextColor(unselectedColor)
            tabStats.setBackgroundResource(R.drawable.bg_tab_default_vertical)
            tabStats.setTextColor(unselectedColor)
        } else {
            tabStats.setBackgroundResource(R.drawable.bg_tab_selected_vertical)
            tabStats.setTextColor(selectedColor)
            tabPromo.setBackgroundResource(R.drawable.bg_tab_default_vertical)
            tabPromo.setTextColor(unselectedColor)
            tabCredits.setBackgroundResource(R.drawable.bg_tab_default_vertical)
            tabCredits.setTextColor(unselectedColor)
        }
    }

    private fun animateTabChange(tab: TextView) {
        tab.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(150)
            .withEndAction {
                tab.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun showPromoContainer() {
        promoContainer.visibility = View.VISIBLE
        creditsContainer.visibility = View.GONE
        statsContainer.visibility = View.GONE

        promoContainer.alpha = 0f
        promoContainer.translationY = 20f
        promoContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun showCreditsContainer() {
        promoContainer.visibility = View.GONE
        creditsContainer.visibility = View.VISIBLE
        statsContainer.visibility = View.GONE

        creditsContainer.alpha = 0f
        creditsContainer.translationY = 20f
        creditsContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun showStatsContainer() {
        promoContainer.visibility = View.GONE
        creditsContainer.visibility = View.GONE
        statsContainer.visibility = View.VISIBLE

        statsContainer.alpha = 0f
        statsContainer.translationY = 20f
        statsContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        updateStats()
    }

    private fun animateEntrance() {
        val views = listOf(backButton, titleText, tabPromo.parent as View)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 30f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(index * 100L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupPromoCode() {
        promoEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                promoResult.text = ""
                promoResult.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        activateButton.setOnClickListener {
            val code = promoEditText.text.toString().trim().lowercase()
            if (code.isEmpty()) {
                showPromoError("Введите промокод")
                return@setOnClickListener
            }
            checkPromoCode(code)
        }
    }

    private fun checkPromoCode(code: String) {
        activateButton.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                activateButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()

        when (code) {
            "31.03.2026" -> {
                showPromoSuccess("🎈 Промокод активирован! 🎈")
                showBalloonAnimation()
            }
            "hack" -> {
                if (!hackMode) {
                    hackMode = true
                    statsPrefs.edit().putBoolean("hack_mode", true).apply()
                    // Устанавливаем все значения в 999
                    statsPrefs.edit()
                        .putInt("total_games", 999)
                        .putInt("wins", 999)
                        .putInt("defeats", 999)
                        .putInt("technical_defeats", 999)
                        .apply()
                    showPromoSuccess("💀 ХАК-РЕЖИМ АКТИВИРОВАН! 💀")
                    updateStats()
                } else {
                    showPromoError("Хак-режим уже активирован!")
                }
            }
            "normal" -> {
                if (hackMode) {
                    hackMode = false
                    statsPrefs.edit().putBoolean("hack_mode", false).apply()
                    // Сбрасываем статистику в ноль
                    statsPrefs.edit()
                        .putInt("total_games", 0)
                        .putInt("wins", 0)
                        .putInt("defeats", 0)
                        .putInt("technical_defeats", 0)
                        .apply()
                    showPromoSuccess("🔓 Режим NORMAL активирован! Статистика сброшена. 🔓")
                    updateStats()
                } else {
                    showPromoError("Хак-режим не активирован!")
                }
            }
            "reset" -> {
                showResetConfirmationDialog()
            }
            "comment" -> {
                showReviewsDialog()
                showPromoSuccess("📝 Спасибо за отзыв! 📝")
            }
            else -> {
                showPromoError("Недействительный промокод")
            }
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ СБРОС ИГРЫ ⚠️")
            .setMessage("ВНИМАНИЕ! Это действие:\n\n" +
                    "• Удалит все сохраненные карты\n" +
                    "• Сбросит статистику\n" +
                    "• Удалит промокоды\n" +
                    "• Вернет игру к заводским настройкам\n\n" +
                    "Это действие НЕОБРАТИМО!\n\n" +
                    "Вы уверены?")
            .setPositiveButton("ДА, СБРОСИТЬ") { _, _ ->
                performReset()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performReset() {
        // Сбрасываем хак-режим
        hackMode = false
        statsPrefs.edit().clear().apply()

        // Сбрасываем сохраненные карты
        gamePrefs.edit().clear().apply()

        // Восстанавливаем стандартную колоду
        val gson = Gson()
        val defaultCards = createDefaultCards()
        val json = gson.toJson(defaultCards)
        gamePrefs.edit().putString("cards_state", json).apply()

        showPromoSuccess("💥 Игра сброшена до заводских настроек! 💥")
        Toast.makeText(this, "Приложение будет перезапущено для применения настроек", Toast.LENGTH_LONG).show()

        // Перезапускаем приложение
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finishAffinity()
        }, 2000)
    }

    private fun createDefaultCards(): MutableList<Card> {
        return mutableListOf(
            Card(1, "Горохострел", R.drawable.student_1, Rarity.RARE, "Когда-то он был человеком... Боевая единица, не обладающая большой защитой, но зато наносящая хороший урон", "С вероятностью 20% атакует повторно", 45, 30, 5, CardCategory.DECK),
            Card(2, "Древний сожитель", R.drawable.student_2, Rarity.COMMON, "Ходят слухи, что именно из-за него начался дефицит табака, но это всего лишь слухи... Так ведь?", "С вероятностью 20% все карты атакуют повторно", 30, 1, 20, CardCategory.DECK),
            Card(3, "Племя потерянных", R.drawable.student_3, Rarity.EPIC, "Эволюция шла миллионы лет. Эти вернулись к истокам за один вечер. Палка — не оружие. Палка — образ жизни.", "С вероятностью 35% оглушает случайную карту палкой (пропускает ход)", 110, 4, 0, CardCategory.DECK),
            Card(4, "Красный дьявол", R.drawable.student_4, Rarity.LEGENDARY, "Когда-то он верил в United... Теперь верит только в наличные", "С вероятностью 50% атака x2, но защита -5", 95, 20, 6, CardCategory.CATEGORY),
            Card(5, "Сын депутата", R.drawable.student_5, Rarity.RARE, "Отказался от папиных денег. Папа отказался от него.", "С вероятностью 80% игнорирует 50% урона, но пропускает ход", 35, 10, 40, CardCategory.CATEGORY),
            Card(6, "Мини Пекка", R.drawable.student_6, Rarity.MYTHIC, "Форма — мечта. Сигарета — реальность", "С вероятностью 65% атака +10, но защита -5", 30, 25, 50, CardCategory.CATEGORY),
            Card(7, "Мастер-класс", R.drawable.student_7, Rarity.COMMON, "Дети пойдут в колледж. Он — в столовую", "С вероятностью 80% восстанавливает 15 здоровья, но пропускает ход", 5, 25, 10, CardCategory.CATEGORY),
            Card(8, "Единение с природой", R.drawable.student_8, Rarity.SUPER_RARE, "Искал природу. Нашёл лошадь.", "С вероятностью 50% +10 к атаке на весь бой, но 40% шанс получить -5 к защите и здоровью", 65, 20, 0, CardCategory.CATEGORY),
            Card(9, "Роланд Азер", R.drawable.student_9, Rarity.LEGENDARY, "Был самым жестоким военачальником.", "С вероятностью 60% увеличивает урон всем картам на 39%", 30, 40, 20, CardCategory.CATEGORY),
            Card(10, "Чай", R.drawable.student_10, Rarity.EPIC, "Устроил крестовый поход. На Фурманову.", "С вероятностью 65% атака +18, но 60% шанс задеть своих", 30, 30, 20, CardCategory.CATEGORY)
        )
    }

    private fun showBalloonAnimation() {
        balloonContainer.visibility = View.VISIBLE
        balloonContainer.bringToFront()

        val colors = listOf(
            "#FF5252", "#FF4081", "#7C4DFF", "#448AFF",
            "#4CAF50", "#FFC107", "#FF9800", "#9C27B0"
        )

        for (i in 0..15) {
            Handler(Looper.getMainLooper()).postDelayed({
                createBalloon(colors[Random.nextInt(colors.size)])
            }, i * 150L)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            balloonContainer.visibility = View.GONE
            balloonContainer.removeAllViews()
        }, 5000)
    }

    private fun createBalloon(colorHex: String) {
        val balloon = ImageView(this)
        balloon.setImageResource(R.drawable.ic_balloon)
        balloon.colorFilter = android.graphics.PorterDuffColorFilter(
            Color.parseColor(colorHex),
            android.graphics.PorterDuff.Mode.SRC_ATOP
        )

        val size = (60 + Random.nextInt(40)).dpToPx()
        val startX = Random.nextInt(balloonContainer.width - size).coerceAtLeast(0)
        val startY = balloonContainer.height

        val params = FrameLayout.LayoutParams(size, size)
        params.leftMargin = startX
        params.topMargin = startY
        balloon.layoutParams = params
        balloonContainer.addView(balloon)

        val animator = ValueAnimator.ofFloat(startY.toFloat(), -size.toFloat())
        animator.duration = (2000 + Random.nextInt(2000)).toLong()
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            val y = it.animatedValue as Float
            balloon.y = y
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                balloonContainer.removeView(balloon)
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        animator.start()

        balloon.animate()
            .rotation(360f)
            .setDuration(2000)
            .start()
    }

    private fun showReviewsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reviews, null)

        val review1Name = dialogView.findViewById<TextView>(R.id.review1Name)
        val review1Text = dialogView.findViewById<TextView>(R.id.review1Text)
        val review1Rating = dialogView.findViewById<View>(R.id.review1Rating)
        val review1Stars = dialogView.findViewById<TextView>(R.id.review1Stars)
        val response1Name = dialogView.findViewById<TextView>(R.id.response1Name)
        val response1Text = dialogView.findViewById<TextView>(R.id.response1Text)

        val review2Name = dialogView.findViewById<TextView>(R.id.review2Name)
        val review2Text = dialogView.findViewById<TextView>(R.id.review2Text)
        val review2Rating = dialogView.findViewById<View>(R.id.review2Rating)
        val review2Stars = dialogView.findViewById<TextView>(R.id.review2Stars)
        val response2Name = dialogView.findViewById<TextView>(R.id.response2Name)
        val response2Text = dialogView.findViewById<TextView>(R.id.response2Text)

        val review3Name = dialogView.findViewById<TextView>(R.id.review3Name)
        val review3Text = dialogView.findViewById<TextView>(R.id.review3Text)
        val review3Rating = dialogView.findViewById<View>(R.id.review3Rating)
        val review3Stars = dialogView.findViewById<TextView>(R.id.review3Stars)
        val response3Name = dialogView.findViewById<TextView>(R.id.response3Name)
        val response3Text = dialogView.findViewById<TextView>(R.id.response3Text)

        // Первый отзыв - негативный
        review1Name.text = "Картонный кот"
        review1Text.text = "Игра в карточки 2. Обосрался жидким 2. Очередная хуйня, сделанная на коленке самым активным юзером нейросети, который из программирования знает только как накатить линукс через ту же нейросеть"
        review1Stars.text = "★★★★★"
        review1Stars.setTextColor(Color.parseColor("#FFD700"))
        review1Rating.setBackgroundColor(Color.parseColor("#4CAF50"))

        response1Name.text = "👨‍💻 Разработчик"
        response1Text.text = "Да пошел ты в жопу! Игра заебись и блять ты нихуя не понимаешь в искусстве, поэтому съебался, животное. И спасибо что активно пользуетесь нашей игрой и за хороший отзыв. Стараемся для вас!!!"

        // Второй отзыв - положительный
        review2Name.text = "Серый"
        review2Text.text = "Я долбаеб"
        review2Stars.text = "★★★☆☆"
        review2Stars.setTextColor(Color.parseColor("#FFD700"))
        review2Rating.setBackgroundColor(Color.parseColor("#4CAF50"))

        response2Name.text = "👨‍💻 Разработчик"
        response2Text.text = "Мы это знали. А хули оценка низкая? Хотя аргумент достойный️"

        // Третий отзыв - конструктивный
        review3Name.text = "Kofanger"
        review3Text.text = "Игра хуйня, на карточки квартиру слил, ничего не получил, отпиздили меня всей семьей в переулке 10 из 10"
        review3Stars.text = "★★★★★"
        review3Stars.setTextColor(Color.parseColor("#FFD700"))
        review3Rating.setBackgroundColor(Color.parseColor("#FF9800"))

        response3Name.text = "👨‍💻 Разработчик"
        response3Text.text = "Блять, а вы еще живы? Наша вина, приносим извинения и скоро исправим. И благодарим на хороший отзыв!"

        AlertDialog.Builder(this)
            .setTitle("📝 ОТЗЫВЫ ИГРОКОВ")
            .setView(dialogView)
            .setPositiveButton("Закрыть") { _, _ -> }
            .setNegativeButton("Оставить отзыв") { _, _ ->
                Toast.makeText(this, "Бюджета хватило только на кнопку, но спасибо что пытались", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun showPromoSuccess(message: String) {
        promoResult.text = message
        promoResult.setTextColor(ContextCompat.getColor(this, R.color.rarity_legendary))
        promoResult.visibility = View.VISIBLE

        promoResult.scaleX = 0.8f
        promoResult.scaleY = 0.8f
        promoResult.alpha = 0f
        promoResult.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(BounceInterpolator())
            .start()
    }

    private fun showPromoError(message: String) {
        promoResult.text = message
        promoResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        promoResult.visibility = View.VISIBLE

        promoResult.animate()
            .translationX(-8f)
            .setDuration(50)
            .withEndAction {
                promoResult.animate()
                    .translationX(8f)
                    .setDuration(50)
                    .withEndAction {
                        promoResult.animate()
                            .translationX(0f)
                            .setDuration(50)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun setupCredits() {
        creditsAdapter = CreditsAdapter(creditsList)
        creditsRecyclerView.layoutManager = LinearLayoutManager(this)
        creditsRecyclerView.adapter = creditsAdapter
    }

    private fun setupStats() {
        refreshButton.setOnClickListener {
            if (!hackMode) {
                animateRefreshButton()
                updateStats()
            } else {
                Toast.makeText(this, "🔒 Хак-режим: статистика заблокирована", Toast.LENGTH_SHORT).show()
            }
        }
        updateStats()
    }

    private fun updateStats() {
        if (hackMode) {
            totalGamesText.text = "999"
            winsText.text = "999"
            defeatsText.text = "999"
            technicalDefeatsText.text = "999"
            cardsCollectedText.text = "999 / 999"
            winRatePercent.text = "999%"

            winsSector.rotation = 0f
            defeatsSector.rotation = 360f
            technicalSector.rotation = 0f
            return
        }

        val totalGames = statsPrefs.getInt("total_games", 0)
        val wins = statsPrefs.getInt("wins", 0)
        val defeats = statsPrefs.getInt("defeats", 0)
        val technicalDefeats = statsPrefs.getInt("technical_defeats", 0)

        val gson = Gson()
        val json = gamePrefs.getString("cards_state", null)
        val type = object : TypeToken<MutableList<Card>>() {}.type
        val allCards: MutableList<Card> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        val collectedCards = allCards.count { it.isUnlocked }
        val totalCards = allCards.size

        totalGamesText.text = totalGames.toString()
        winsText.text = wins.toString()
        defeatsText.text = defeats.toString()
        technicalDefeatsText.text = technicalDefeats.toString()
        cardsCollectedText.text = "$collectedCards / $totalCards"

        val total = wins + defeats + technicalDefeats

        val winsPercent = if (total > 0) wins.toFloat() / total else 0f
        val defeatsPercent = if (total > 0) defeats.toFloat() / total else 0f
        val technicalPercent = if (total > 0) technicalDefeats.toFloat() / total else 0f

        val winsAngle = (winsPercent * 360).roundToInt()
        val defeatsAngle = (defeatsPercent * 360).roundToInt()
        val technicalAngle = (technicalPercent * 360).roundToInt()

        val totalAngle = winsAngle + defeatsAngle + technicalAngle
        val correctedWinsAngle = if (totalAngle != 360) winsAngle + (360 - totalAngle) else winsAngle

        val winRate = if (total > 0) (winsPercent * 100).roundToInt() else 0
        winRatePercent.text = "$winRate%"

        animateSector(winsSector, correctedWinsAngle, 0)
        animateSector(defeatsSector, defeatsAngle, correctedWinsAngle)
        animateSector(technicalSector, technicalAngle, correctedWinsAngle + defeatsAngle)

        winRateCircle.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).withEndAction {
            winRateCircle.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        }.start()
    }

    private fun animateSector(sector: View, targetAngle: Int, startAngle: Int) {
        sector.rotation = startAngle.toFloat()
        val rotationAnim = ObjectAnimator.ofFloat(sector, "rotation", startAngle.toFloat(), (startAngle + targetAngle).toFloat())
        rotationAnim.duration = 1000
        rotationAnim.interpolator = AnticipateOvershootInterpolator()
        rotationAnim.start()

        sector.alpha = 0f
        sector.scaleX = 0f
        sector.scaleY = 0f
        sector.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start()
    }

    private fun animateRefreshButton() {
        refreshButton.animate()
            .rotation(360f)
            .setDuration(500)
            .withEndAction {
                refreshButton.rotation = 0f
            }
            .start()
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

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
}