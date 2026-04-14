package com.example.anotheriptv


import android.app.Application
import com.example.anotheriptv.di.AppContainer

class MyApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}