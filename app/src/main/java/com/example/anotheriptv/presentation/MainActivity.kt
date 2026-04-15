package com.example.anotheriptv.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.anotheriptv.R
import com.example.anotheriptv.presentation.channels.ChannelFragment
import com.example.anotheriptv.presentation.playlist.PlaylistFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        android.util.Log.d("MainActivity", "onCreate called")

        if (savedInstanceState == null) {
            android.util.Log.d("MainActivity", "Loading PlaylistFragment")
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PlaylistFragment())
                .commit()
        }
    }
}