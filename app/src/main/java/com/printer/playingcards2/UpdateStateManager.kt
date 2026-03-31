package com.printer.playingcards2

import android.content.Context
import android.content.SharedPreferences

object UpdateStateManager {

    private const val PREFS_NAME = "update_state"
    private const val KEY_DIALOG_ACTIVE = "dialog_active"
    private const val KEY_LATEST_VERSION = "latest_version"
    private const val KEY_DOWNLOAD_URL = "download_url"
    private const val KEY_APK_URL = "apk_url"
    private const val KEY_LAST_DISMISS_TIME = "last_dismiss_time"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setDialogActive(version: String, downloadUrl: String, apkUrl: String?) {
        prefs.edit()
            .putBoolean(KEY_DIALOG_ACTIVE, true)
            .putString(KEY_LATEST_VERSION, version)
            .putString(KEY_DOWNLOAD_URL, downloadUrl)
            .putString(KEY_APK_URL, apkUrl)
            .apply()
    }

    fun setDialogInactive() {
        prefs.edit()
            .putBoolean(KEY_DIALOG_ACTIVE, false)
            .putLong(KEY_LAST_DISMISS_TIME, System.currentTimeMillis())
            .apply()
    }

    fun shouldShowDialog(): Boolean {
        return prefs.getBoolean(KEY_DIALOG_ACTIVE, false)
    }

    fun getLatestVersion(): String? = prefs.getString(KEY_LATEST_VERSION, null)
    fun getDownloadUrl(): String? = prefs.getString(KEY_DOWNLOAD_URL, null)
    fun getApkUrl(): String? = prefs.getString(KEY_APK_URL, null)

    fun shouldCheckAgain(): Boolean {
        val lastDismiss = prefs.getLong(KEY_LAST_DISMISS_TIME, 0)
        val now = System.currentTimeMillis()
        return (now - lastDismiss) > 24 * 60 * 60 * 1000L
    }

    // Методы для UpdateChecker
    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK_TIME, 0)
    }

    fun setLastCheckTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, time).apply()
    }
}