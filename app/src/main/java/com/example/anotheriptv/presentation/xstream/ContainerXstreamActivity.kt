package com.example.anotheriptv.presentation.xstream

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.anotheriptv.R
import com.example.anotheriptv.presentation.history.HistoryFragment
import com.example.anotheriptv.presentation.settings.SettingsFragment
import com.example.anotheriptv.presentation.xstream.live.LiveXstreamFragment
import com.example.anotheriptv.presentation.xstream.movie.MovieXstreamFragment
import com.example.anotheriptv.presentation.xstream.series.SeriesXstreamFragment
import com.example.anotheriptv.utils.LocaleHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class ContainerXstreamActivity : AppCompatActivity() {

    private var currentPlaylistId: Long = -1L
    private var currentPlaylistName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container_xstream)

        currentPlaylistId   = intent.getLongExtra("playlistId", -1L)
        currentPlaylistName = intent.getStringExtra("playlistName") ?: ""

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            val historyFragment = HistoryFragment().apply {
                arguments = Bundle().apply {
                    putLong("playlistId", currentPlaylistId)
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
                R.id.nav_live -> {
                    val liveXstreamlFragment = LiveXstreamFragment().apply {
                        arguments = Bundle().apply {
                            putLong("playlistId", currentPlaylistId)
                        }
                    }
                    replaceFragment(liveXstreamlFragment)
                    true
                }

                R.id.nav_movie -> {
                    val movieXstreamlFragment = MovieXstreamFragment().apply {
                        arguments = Bundle().apply {
                            putLong("playlistId", currentPlaylistId)
                        }
                    }
                    replaceFragment(movieXstreamlFragment)
                    true
                }
                R.id.nav_series -> {
                    val seriesXstreamFragment = SeriesXstreamFragment().apply {
                        arguments = Bundle().apply {
                            putLong("playlistId", currentPlaylistId)
                        }
                    }
                    replaceFragment(seriesXstreamFragment)
                    true
                }

                R.id.nav_settings -> {
                    val settingsFragment = SettingsFragment().apply {
                        arguments = Bundle().apply {
                            putLong("playlistId", currentPlaylistId)
                            putString("playlistName", currentPlaylistName)
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

    fun setBottomNavVisible(visible: Boolean) {
        findViewById<BottomNavigationView>(R.id.bottom_navigation).visibility =
            if (visible) View.VISIBLE else View.GONE
    }

}