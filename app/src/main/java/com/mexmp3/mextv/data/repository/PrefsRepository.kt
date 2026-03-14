package com.mexmp3.mextv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mexmp3.mextv.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

class PrefsRepository(private val context: Context) {

    private val ONBOARDING_DONE = booleanPreferencesKey(Constants.KEY_ONBOARDING_DONE)
    private val THEME = stringPreferencesKey(Constants.KEY_THEME)
    private val SHUFFLE = booleanPreferencesKey(Constants.KEY_SHUFFLE)
    private val REPEAT_MODE = intPreferencesKey(Constants.KEY_REPEAT_MODE)
    private val QUEUE_JSON = stringPreferencesKey(Constants.KEY_QUEUE)
    private val CURRENT_SONG_ID = longPreferencesKey(Constants.KEY_CURRENT_SONG_ID)
    private val GAPLESS = booleanPreferencesKey(Constants.KEY_GAPLESS)
    private val SLEEP_TIMER = intPreferencesKey(Constants.KEY_SLEEP_TIMER_MINUTES)
    private val EQ_ENABLED       = booleanPreferencesKey(Constants.KEY_EQ_ENABLED)
    private val EQ_PRESET        = stringPreferencesKey(Constants.KEY_EQ_PRESET)
    private val EQ_CUSTOM_BANDS  = stringPreferencesKey(Constants.KEY_EQ_CUSTOM_BANDS)

    private fun <T> Flow<T>.safeFlow(default: T) = catch { e ->
        if (e is IOException) emit(default as T)
        else throw e
    }

    val isOnboardingDone: Flow<Boolean> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[ONBOARDING_DONE] ?: false }

    val themeName: Flow<String> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[THEME] ?: Constants.THEME_MILITARY }

    val shuffleEnabled: Flow<Boolean> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[SHUFFLE] ?: false }

    val repeatMode: Flow<Int> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[REPEAT_MODE] ?: Constants.REPEAT_NONE }

    val queueJson: Flow<String> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[QUEUE_JSON] ?: "" }

    val currentSongId: Flow<Long> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[CURRENT_SONG_ID] ?: -1L }

    val gaplessEnabled: Flow<Boolean> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[GAPLESS] ?: true }

    val sleepTimerMinutes: Flow<Int> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[SLEEP_TIMER] ?: 0 }

    val eqEnabled: Flow<Boolean> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[EQ_ENABLED] ?: false }

    val eqPreset: Flow<String> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[EQ_PRESET] ?: "Flat" }

    /** Comma-separated dB floats for the Custom preset, e.g. "0.0,3.5,-2.0,1.0,0.0" */
    val eqCustomBands: Flow<String> =
        context.dataStore.data.safeFlow(emptyPreferences()).map { it[EQ_CUSTOM_BANDS] ?: "0,0,0,0,0" }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[ONBOARDING_DONE] = done }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME] = theme }
    }

    suspend fun setShuffle(enabled: Boolean) {
        context.dataStore.edit { it[SHUFFLE] = enabled }
    }

    suspend fun setRepeatMode(mode: Int) {
        context.dataStore.edit { it[REPEAT_MODE] = mode }
    }

    suspend fun setQueueJson(json: String) {
        context.dataStore.edit { it[QUEUE_JSON] = json }
    }

    suspend fun setCurrentSongId(id: Long) {
        context.dataStore.edit { it[CURRENT_SONG_ID] = id }
    }

    suspend fun setGapless(enabled: Boolean) {
        context.dataStore.edit { it[GAPLESS] = enabled }
    }

    suspend fun setSleepTimerMinutes(minutes: Int) {
        context.dataStore.edit { it[SLEEP_TIMER] = minutes }
    }

    suspend fun setEqEnabled(enabled: Boolean) {
        context.dataStore.edit { it[EQ_ENABLED] = enabled }
    }

    suspend fun setEqPreset(preset: String) {
        context.dataStore.edit { it[EQ_PRESET] = preset }
    }

    suspend fun setEqCustomBands(bands: List<Float>) {
        context.dataStore.edit { it[EQ_CUSTOM_BANDS] = bands.joinToString(",") }
    }
}
