package com.jlpt.wordoftheday

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jlpt.wordoftheday.data.WordRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules the once-a-day word refresh so it fires at the time of day the
 * user picked, measured against the device's local system clock.
 *
 * WorkManager periodic work cannot target a wall-clock time directly, so we
 * anchor the first run with an initial delay equal to the time remaining until
 * the next occurrence of the chosen hour:minute. After that the work repeats
 * every 24 hours, keeping it aligned with that time of day.
 */
object DailyWordScheduler {

    const val WORK_NAME = "daily_word_refresh"

    /**
     * Ensures a daily refresh is scheduled, without disturbing an existing
     * schedule. Safe to call on every app start.
     */
    fun ensureScheduled(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildRequest(context)
        )
    }

    /**
     * Re-schedules the daily refresh to the time currently stored in
     * preferences, taking effect immediately. Call this after the user changes
     * the time.
     *
     * The existing work is cancelled first on purpose:
     * [ExistingPeriodicWorkPolicy.UPDATE] keeps the previous period boundary
     * and would ignore the new initial delay, so a changed time would never
     * actually take effect. Cancelling forces a fresh schedule anchored to the
     * new time.
     */
    fun reschedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_NAME)
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildRequest(context)
        )
    }

    private fun buildRequest(context: Context): PeriodicWorkRequest {
        val repository = WordRepository(context)
        val initialDelayMillis = millisUntilNext(
            repository.getDailyRefreshHour(),
            repository.getDailyRefreshMinute()
        )
        return PeriodicWorkRequestBuilder<JlptWordWidgetService>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()
    }

    /** Milliseconds from now until the next [hour]:[minute] in local time. */
    private fun millisUntilNext(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_MONTH, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }
}
