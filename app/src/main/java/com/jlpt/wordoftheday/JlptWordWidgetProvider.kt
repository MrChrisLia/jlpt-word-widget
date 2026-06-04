package com.jlpt.wordoftheday

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.jlpt.wordoftheday.data.Word
import com.jlpt.wordoftheday.data.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JlptWordWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH_WORD) {
            updateAllWidgets(context, forceRefresh = true)
            return
        }

        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, forceRefresh = false)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetId = appWidgetId,
            forceRefresh = false,
            options = newOptions
        )
    }

    override fun onEnabled(context: Context) {
        // First widget added
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        forceRefresh: Boolean,
        options: Bundle? = null
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val layoutMode = WidgetLayoutMode.from(
            options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
        )
        configureClickActions(context, views, appWidgetId)
        configureForSize(context, views, layoutMode)
        val repository = WordRepository(context)

        scope.launch {
            try {
                val word = if (forceRefresh) {
                    repository.refreshRandomWord()
                } else {
                    repository.getCurrentWord() ?: repository.refreshRandomWord()
                }

                if (word != null) {
                    bindWord(context, views, word, layoutMode)
                } else {
                    views.setTextViewText(R.id.widget_kanji, context.getString(R.string.no_word_loaded))
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
                views.setTextViewText(R.id.widget_kanji, "Error loading word")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun updateAllWidgets(context: Context, forceRefresh: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, JlptWordWidgetProvider::class.java)
        )

        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId, forceRefresh)
        }
    }

    private fun configureForSize(
        context: Context,
        views: RemoteViews,
        layoutMode: WidgetLayoutMode
    ) {
        val padding = layoutMode.paddingDp.toPx(context)
        views.setViewPadding(R.id.widget_root, padding, padding, padding, padding)
        views.setTextViewTextSize(R.id.widget_level, TypedValue.COMPLEX_UNIT_SP, layoutMode.badgeSp)
        views.setTextViewTextSize(R.id.widget_word_type, TypedValue.COMPLEX_UNIT_SP, layoutMode.badgeSp)
        views.setTextViewTextSize(R.id.widget_refresh, TypedValue.COMPLEX_UNIT_SP, layoutMode.badgeSp)
        views.setTextViewTextSize(
            R.id.widget_pronunciation,
            TypedValue.COMPLEX_UNIT_SP,
            layoutMode.pronunciationSp
        )
        views.setTextViewTextSize(R.id.widget_kanji, TypedValue.COMPLEX_UNIT_SP, layoutMode.kanjiSp)
        views.setTextViewTextSize(R.id.widget_hiragana, TypedValue.COMPLEX_UNIT_SP, layoutMode.kanaSp)
        views.setTextViewTextSize(R.id.widget_katakana, TypedValue.COMPLEX_UNIT_SP, layoutMode.kanaSp)
        views.setTextViewTextSize(R.id.widget_english, TypedValue.COMPLEX_UNIT_SP, layoutMode.englishSp)
        views.setTextViewTextSize(R.id.widget_example_ja, TypedValue.COMPLEX_UNIT_SP, layoutMode.exampleJaSp)
        views.setTextViewTextSize(R.id.widget_example_en, TypedValue.COMPLEX_UNIT_SP, layoutMode.exampleEnSp)
        views.setInt(R.id.widget_example_ja, "setMaxLines", layoutMode.exampleJaLines)
        views.setInt(R.id.widget_example_en, "setMaxLines", layoutMode.exampleEnLines)

        val compact = layoutMode == WidgetLayoutMode.Compact
        views.setViewVisibility(R.id.widget_refresh, if (compact) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_katakana_row, if (compact) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_divider, if (layoutMode.showExample) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_example_ja, if (layoutMode.showExample) View.VISIBLE else View.GONE)
        views.setViewVisibility(
            R.id.widget_example_en,
            if (layoutMode.showEnglishExample) View.VISIBLE else View.GONE
        )
        views.setTextViewText(
            R.id.widget_hiragana_label,
            context.getString(if (compact) R.string.widget_kana_label else R.string.widget_hiragana_label)
        )
    }

    private fun configureClickActions(context: Context, views: RemoteViews, appWidgetId: Int) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val openAppIntent = Intent(context, MainActivity::class.java)
        val refreshIntent = Intent(context, JlptWordWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WORD
        }

        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(context, appWidgetId, openAppIntent, flags)
        )
        views.setOnClickPendingIntent(
            R.id.widget_refresh,
            PendingIntent.getBroadcast(context, appWidgetId, refreshIntent, flags)
        )
    }

    private fun bindWord(
        context: Context,
        views: RemoteViews,
        word: Word,
        layoutMode: WidgetLayoutMode
    ) {
        views.setTextViewText(R.id.widget_level, word.jlptLevel)
        views.setTextViewText(R.id.widget_word_type, word.wordTypeLabel)
        views.setTextViewText(R.id.widget_pronunciation, word.pronunciation)
        views.setTextViewText(R.id.widget_kanji, word.displayKanji)
        views.setTextViewText(
            R.id.widget_hiragana,
            if (layoutMode == WidgetLayoutMode.Compact) {
                "${word.hiragana} / ${word.katakana}"
            } else {
                word.hiragana
            }
        )
        views.setTextViewText(R.id.widget_katakana, word.katakana)
        // Part of speech is shown as its own badge in the header, so the
        // meaning line is just the meaning at every size.
        views.setTextViewText(R.id.widget_english, word.english)
        views.setTextViewText(R.id.widget_example_ja, word.exampleSentenceJa)
        views.setTextViewText(R.id.widget_example_en, word.exampleSentenceEn)
    }

    private fun Int.toPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    private enum class WidgetLayoutMode(
        val paddingDp: Int,
        val badgeSp: Float,
        val pronunciationSp: Float,
        val kanjiSp: Float,
        val kanaSp: Float,
        val englishSp: Float,
        val exampleJaSp: Float,
        val exampleEnSp: Float,
        val exampleJaLines: Int,
        val exampleEnLines: Int,
        val showExample: Boolean,
        val showEnglishExample: Boolean
    ) {
        Compact(
            paddingDp = 8,
            badgeSp = 9f,
            pronunciationSp = 10f,
            kanjiSp = 21f,
            kanaSp = 10f,
            englishSp = 11f,
            exampleJaSp = 10f,
            exampleEnSp = 9f,
            exampleJaLines = 1,
            exampleEnLines = 1,
            // The example sentence stays at every size. Compact makes room by
            // merging the kana into one row and dropping the EN translation,
            // keeping the JA example on a single ellipsized line at the bottom.
            showExample = true,
            showEnglishExample = false
        ),
        Medium(
            paddingDp = 10,
            badgeSp = 10f,
            pronunciationSp = 11f,
            kanjiSp = 25f,
            kanaSp = 11f,
            englishSp = 12f,
            exampleJaSp = 10f,
            exampleEnSp = 9f,
            exampleJaLines = 1,
            exampleEnLines = 1,
            showExample = true,
            showEnglishExample = false
        ),
        Expanded(
            paddingDp = 12,
            badgeSp = 10f,
            pronunciationSp = 12f,
            kanjiSp = 28f,
            kanaSp = 12f,
            englishSp = 13f,
            exampleJaSp = 11f,
            exampleEnSp = 10f,
            exampleJaLines = 2,
            exampleEnLines = 2,
            showExample = true,
            showEnglishExample = true
        );

        companion object {
            // Vertical content is what overflows on resize, so the mode is
            // driven by available height. Width only pulls a very narrow
            // widget down to Compact (where kana rows are merged). The
            // thresholds keep headroom because the launcher-reported
            // min height includes widget margins and overstates the real
            // drawable area.
            fun from(options: Bundle?): WidgetLayoutMode {
                val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 250
                val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 180

                return when {
                    minWidth < 220 || minHeight < 160 -> Compact
                    minHeight < 215 -> Medium
                    else -> Expanded
                }
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH_WORD = "com.jlpt.wordoftheday.action.REFRESH_WORD"

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, JlptWordWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, JlptWordWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        fun triggerRefresh(context: Context) {
            context.sendBroadcast(
                Intent(context, JlptWordWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WORD
                }
            )
        }
    }
}
