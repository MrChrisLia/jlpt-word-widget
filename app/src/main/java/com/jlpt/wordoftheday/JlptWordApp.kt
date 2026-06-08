package com.jlpt.wordoftheday

import android.app.Application
import androidx.work.Configuration

class JlptWordApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // Keep any existing schedule; only seed the default if none exists yet.
        DailyWordScheduler.ensureScheduled(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
