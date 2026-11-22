package com.mediagrab.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mediagrab.app.data.Engine
import com.mediagrab.app.data.HistoryStore
import com.mediagrab.app.data.Prefs

class App : Application() {
    companion object {
        const val CH_PROGRESS = "mediagrab_progress"
        const val CH_DONE = "mediagrab_done"
    }

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        HistoryStore.init(this)
        Engine.initAsync(this)

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "Proses Unduhan", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Unduhan Selesai", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}

