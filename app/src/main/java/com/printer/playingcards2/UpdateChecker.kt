package com.printer.playingcards2

import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.util.Log
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val html_url: String,
    val assets: List<Asset>? = null
)

data class Asset(
    val browser_download_url: String,
    val name: String
)

class UpdateChecker(private val context: Context, private val onResult: (Boolean, String?, String?, String?, String?) -> Unit) {

    companion object {
        private const val PREFS_NAME = "update_prefs"
        private const val LAST_CHECK_TIME = "last_check_time"
        private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 часа
    }

    fun checkForUpdates(forceCheck: Boolean = false) {
        Log.d("UpdateChecker", "checkForUpdates() вызван, forceCheck=$forceCheck")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = UpdateStateManager.getLastCheckTime()
        val now = System.currentTimeMillis()

        if (!forceCheck && (now - lastCheck) < CHECK_INTERVAL_MS) {
            Log.d("UpdateChecker", "Последняя проверка была менее 24 часов назад, пропускаем")
            onResult(false, null, null, null, null)
            return
        }

        UpdateStateManager.setLastCheckTime(now)

        prefs.edit().putLong(LAST_CHECK_TIME, now).apply()

        CheckUpdateTask().execute()
    }

    private inner class CheckUpdateTask : AsyncTask<Void, Void, GitHubRelease?>() {

        override fun doInBackground(vararg params: Void): GitHubRelease? {
            return try {
                val url = URL("https://api.github.com/repos/Andrey3141/Playing-Cards-2/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val json = inputStream.bufferedReader().use { it.readText() }
                    Gson().fromJson(json, GitHubRelease::class.java)
                } else {
                    Log.e("UpdateChecker", "GitHub API вернул код: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("UpdateChecker", "Ошибка проверки обновления", e)
                null
            }
        }

        override fun onPostExecute(release: GitHubRelease?) {
            if (release == null) {
                onResult(false, null, null, null, null)
                return
            }

            val currentVersion = getCurrentVersion()
            val latestVersion = release.tag_name.replace("v", "")
            val isUpdateAvailable = compareVersions(currentVersion, latestVersion) < 0

            // Получаем прямую ссылку на APK (если есть в ассетах)
            val apkUrl = release.assets?.find { it.name.endsWith(".apk") }?.browser_download_url

            // Получаем changelog из body релиза
            val changelog = release.body

            onResult(isUpdateAvailable, latestVersion, release.html_url, apkUrl, changelog)
        }

        private fun getCurrentVersion(): String {
            return BuildConfig.VERSION_NAME
        }

        private fun compareVersions(v1: String, v2: String): Int {
            val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLength = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLength) {
                val num1 = if (i < parts1.size) parts1[i] else 0
                val num2 = if (i < parts2.size) parts2[i] else 0
                if (num1 != num2) return num1.compareTo(num2)
            }
            return 0
        }
    }
}