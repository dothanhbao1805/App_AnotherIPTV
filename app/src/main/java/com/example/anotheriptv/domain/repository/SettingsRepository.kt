package com.example.anotheriptv.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val theme: Flow<String>
    val language: Flow<String>
    val quality: Flow<String>
    val backgroundPlay: Flow<Boolean>

    suspend fun setTheme(value: String)
    suspend fun setLanguage(value: String)
    suspend fun setQuality(value: String)
    suspend fun setBackgroundPlay(value: Boolean)
}