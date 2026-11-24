package com.multmaster.app

import android.app.Application
import com.multmaster.app.work.Reminders

class MultiplicationApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.schedule(this)
    }
}