package com.example.anotheriptv


import android.app.Application
import com.example.anotheriptv.di.AppContainer
import com.example.anotheriptv.utils.LocaleHelper

class MyApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        val lang = LocaleHelper.getSavedLanguage(this)
        LocaleHelper.setLocale(this, lang)

        container = AppContainer(this)

        // Apply saved theme khi app khởi động
        val prefs = getSharedPreferences(
            com.example.anotheriptv.presentation.settings.SettingsFragment.PREF_NAME,
            android.content.Context.MODE_PRIVATE
        )
        val savedTheme = prefs.getInt(
            com.example.anotheriptv.presentation.settings.SettingsFragment.KEY_THEME,
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(savedTheme)
    }
}