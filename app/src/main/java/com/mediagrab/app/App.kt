package com.mediagrab.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mediagrab.app.data.Engine
import com.mediagrab.app.data.HistoryStore
import com.mediagrab.app.data.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        HistoryStore.init(this)
        createChannels()
        Engine.initAsync(this)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "Progres unduhan", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Unduhan selesai", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    companion object {
        const val CH_PROGRESS = "progress"
        const val CH_DONE = "done"
    }
}
