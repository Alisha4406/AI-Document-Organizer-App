package com.example.aidocumentorganizer.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

/**
 * Single source of truth for user-facing app preferences.
 *
 * Preferences are stored with Jetpack DataStore rather than synchronous
 * SharedPreferences, so settings updates are safe and observable.
 */
class AppPreferences(context: Context) {

    private val dataStore = context.applicationContext.appSettingsDataStore

    val darkMode: Flow<Boolean?> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE]
    }

    val remindersEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_REMINDERS] ?: true
    }

    suspend fun setDarkMode(value: Boolean?) {
        dataStore.edit { prefs ->
            if (value == null) prefs.remove(KEY_DARK_MODE)
            else prefs[KEY_DARK_MODE] = value
        }
    }

    suspend fun setRemindersEnabled(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_REMINDERS] = value
        }
    }

    private companion object {
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_REMINDERS = booleanPreferencesKey("reminders_enabled")
    }
}
