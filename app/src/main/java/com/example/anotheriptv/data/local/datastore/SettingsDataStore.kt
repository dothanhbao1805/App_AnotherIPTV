package com.example.anotheriptv.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val QUALITY_KEY = stringPreferencesKey("quality")
        val BACKGROUND_PLAY_KEY = booleanPreferencesKey("background_play")
    }

    // ── Read ──
    val theme: Flow<String> = context.dataStore.data
        .map { it[THEME_KEY] ?: "system" }

    val language: Flow<String> = context.dataStore.data
        .map { it[LANGUAGE_KEY] ?: "vi" }

    val quality: Flow<String> = context.dataStore.data
        .map { it[QUALITY_KEY] ?: "auto" }

    val backgroundPlay: Flow<Boolean> = context.dataStore.data
        .map { it[BACKGROUND_PLAY_KEY] ?: false }

    // ── Write ──
    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[THEME_KEY] = value }
    }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = value }
    }

    suspend fun setQuality(value: String) {
        context.dataStore.edit { it[QUALITY_KEY] = value }
    }

    suspend fun setBackgroundPlay(value: Boolean) {
        context.dataStore.edit { it[BACKGROUND_PLAY_KEY] = value }
    }
}