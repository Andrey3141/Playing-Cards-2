package com.printer.playingcards2

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var playButton: Button
    private lateinit var inventoryButton: Button
    private lateinit var shopButton: Button
    private lateinit var settingsButton: Button
    private lateinit var playCard: CardView
    private lateinit var inventoryCard: CardView
    private lateinit var shopCard: CardView
    private lateinit var settingsCard: CardView
    private lateinit var card1: ImageView
    private lateinit var card2: ImageView

    private var updateDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        initViews()
        setupWindowInsets()
        startEntranceAnimation()
        setupButtonListeners()
        setupCardAnimations()

        // Проверяем обновления
        checkForUpdate()
    }

    override fun onResume() {
        super.onResume()
        // При возврате в приложение проверяем обновления, если диалог еще не показывали
        if (!updateDialogShown) {
            checkForUpdate()
        }
    }

    private fun checkForUpdate() {
        if (!isNetworkAvailable()) return

        val updateChecker = UpdateChecker(this) { isAvailable, latestVersion, downloadUrl, apkUrl, changelog ->
            if (isAvailable && downloadUrl != null && !updateDialogShown) {
                updateDialogShown = true
                val currentVersion = BuildConfig.VERSION_NAME

                val updateDialog = UpdateDialog(
                    this,
                    currentVersion,
                    latestVersion ?: BuildConfig.VERSION_NAME
                ) {
                    val downloadUrlToUse = apkUrl ?: downloadUrl
                    if (downloadUrlToUse != null) {
                        val downloadDialog = DownloadDialog(this, downloadUrlToUse) {
                            finishAffinity()
                        }
                        downloadDialog.show()
                    }
                }
                updateDialog.show()
            }
        }
        updateChecker.checkForUpdates()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private fun showUpdateDialog(latestVersion: String, downloadUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("🔄 Доступно обновление!")
            .setMessage("Версия $latestVersion уже доступна.\n\nХотите перейти на GitHub и скачать новую версию?")
            .setPositiveButton("Обновить") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                startActivity(intent)
            }
            .setNegativeButton("Позже", null)
            .setCancelable(true)
            .show()
    }

    private fun initViews() {
        titleText = findViewById(R.id.titleText)
        subtitleText = findViewById(R.id.subtitleText)
        playButton = findViewById(R.id.playButton)
        inventoryButton = findViewById(R.id.inventoryButton)
        shopButton = findViewById(R.id.shopButton)
        settingsButton = findViewById(R.id.settingsButton)
        playCard = findViewById(R.id.playCard)
        inventoryCard = findViewById(R.id.inventoryCard)
        shopCard = findViewById(R.id.shopCard)
        settingsCard = findViewById(R.id.settingsCard)
        card1 = findViewById(R.id.card1)
        card2 = findViewById(R.id.card2)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startEntranceAnimation() {
        // Анимация декоративных карт
        card1.alpha = 0f
        card1.animate()
            .alpha(0.2f)
            .setDuration(1000)
            .setInterpolator(DecelerateInterpolator())
            .start()

        card2.alpha = 0f
        card2.animate()
            .alpha(0.2f)
            .setDuration(1000)
            .setStartDelay(200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Анимация заголовка
        titleText.alpha = 0f
        titleText.scaleX = 0.5f
        titleText.scaleY = 0.5f
        titleText.translationY = -200f

        titleText.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(AnticipateOvershootInterpolator())
            .start()

        // Анимация подзаголовка
        subtitleText.alpha = 0f
        subtitleText.translationY = 50f

        subtitleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Анимация кнопок
        animateButtonWithSpring(playCard, 0)
        animateButtonWithSpring(inventoryCard, 150)
        animateButtonWithSpring(shopCard, 300)
        animateButtonWithSpring(settingsCard, 450)
    }

    private fun animateButtonWithSpring(button: CardView, delay: Long) {
        button.alpha = 0f
        button.scaleX = 0.3f
        button.scaleY = 0.3f
        button.translationY = 200f

        button.postDelayed({
            button.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(BounceInterpolator())
                .start()
        }, delay)
    }

    private fun setupButtonListeners() {
        playButton.setOnClickListener {
            animateButtonClick(it) {
                val dialog = GameModeDialog(this)
                dialog.show()
            }
        }

        inventoryButton.setOnClickListener {
            animateButtonClick(it) {
                val intent = Intent(this, InventoryActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        shopButton.setOnClickListener {
            animateButtonClick(it) {
                Toast.makeText(this, "В этой игре все бесплатно, наслаждайся!!!", Toast.LENGTH_SHORT).show()
            }
        }

        settingsButton.setOnClickListener {
            animateButtonClick(it) {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    private fun animateButtonClick(view: View, action: () -> Unit) {
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 0.95f),
            PropertyValuesHolder.ofFloat("scaleY", 0.95f)
        )
        scaleDown.duration = 100

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f)
        )
        scaleUp.duration = 200
        scaleUp.interpolator = BounceInterpolator()

        scaleDown.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}

            override fun onAnimationEnd(animation: android.animation.Animator) {
                scaleUp.start()
                action.invoke()
            }
        })

        scaleDown.start()
    }

    private fun setupCardAnimations() {
        animateFloatingCard(card1, -15f, 3000)
        animateFloatingCard(card2, 20f, 3500)
    }

    private fun animateFloatingCard(card: ImageView, rotation: Float, duration: Long) {
        val animator = ObjectAnimator.ofFloat(
            card,
            "translationY",
            card.translationY,
            card.translationY + 30f,
            card.translationY - 30f,
            card.translationY
        )
        animator.duration = duration
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.interpolator = DecelerateInterpolator()
        animator.start()

        val rotationAnim = ObjectAnimator.ofFloat(
            card,
            "rotation",
            rotation,
            rotation + 3f,
            rotation - 3f,
            rotation
        )
        rotationAnim.duration = (duration * 1.5).toLong()
        rotationAnim.repeatCount = ObjectAnimator.INFINITE
        rotationAnim.interpolator = DecelerateInterpolator()
        rotationAnim.start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
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