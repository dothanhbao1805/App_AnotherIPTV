package com.example.anotheriptv.presentation.xstream

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.anotheriptv.R
import com.example.anotheriptv.presentation.channels.ChannelFragment
import com.example.anotheriptv.presentation.history.HistoryFragment
import com.example.anotheriptv.presentation.xstream.live.LiveXstreamFragment
import com.example.anotheriptv.presentation.xstream.movie.MovieXstreamFragment
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
                    // TODO: mở SeriesFragment
                    true
                }
                R.id.nav_settings -> {
                    // TODO: mở SettingsFragment
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

}