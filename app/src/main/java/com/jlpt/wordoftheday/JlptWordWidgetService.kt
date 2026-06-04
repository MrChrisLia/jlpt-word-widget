package com.jlpt.wordoftheday

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class JlptWordWidgetService(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            JlptWordWidgetProvider.triggerRefresh(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
