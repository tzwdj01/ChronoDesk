package com.chronodesk.app

import android.app.Application

class ChronoDeskApp : Application() {
    companion object {
        lateinit var instance: ChronoDeskApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
