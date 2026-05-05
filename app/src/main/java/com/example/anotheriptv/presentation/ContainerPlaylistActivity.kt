package com.example.anotheriptv.presentation

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.anotheriptv.R
import com.example.anotheriptv.presentation.channels.ChannelFragment
import com.example.anotheriptv.presentation.history.HistoryFragment
import com.example.anotheriptv.presentation.settings.SettingsFragment
import com.example.anotheriptv.utils.LocaleHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class ContainerPlaylistActivity : AppCompatActivity() {

    private var currentPlaylistId: Long = -1L
    private var currentPlaylistName: String = "All Channels"
    private var currentPlaylistType: String = "M3U"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container_playlist)

        currentPlaylistId = intent.getLongExtra("playlistId", -1L)
        currentPlaylistName = intent.getStringExtra("playlistName") ?: "All Channels"
        currentPlaylistType = intent.getStringExtra("playlistType") ?: "M3U"

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            val historyFragment = HistoryFragment().apply {
                arguments = Bundle().apply {
                    putLong("playlistId", currentPlaylistId)  // ← thêm
                }
            }
            replaceFragment(historyFragment)
            bottomNavigationView.selectedItemId = R.id.nav_history
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history -> {
                    val historyFragment = HistoryFragment().apply {
                        arguments = Bundle().apply {
                            putLong("playlistId", currentPlaylistId)
                        }
                    }
                    replaceFragment(historyFragment)
                    true
                }
                R.id.nav_all -> {
                    val channelFragment = ChannelFragment().apply {
                        arguments = Bundle().apply {
                            putLong("playlistId", currentPlaylistId)
                            putString("playlistName", currentPlaylistName)
                        }
                    }
                    replaceFragment(channelFragment)
                    true
                }

                R.id.nav_settings -> {
                    val settingsFragment = SettingsFragment().apply {
                        arguments = Bundle().apply {
                            putLong("playlistId", currentPlaylistId)
                            putString("playlistName", currentPlaylistName)
                            putString("playlistType", currentPlaylistType)
                        }
                    }
                    replaceFragment(settingsFragment)
                    true
                }

                else -> false
            }
        }

    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

}