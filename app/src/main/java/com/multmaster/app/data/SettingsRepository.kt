package com.multmaster.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "mm_settings")

data class AppSettings(
    val loaded: Boolean = false,
    val theme: String = "system",      // system | light | dark
    val language: String = "en",       // en | hi
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val timer: Boolean = true,
    val instantFeedback: Boolean = true,
    val dailyQuestionGoal: Int = 30,
    val dailyMinuteGoal: Int = 10,
    val difficulty: String = "normal", // easy | normal | hard
    val reminders: Boolean = false,
    val onboarded: Boolean = false
)

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    val current: StateFlow<AppSettings> = store.data
        .map { p ->
            AppSettings(
                loaded = true,
                theme = p[THEME] ?: "system",
                language = p[LANG] ?: "en",
                sound = p[SOUND] ?: true,
                haptics = p[HAPTICS] ?: true,
                timer = p[TIMER] ?: true,
                instantFeedback = p[FEEDBACK] ?: true,
                dailyQuestionGoal = p[QGOAL] ?: 30,
                dailyMinuteGoal = p[MGOAL] ?: 10,
                difficulty = p[DIFF] ?: "normal",
                reminders = p[REMIND] ?: false,
                onboarded = p[ONBOARDED] ?: false
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    fun setTheme(v: String) = put { it[THEME] = v }
    fun setLanguage(v: String) = put { it[LANG] = v }
    fun setSound(v: Boolean) = put { it[SOUND] = v }
    fun setHaptics(v: Boolean) = put { it[HAPTICS] = v }
    fun setTimer(v: Boolean) = put { it[TIMER] = v }
    fun setInstantFeedback(v: Boolean) = put { it[FEEDBACK] = v }
    fun setDailyQuestionGoal(v: Int) = put { it[QGOAL] = v }
    fun setDailyMinuteGoal(v: Int) = put { it[MGOAL] = v }
    fun setDifficulty(v: String) = put { it[DIFF] = v }
    fun setReminders(v: Boolean) = put { it[REMIND] = v }
    fun setOnboarded(v: Boolean) = put { it[ONBOARDED] = v }

    private fun put(block: (MutablePreferences) -> Unit) {
        scope.launch { store.edit { block(it) } }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val LANG = stringPreferencesKey("lang")
        val SOUND = booleanPreferencesKey("sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val TIMER = booleanPreferencesKey("timer")
        val FEEDBACK = booleanPreferencesKey("feedback")
        val QGOAL = intPreferencesKey("qgoal")
        val MGOAL = intPreferencesKey("mgoal")
        val DIFF = stringPreferencesKey("difficulty")
        val REMIND = booleanPreferencesKey("reminders")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }
}