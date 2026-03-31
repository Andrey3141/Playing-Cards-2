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
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var disclaimerTitle: TextView
    private lateinit var disclaimerLine: View
    private lateinit var disclaimerText: TextView

    private var handler = Handler(Looper.getMainLooper())
    private var fadeOutRunnable: Runnable? = null
    private var isFinished = false
    private var isTransitioning = false
    private var isUpdateDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UpdateStateManager.init(this)

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

        // Проверяем, нужно ли показать диалог
        if (UpdateStateManager.shouldShowDialog()) {
            // Диалог был активен при прошлом закрытии - показываем снова
            val latestVersion = UpdateStateManager.getLatestVersion()
            val downloadUrl = UpdateStateManager.getDownloadUrl()
            val apkUrl = UpdateStateManager.getApkUrl()

            if (latestVersion != null && downloadUrl != null) {
                isUpdateDialogShowing = true
                cancelFadeOutTimer()
                showUpdateDialog(latestVersion, downloadUrl, apkUrl)
            } else {
                checkForUpdate()
            }
        } else {
            // Проверяем, прошло ли 24 часа
            if (UpdateStateManager.shouldCheckAgain()) {
                checkForUpdate()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // При повороте, если диалог показан, он пересоздастся сам
        if (!isFinished && !isTransitioning && !isUpdateDialogShowing) {
            cancelFadeOutTimer()
            startFadeOutTimer()
        }
    }

    private fun checkForUpdate() {
        if (!isNetworkAvailable()) return

        val updateChecker = UpdateChecker(this) { isAvailable, latestVersion, downloadUrl, apkUrl, changelog ->
            if (isAvailable && downloadUrl != null && !isUpdateDialogShowing && !isFinished && !isTransitioning) {
                isUpdateDialogShowing = true

                // Сохраняем, что диалог активен
                UpdateStateManager.setDialogActive(
                    latestVersion ?: BuildConfig.VERSION_NAME,
                    downloadUrl,
                    apkUrl
                )

                cancelFadeOutTimer()
                showUpdateDialog(latestVersion ?: BuildConfig.VERSION_NAME, downloadUrl, apkUrl)
            }
        }
        updateChecker.checkForUpdates()
    }

    private fun showUpdateDialog(latestVersion: String, downloadUrl: String, apkUrl: String?) {
        val dialog = UpdateDialog(
            this,
            BuildConfig.VERSION_NAME,
            latestVersion
        ) {
            // Нажали "Обновить"
            UpdateStateManager.setDialogInactive()
            isUpdateDialogShowing = false
            val url = apkUrl ?: downloadUrl
            if (url != null) {
                val downloadDialog = DownloadDialog(this, url) {
                    finishAffinity()
                }
                downloadDialog.show()
            }
        }

        dialog.setOnDismissListener {
            // Нажали "Позже" или закрыли диалог
            UpdateStateManager.setDialogInactive()
            isUpdateDialogShowing = false
            startFadeOutTimer()
        }

        dialog.show()
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

    private fun initViews() {
        disclaimerTitle = findViewById(R.id.disclaimerTitle)
        disclaimerLine = findViewById(R.id.disclaimerLine)
        disclaimerText = findViewById(R.id.disclaimerText)
    }

    private fun setupSkipClickListener() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnClickListener {
            if (!isFinished && !isTransitioning && !isUpdateDialogShowing) {
                skipDisclaimer()
            }
        }
    }

    private fun startDisclaimerAnimation() {
        disclaimerTitle.alpha = 0f
        disclaimerTitle.scaleX = 0.8f
        disclaimerTitle.scaleY = 0.8f
        disclaimerTitle.translationY = -50f

        disclaimerLine.alpha = 0f
        disclaimerLine.scaleX = 0f

        disclaimerText.alpha = 0f
        disclaimerText.translationY = 50f

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

        startFadeOutTimer()
    }

    private fun startFadeOutTimer() {
        cancelFadeOutTimer()
        fadeOutRunnable = Runnable {
            if (!isFinished && !isTransitioning && !isUpdateDialogShowing) {
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

        cancelFadeOutTimer()

        val fadeDuration = 1000L

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

    override fun onBackPressed() {}
}