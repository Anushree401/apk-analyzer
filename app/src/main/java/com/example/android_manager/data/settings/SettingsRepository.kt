package com.example.android_manager.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.android_manager.data.history.ScanHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "apvm_settings")

data class AppSettings(
    val showSystemApps: Boolean = false,
    val autoRefresh: Boolean = true,
    val hapticFeedback: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val isAmoledMode: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private val historyRepository = ScanHistoryRepository(context)

    companion object {
        val KEY_SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_AMOLED_MODE = booleanPreferencesKey("amoled_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                showSystemApps = preferences[KEY_SHOW_SYSTEM_APPS] ?: false,
                autoRefresh = preferences[KEY_AUTO_REFRESH] ?: true,
                hapticFeedback = preferences[KEY_HAPTIC_FEEDBACK] ?: true,
                onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false,
                isAmoledMode = preferences[KEY_AMOLED_MODE] ?: true
            )
        }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_AMOLED_MODE] = enabled
        }
    }

    suspend fun setShowSystemApps(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_SHOW_SYSTEM_APPS] = enabled
        }
    }

    suspend fun setAutoRefresh(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_AUTO_REFRESH] = enabled
        }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun clearScanHistory(): Boolean = withContext(Dispatchers.IO) {
        historyRepository.clearHistory()
    }

    suspend fun clearApkCache(): Long = withContext(Dispatchers.IO) {
        var bytesFreed = 0L
        try {
            val cacheFiles = context.cacheDir.listFiles() ?: emptyArray()
            cacheFiles.forEach { file ->
                if (file.name.startsWith("apvm_scan_") || file.extension.equals("apk", ignoreCase = true)) {
                    val size = file.length()
                    if (file.delete()) {
                        bytesFreed += size
                    }
                }
            }
        } catch (_: Exception) {
            // Best effort cache purge
        }
        bytesFreed
    }

    fun getCrashLog(): String {
        val logFile = File(context.cacheDir, "apvm_crash.log")
        return if (logFile.exists()) {
            try { logFile.readText() } catch (_: Exception) { "" }
        } else {
            ""
        }
    }

    fun clearCrashLog(): Boolean {
        val logFile = File(context.cacheDir, "apvm_crash.log")
        return if (logFile.exists()) logFile.delete() else true
    }
}
