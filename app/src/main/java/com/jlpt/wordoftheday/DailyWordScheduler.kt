package com.jlpt.wordoftheday

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
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
     * (Re)schedules the daily refresh from the time stored in preferences.
     *
     * Use [ExistingPeriodicWorkPolicy.KEEP] on app startup so an existing
     * schedule is left untouched, and [ExistingPeriodicWorkPolicy.UPDATE]
     * after the user changes the time so the new time takes effect.
     */
    fun schedule(
        context: Context,
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
    ) {
        val repository = WordRepository(context)
        val initialDelayMillis = millisUntilNext(
            repository.getDailyRefreshHour(),
            repository.getDailyRefreshMinute()
        )

        val dailyWork = PeriodicWorkRequestBuilder<JlptWordWidgetService>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            policy,
            dailyWork
        )
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
