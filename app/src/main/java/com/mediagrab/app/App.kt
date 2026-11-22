package com.mediagrab.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mediagrab.app.data.Engine
import com.mediagrab.app.data.HistoryStore
import com.mediagrab.app.data.Prefs

class App : Application() {
    companion object {
        const val CH_WORK = "mediagrab_work"
        const val CH_DONE = "mediagrab_done"
    }

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        HistoryStore.init(this)
        Engine.initAsync(this)

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_WORK, "Proses Unduhan", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Unduhan Selesai", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}

