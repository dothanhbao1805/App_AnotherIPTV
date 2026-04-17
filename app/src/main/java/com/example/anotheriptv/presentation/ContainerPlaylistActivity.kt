// Trong ContainerPlaylistActivity.kt
package com.example.anotheriptv.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.anotheriptv.R
import com.example.anotheriptv.presentation.channels.ChannelFragment
import com.example.anotheriptv.presentation.history.HistoryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class ContainerPlaylistActivity : AppCompatActivity() {

    private var currentPlaylistId: Long = -1L
    private var currentPlaylistName: String = "All Channels"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container_playlist)

        currentPlaylistId = intent.getLongExtra("playlistId", -1L)
        currentPlaylistName = intent.getStringExtra("playlistName") ?: "All Channels"

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            replaceFragment(HistoryFragment())
            bottomNavigationView.selectedItemId = R.id.nav_history
        }

        // 3. Xử lý sự kiện click Menu
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history -> {
                    replaceFragment(HistoryFragment())
                    true
                }
                R.id.nav_all -> {
                    // TẠI ĐÂY: Tạo ChannelFragment và nhét cái ID đang nhớ vào arguments
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

                    false
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