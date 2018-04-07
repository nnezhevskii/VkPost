package com.licht.vkpost.vkpost

import android.app.Application

class App : Application() {
    init {
        application = this
    }

    companion object {
        public lateinit var application: Application
    }
}