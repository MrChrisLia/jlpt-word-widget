package com.jlpt.wordoftheday

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jlpt.wordoftheday.data.WordRepository

class JlptWordWidgetService(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Pick the new word inside the worker's own coroutine so it is
            // persisted before we finish, then ask the widgets to re-render
            // from the stored word. Doing the refresh here (rather than firing
            // a broadcast and returning immediately) keeps the work alive until
            // the new word is actually committed.
            WordRepository(applicationContext).refreshRandomWord()
            JlptWordWidgetProvider.triggerUpdate(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
