package com.printer.playingcards2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var disclaimerTitle: TextView
    private lateinit var disclaimerLine: View
    private lateinit var disclaimerText: TextView

    private var handler = Handler(Looper.getMainLooper())
    private var fadeOutRunnable: Runnable? = null
    private var isFinished = false
    private var isTransitioning = false
    private var pendingUpdateDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Полноэкранный режим
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        }

        setContentView(R.layout.activity_splash)

        initViews()
        startDisclaimerAnimation()
        setupSkipClickListener()

        // Проверяем обновления
        checkForUpdate()
    }

    private fun checkForUpdate() {
        // Проверяем интернет
        if (!isNetworkAvailable()) {
            // Нет интернета — просто запускаем таймер
            return
        }

        val updateChecker = UpdateChecker(this) { isAvailable, latestVersion, downloadUrl ->
            if (isAvailable && downloadUrl != null && !pendingUpdateDialog && !isFinished && !isTransitioning) {
                pendingUpdateDialog = true
                // Отменяем таймер перехода
                cancelFadeOutTimer()

                showUpdateDialog(latestVersion ?: "неизвестная", downloadUrl)
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
                // Открываем браузер
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                startActivity(intent)
                // После открытия браузера всё равно переходим в главное меню
                finishAndGoToMain()
            }
            .setNegativeButton("Позже") { _, _ ->
                pendingUpdateDialog = false
                // Возвращаем таймер
                startFadeOutTimer()
            }
            .setCancelable(false)
            .show()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // При повороте отменяем старый таймер и запускаем новый
        if (!isFinished && !isTransitioning && !pendingUpdateDialog) {
            cancelFadeOutTimer()
            startFadeOutTimer()
        }
    }

    private fun initViews() {
        disclaimerTitle = findViewById(R.id.disclaimerTitle)
        disclaimerLine = findViewById(R.id.disclaimerLine)
        disclaimerText = findViewById(R.id.disclaimerText)
    }

    private fun setupSkipClickListener() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnClickListener {
            if (!isFinished && !isTransitioning && !pendingUpdateDialog) {
                skipDisclaimer()
            }
        }
    }

    private fun startDisclaimerAnimation() {
        // Начальное состояние
        disclaimerTitle.alpha = 0f
        disclaimerTitle.scaleX = 0.8f
        disclaimerTitle.scaleY = 0.8f
        disclaimerTitle.translationY = -50f

        disclaimerLine.alpha = 0f
        disclaimerLine.scaleX = 0f

        disclaimerText.alpha = 0f
        disclaimerText.translationY = 50f

        // Анимация появления
        disclaimerTitle.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(1500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        disclaimerLine.animate()
            .alpha(1f)
            .scaleX(1f)
            .setDuration(1000)
            .setStartDelay(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        disclaimerText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(1200)
            .setStartDelay(1800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Запускаем таймер исчезновения
        startFadeOutTimer()
    }

    private fun startFadeOutTimer() {
        cancelFadeOutTimer()
        fadeOutRunnable = Runnable {
            if (!isFinished && !isTransitioning && !pendingUpdateDialog) {
                startFadeOutAnimation()
            }
        }
        handler.postDelayed(fadeOutRunnable!!, 6000)
    }

    private fun cancelFadeOutTimer() {
        fadeOutRunnable?.let { handler.removeCallbacks(it) }
        fadeOutRunnable = null
    }

    private fun startFadeOutAnimation() {
        if (isFinished || isTransitioning) return
        isTransitioning = true

        // Отменяем таймер на всякий случай
        cancelFadeOutTimer()

        val fadeDuration = 1000L

        // Все анимации запускаются одновременно
        disclaimerTitle.animate()
            .alpha(0f)
            .scaleX(0.6f)
            .scaleY(0.6f)
            .translationY(-80f)
            .setDuration(fadeDuration)
            .start()

        disclaimerLine.animate()
            .alpha(0f)
            .scaleX(0f)
            .setDuration(fadeDuration)
            .start()

        disclaimerText.animate()
            .alpha(0f)
            .translationY(80f)
            .setDuration(fadeDuration)
            .start()

        handler.postDelayed({
            finishAndGoToMain()
        }, fadeDuration)
    }

    private fun skipDisclaimer() {
        if (isFinished || isTransitioning) return
        isFinished = true
        isTransitioning = true

        cancelFadeOutTimer()

        finishAndGoToMain()
    }

    private fun finishAndGoToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        // Кнопка назад не работает
    }
}