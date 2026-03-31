package com.printer.playingcards2

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadDialog(
    context: Context,
    private val downloadUrl: String,
    private val onComplete: () -> Unit
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private lateinit var cardView: CardView
    private lateinit var titleText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var percentText: TextView
    private lateinit var statusText: TextView
    private lateinit var cancelButton: Button
    private lateinit var loadingIndicator: View

    private var downloadTask: DownloadTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createLayout())

        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        startAnimation()
        startDownload()
    }

    private fun createLayout(): View {
        val root = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.parseColor("#CC000000"))
        }

        cardView = CardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            radius = 32f
            cardElevation = 24f
            setCardBackgroundColor(Color.parseColor("#1E1E2E"))
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
        }

        val cardContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Анимация загрузки (спиннер)
        loadingIndicator = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                bottomMargin = 16
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFD700"))
                cornerRadius = 24f
            }
        }

        titleText = TextView(context).apply {
            text = "⬇️ Загрузка обновления..."
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 24 }
        }

        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                8
            ).apply { bottomMargin = 8 }
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700"))
        }

        percentText = TextView(context).apply {
            text = "0%"
            textSize = 24f
            setTextColor(Color.parseColor("#FFD700"))
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        statusText = TextView(context).apply {
            text = "Подготовка к загрузке..."
            textSize = 14f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 24 }
        }

        cancelButton = Button(context).apply {
            text = "Отмена"
            textSize = 14f
            setTextColor(Color.parseColor("#CCCCCC"))
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                48
            )
            setOnClickListener {
                downloadTask?.cancel(true)
                animateExit { dismiss() }
            }
        }

        cardContent.addView(loadingIndicator)
        cardContent.addView(titleText)
        cardContent.addView(progressBar)
        cardContent.addView(percentText)
        cardContent.addView(statusText)
        cardContent.addView(cancelButton)

        cardView.addView(cardContent)
        root.addView(cardView)

        return root
    }

    private fun startAnimation() {
        cardView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Анимация спиннера
        ObjectAnimator.ofFloat(loadingIndicator, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun startDownload() {
        downloadTask = DownloadTask()
        downloadTask?.execute(downloadUrl)
    }

    private inner class DownloadTask : AsyncTask<String, Int, File>() {

        override fun doInBackground(vararg params: String): File? {
            return try {
                val url = URL(params[0])
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()

                val fileLength = connection.contentLength
                val inputStream = connection.inputStream

                val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PlayingCards2_update.apk")
                if (apkFile.exists()) apkFile.delete()

                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(4096)
                var total = 0
                var count: Int

                while (inputStream.read(buffer).also { count = it } != -1) {
                    if (isCancelled) {
                        apkFile.delete()
                        return null
                    }
                    total += count
                    outputStream.write(buffer, 0, count)
                    val percent = (total * 100 / fileLength).toInt()
                    publishProgress(percent)
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()

                apkFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            val percent = values[0] ?: 0
            progressBar.progress = percent
            percentText.text = "$percent%"
            statusText.text = "Скачано $percent%"
        }

        override fun onPostExecute(result: File?) {
            if (result != null && result.exists()) {
                statusText.text = "Загрузка завершена! Установка..."
                percentText.text = "100%"
                progressBar.progress = 100

                Handler(Looper.getMainLooper()).postDelayed({
                    installApk(result)
                }, 500)
            } else {
                statusText.text = "Ошибка загрузки"
                statusText.setTextColor(Color.parseColor("#E74C3C"))
                cancelButton.text = "Закрыть"
            }
        }

        private fun installApk(apkFile: File) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                onComplete()
                dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
                statusText.text = "Ошибка установки"
                statusText.setTextColor(Color.parseColor("#E74C3C"))
                cancelButton.text = "Закрыть"
            }
        }
    }

    private fun animateExit(onEnd: () -> Unit) {
        cardView.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(300)
            .withEndAction { onEnd() }
            .start()
    }
}