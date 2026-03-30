package com.printer.playingcards2

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class InventoryActivity : AppCompatActivity() {

    private lateinit var backButton: MaterialCardView
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryTitle: TextView
    private lateinit var categoryIcon: ImageView
    private lateinit var cardCount: TextView
    private lateinit var glassCard: CardView
    private lateinit var decorCard1: ImageView
    private lateinit var decorCard2: ImageView
    private lateinit var titleText: TextView

    private lateinit var cardAdapter: CardAdapter
    private var currentCategory = CardCategory.DECK
    private lateinit var sharedPreferences: SharedPreferences
    private val DECK_MAX_SIZE = 3

    // База данных всех карт (25 карт)
    private val allCardsDatabase = listOf(
        // КОЛОДА (DECK) - 3 карты
        Card(
            id = 1,
            name = "Горохострел",
            photoResId = R.drawable.student_1,
            rarity = Rarity.RARE,
            description = "Когда-то он был человеком... Боевая единица, не обладающая большой защитой, но зато наносящая хороший урон",
            specialFeature = "С вероятностью 20% атакует повторно",
            health = 45,
            attack = 30,
            defense = 5,
            category = CardCategory.DECK
        ),
        Card(
            id = 2,
            name = "Древний сожитель",
            photoResId = R.drawable.student_2,
            rarity = Rarity.COMMON,
            description = "Ходят слухи, что именно из-за него начался дефицит табака, но это всего лишь слухи... Так ведь?",
            specialFeature = "С вероятностью 20% все карты атакуют повторно",
            health = 30,
            attack = 1,
            defense = 20,
            category = CardCategory.DECK
        ),
        Card(
            id = 3,
            name = "Племя потерянных",
            photoResId = R.drawable.student_3,
            rarity = Rarity.EPIC,
            description = "Эволюция шла миллионы лет. Эти вернулись к истокам за один вечер. Палка — не оружие. Палка — образ жизни. Один уже 'эволюционировал' слишком далеко. Остальные... догоняют",
            specialFeature = "С вероятностью 35% оглушает случайную карту палкой (пропускает ход)",
            health = 110,
            attack = 4,
            defense = 0,
            category = CardCategory.DECK
        ),

        // КАРТЫ (CATEGORY) - хренова туча карт
        Card(
            id = 4,
            name = "Красный дьявол",
            photoResId = R.drawable.student_4,
            rarity = Rarity.LEGENDARY,
            description = "Когда-то он верил в United... Теперь верит только в наличные",
            specialFeature = "С вероятностью 50% атака x2, но защита -5",
            health = 95,
            attack = 20,
            defense = 6,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 5,
            name = "Сын депутата",
            photoResId = R.drawable.student_5,
            rarity = Rarity.RARE,
            description = "Отказался от папиных денег. Папа отказался от него. Теперь они квиты",
            specialFeature = "С вероятностью 80% игнорирует 50% урона, но не атакует",
            health = 35,
            attack = 10,
            defense = 40,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 6,
            name = "Мини Пекка",
            photoResId = R.drawable.student_6,
            rarity = Rarity.MYTHIC,
            description = "Форма — мечта. Сигарета — реальность",
            specialFeature = "С вероятностью 65% атака +10, но защита -5",
            health = 30,
            attack = 25,
            defense = 50,
            category = CardCategory.CATEGORY,
        ),
        Card(
            id = 7,
            name = "Мастер-класс",
            photoResId = R.drawable.student_7,
            rarity = Rarity.COMMON,
            description = "Дети пойдут в колледж. Он — в столовую",
            specialFeature = "С вероятностью 80% восстанавливает 15 здоровья, но пропускает ход",
            health = 5,
            attack = 25,
            defense = 10,
            category = CardCategory.CATEGORY,
        ),
        Card(
            id = 8,
            name = "Единение с природой",
            photoResId = R.drawable.student_8,
            rarity = Rarity.SUPER_RARE,
            description = "Искал природу. Нашёл лошадь. Лошадь не оценила",
            specialFeature = "С вероятностью 50% +10 к атаке на весь бой, но 40% шанс получить -5 к защите и здоровью",
            health = 65,
            attack = 20,
            defense = 0,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 9,
            name = "Роланд Азер",
            photoResId = R.drawable.student_9,
            rarity = Rarity.LEGENDARY,
            description = "Был самым жестоким военачальником. После неудачного покушения странным образом исчез. По данным британской разведки его след был утерян у границы Польши. По не официальным источникам может быть до сих пор жив",
            specialFeature = "С вероятностью 60% увеличивает урон всем картам на 39% от их первоначального урона",
            health = 30,
            attack = 40,
            defense = 20,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 10,
            name = "Чай",
            photoResId = R.drawable.student_10,
            rarity = Rarity.EPIC,
            description = "Устроил крестовый поход. На Фурманову. Завалил. Не её",
            specialFeature = "С вероятностью 65% атака +18, но 60% шанс задеть своих",
            health = 30,
            attack = 30,
            defense = 20,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 11,
            name = "Курсовая",
            photoResId = R.drawable.student_11,
            rarity = Rarity.SUPER_RARE,
            description = "Не написал сам, поэтому нашёл тех, кто сделает. Оказалось их не интересуют наличные",
            specialFeature = "С вероятностью 90% убивает самую сильную карту противника, но при этом сам умирает",
            health = 100,
            attack = 18,
            defense = 20,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 12,
            name = "Фантазер",
            photoResId = R.drawable.student_12,
            rarity = Rarity.RARE,
            description = "Какая отличная у него задница. Вот бы он меня отымел",
            specialFeature = "С вероятностью 50% атака +20, но 40% шанс пропуск хода",
            health = 80,
            attack = 25,
            defense = 10,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 13,
            name = "Стример-неудачник",
            photoResId = R.drawable.student_13,
            rarity = Rarity.RARE,
            description = "Мечтал о Twitch. Получил Discord. Мечтал о подписчиках. Получил второго отца",
            specialFeature = "С вероятностью 40% +12, но 70% шанс что это только ему кажется",
            health = 90,
            attack = 14,
            defense = 12,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 14,
            name = "Стимпанк из подвала",
            photoResId = R.drawable.student_14,
            rarity = Rarity.EPIC,
            description = "Сделал из того что было. Выглядело круто, пока не понял что ничего не видит",
            specialFeature = "С вероятностью 45% все характеристики +5, но 30% шанс промахнуться",
            health = 85,
            attack = 8,
            defense = 25,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 15,
            name = "Киберпанк",
            photoResId = R.drawable.student_15,
            rarity = Rarity.MYTHIC,
            description = "Будущее уже здесь. Просто от кого-то оно чуть дальше",
            specialFeature = "С вероятностью 55% игнорирует атаки от легендарных карт",
            health = 120,
            attack = 12,
            defense = 18,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 16,
            name = "ИИсус 2.0",
            photoResId = R.drawable.student_16,
            rarity = Rarity.EPIC,
            description = "Ты был прекрасен, как Иисус\n" +
                          "В произведениях искусств\n" +
                          "Я думала, что вознесусь\n" +
                          "От красоты или от чувств",
            specialFeature = "С вероятностью 100% ослепляет все карты и атака производится случайно. Ослепление действует 3 атаки. Сама же карта исчезает",
            health = 70,
            attack = 22,
            defense = 8,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 17,
            name = "Эмоциональная стабильность",
            photoResId = R.drawable.student_17,
            rarity = Rarity.SUPER_RARE,
            description = "Блять, а я утюг выключил?",
            specialFeature = "С вероятностью 60% атакует именно выбранную карту",
            health = 95,
            attack = 5,
            defense = 25,
            category = CardCategory.CATEGORY
        ),
        Card(
            id = 18,
            name = "Дети чернобыля",
            photoResId = R.drawable.student_18,
            rarity = Rarity.EPIC,
            description = "Главное такое ночью не увидеть",
            specialFeature = "С вероятностью 85% игнорирует атаки от редких карт",
            health = 150,
            attack = 30,
            defense = 20,
            category = CardCategory.CATEGORY
        ),

        // НЕДОСТУПНО (UNAVAILABLE) - пару карт
        Card(
            id = 19,
            name = "Староста",
            photoResId = R.drawable.student_19,
            rarity = Rarity.EPIC,
            description = "Следит за порядком. У неё везде свои люди",
            specialFeature = "Сеть информаторов — видит планы врага",
            health = 100,
            attack = 10,
            defense = 30,
            category = CardCategory.UNAVAILABLE,
            isUnlocked = false
        ),
        Card(
            id = 20,
            name = "Загадочный незнакомец",
            photoResId = R.drawable.student_20,
            rarity = Rarity.MYTHIC,
            description = "Никто не знает, кто он. Но все знают — лучше не злить",
            specialFeature = "Тайна — случайный эффект",
            health = 120,
            attack = 25,
            defense = 25,
            category = CardCategory.UNAVAILABLE,
            isUnlocked = false
        ),
        Card(
            id = 21,
            name = "Легенда универа",
            photoResId = R.drawable.student_21,
            rarity = Rarity.LEGENDARY,
            description = "О нём слагают легенды. Говорят, он видел 5-й корпус",
            specialFeature = "Мифическая аура — враги боятся",
            health = 250,
            attack = 40,
            defense = 40,
            category = CardCategory.UNAVAILABLE,
            isUnlocked = false
        ),
        Card(
            id = 22,
            name = "Тот самый",
            photoResId = R.drawable.student_22,
            rarity = Rarity.MYTHIC,
            description = "Тот самый, кого ищут все. Но лучше не находить",
            specialFeature = "Невидимость — 50% уклониться",
            health = 190,
            attack = 35,
            defense = 30,
            category = CardCategory.UNAVAILABLE,
            isUnlocked = false
        ),
        Card(
            id = 23,
            name = "Босс",
            photoResId = R.drawable.student_23,
            rarity = Rarity.LEGENDARY,
            description = "Главный в общаге. Все приносят ему дань",
            specialFeature = "Сбор дани — крадёт 10% характеристик",
            health = 300,
            attack = 25,
            defense = 50,
            category = CardCategory.UNAVAILABLE,
            isUnlocked = false
        ),
        Card(
            id = 24,
            name = "Тень",
            photoResId = R.drawable.student_24,
            rarity = Rarity.EPIC,
            description = "Есть только в сумерках. Или в подвале",
            specialFeature = "Скрытность — первая атака неуязвим",
            health = 140,
            attack = 38,
            defense = 15,
            category = CardCategory.UNAVAILABLE,
            isUnlocked = false
        ),
        Card(
            id = 25,
            name = "Финальный босс",
            photoResId = R.drawable.student_25,
            rarity = Rarity.CUSTOM,
            description = "Победить его невозможно. Но можно попробовать",
            specialFeature = "Абсолютная сила — всё или ничего",
            health = 500,
            attack = 60,
            defense = 60,
            category = CardCategory.UNAVAILABLE,
            isUnlocked = false
        )
    )

    // Текущее состояние карт (изменяемое)
    private lateinit var currentCards: MutableList<Card>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        // Скрываем системную панель
        hideSystemUI()

        sharedPreferences = getSharedPreferences("CardGamePrefs", Context.MODE_PRIVATE)
        initViews()
        loadCardsState()
        startEntranceAnimation()
        setupTabs()
        setupRecyclerView()
        setupBackButton()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        categoryTitle = findViewById(R.id.categoryTitle)
        categoryIcon = findViewById(R.id.categoryIcon)
        glassCard = findViewById(R.id.glassCard)
        decorCard1 = findViewById(R.id.decorCard1)
        decorCard2 = findViewById(R.id.decorCard2)
        titleText = findViewById(R.id.titleText)

        try {
            cardCount = findViewById(R.id.cardCount)
        } catch (e: Exception) {
            // Игнорируем
        }
    }

    private fun loadCardsState() {
        val gson = Gson()
        val json = sharedPreferences.getString("cards_state", null)
        val type = object : TypeToken<MutableList<Card>>() {}.type

        currentCards = if (json != null) {
            gson.fromJson(json, type)
        } else {
            // Если нет сохраненного состояния, используем базу данных
            allCardsDatabase.toMutableList()
        }
    }

    private fun saveCardsState() {
        val gson = Gson()
        val json = gson.toJson(currentCards)
        sharedPreferences.edit().putString("cards_state", json).apply()
    }

    private fun startEntranceAnimation() {
        // Анимация стеклянной карточки
        glassCard.alpha = 0f
        glassCard.scaleX = 0.8f
        glassCard.scaleY = 0.8f

        ObjectAnimator.ofFloat(glassCard, "alpha", 0f, 0.3f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofFloat(glassCard, "scaleX", 0.8f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofFloat(glassCard, "scaleY", 0.8f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            start()
        }

        // Анимация декоративных карт
        decorCard1.alpha = 0f
        decorCard1.animate()
            .alpha(0.15f)
            .setDuration(1000)
            .setInterpolator(DecelerateInterpolator())
            .start()

        decorCard2.alpha = 0f
        decorCard2.animate()
            .alpha(0.15f)
            .setDuration(1000)
            .setStartDelay(200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Анимация кнопки назад
        backButton.alpha = 0f
        backButton.translationX = -100f
        backButton.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(600)
            .setInterpolator(AnticipateOvershootInterpolator())
            .start()

        // Анимация заголовка
        titleText.alpha = 0f
        titleText.translationY = -50f
        titleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(200)
            .setInterpolator(BounceInterpolator())
            .start()

        // Анимация табов
        tabLayout.alpha = 0f
        tabLayout.translationY = 100f
        tabLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun setupTabs() {
        tabLayout.removeAllTabs()
        tabLayout.addTab(tabLayout.newTab().setText("Колода"))
        tabLayout.addTab(tabLayout.newTab().setText("Карты"))
        tabLayout.addTab(tabLayout.newTab().setText("Недоступно"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentCategory = CardCategory.DECK
                        categoryTitle.text = "Моя колода"
                        categoryIcon.setImageResource(R.drawable.ic_deck)
                        animateCategoryChange()
                    }
                    1 -> {
                        currentCategory = CardCategory.CATEGORY
                        categoryTitle.text = "Мои карты"
                        categoryIcon.setImageResource(R.drawable.ic_category)
                        animateCategoryChange()
                    }
                    2 -> {
                        currentCategory = CardCategory.UNAVAILABLE
                        categoryTitle.text = "Недоступные карты"
                        categoryIcon.setImageResource(R.drawable.ic_unavailable)
                        animateCategoryChange()
                    }
                }
                updateRecyclerView()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun animateCategoryChange() {
        // Анимация смены категории
        categoryTitle.alpha = 0f
        categoryTitle.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        categoryIcon.alpha = 0f
        categoryIcon.scaleX = 0.5f
        categoryIcon.scaleY = 0.5f
        categoryIcon.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(BounceInterpolator())
            .start()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        val filteredCards = currentCards.filter { it.category == currentCategory }

        updateCardCount(filteredCards)

        cardAdapter = CardAdapter(
            cards = filteredCards,
            onItemClick = { card ->
                handleCardClick(card)
            },
            onItemLongClick = { card ->
                // Показываем диалог с возможностью активации анимации
                CardDetailDialog(this, card) { activatedCard ->
                    // Активируем анимацию для карты
                    val index = currentCards.indexOfFirst { it.id == activatedCard.id }
                    if (index != -1) {
                        currentCards[index].animationActivated = true
                        saveCardsState()
                        updateRecyclerView()
                    }
                }.show()
            },
            onAnimationStateChanged = { card ->
                // Колбэк для обновления состояния анимации
            }
        )
        recyclerView.adapter = cardAdapter
    }

    private fun handleCardClick(card: Card) {
        val deckCards = currentCards.filter { it.category == CardCategory.DECK }

        when (card.category) {
            CardCategory.DECK -> {
                // Перемещаем из колоды в карты
                moveCard(card, CardCategory.CATEGORY)
            }
            CardCategory.CATEGORY -> {
                // Проверяем лимит колоды
                if (deckCards.size >= DECK_MAX_SIZE) {
                    Toast.makeText(
                        this,
                        "В колоде может быть максимум $DECK_MAX_SIZE карт!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Перемещаем из карт в колоду
                    moveCard(card, CardCategory.DECK)
                }
            }
            else -> {
                // Для недоступных карт ничего не делаем
                showQuickInfo(card)
            }
        }
    }

    private fun moveCard(card: Card, newCategory: CardCategory) {
        val index = currentCards.indexOfFirst { it.id == card.id }
        if (index != -1) {
            val updatedCard = card.copy(category = newCategory)
            currentCards[index] = updatedCard
            saveCardsState()
            updateRecyclerView()

            // Анимация и уведомление
            Toast.makeText(
                this,
                "Карта перемещена в ${getCategoryName(newCategory)}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getCategoryName(category: CardCategory): String {
        return when (category) {
            CardCategory.DECK -> "колоду"
            CardCategory.CATEGORY -> "карты"
            CardCategory.UNAVAILABLE -> "недоступные"
        }
    }

    private fun updateCardCount(cards: List<Card>) {
        try {
            if (::cardCount.isInitialized) {
                val deckCards = currentCards.filter { it.category == CardCategory.DECK }
                when (currentCategory) {
                    CardCategory.DECK -> {
                        val cardCountText = when (deckCards.size) {
                            1 -> "1 карта"
                            in 2..4 -> "${deckCards.size} карты"
                            else -> "${deckCards.size} карт"
                        }
                        cardCount.text = "$cardCountText / $DECK_MAX_SIZE"
                    }
                    else -> {
                        val cardCountText = when (cards.size) {
                            1 -> "1 карта"
                            in 2..4 -> "${cards.size} карты"
                            else -> "${cards.size} карт"
                        }
                        cardCount.text = cardCountText
                    }
                }
            }
        } catch (e: Exception) {
            // Игнорируем
        }
    }

    private fun showQuickInfo(card: Card) {
        android.app.AlertDialog.Builder(this)
            .setTitle(card.name)
            .setMessage("${card.rarity.displayName}\n❤️ ${card.health}  ⚔️ ${card.attack}  🛡️ ${card.defense}")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            backButton.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    finish()
                }
                .start()
        }
    }

    override fun onBackPressed() {
        backButton.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                super.onBackPressed()
            }
            .start()
    }

    override fun onPause() {
        super.onPause()
        saveCardsState() // Сохраняем при выходе из активности
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
}