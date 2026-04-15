package com.example.anotheriptv.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.anotheriptv.R
import com.example.anotheriptv.presentation.channels.ChannelFragment
import com.example.anotheriptv.presentation.history.HistoryFragment

class ContainerPlaylistActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container_playlist)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)


        if (savedInstanceState == null) {
            replaceFragment(HistoryFragment())
            bottomNavigationView.selectedItemId = R.id.nav_history
        }

        // Xử lý sự kiện khi chọn các item trên menu
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history -> {
                    replaceFragment(HistoryFragment())
                    true
                }
                R.id.nav_all -> {
                    replaceFragment(ChannelFragment())
                    true
                }
                R.id.nav_settings -> {

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