package com.jlpt.wordoftheday.data

import android.content.Context

class WordRepository(context: Context) {
    private val wordDao: WordDao = WordDatabase.getDatabase(context).wordDao()
    private val prefs = context.applicationContext.getSharedPreferences(
        "jlpt_word_pref",
        Context.MODE_PRIVATE
    )

    suspend fun getRandomWord(levels: List<String>): Word? {
        ensureSeeded()
        return wordDao.getRandomWord(levels.ifEmpty { DefaultLevels })
    }

    suspend fun getRandomWordByLevel(level: String): Word? {
        ensureSeeded()
        return wordDao.getRandomWordByLevel(level)
    }

    fun getAllWords() = wordDao.getAllWords()

    fun setEnabledLevels(levels: Set<String>) {
        val normalized = levels
            .filter { it in DefaultLevels }
            .toSet()
            .ifEmpty { DefaultLevels.toSet() }

        prefs.edit().putStringSet("enabled_levels", normalized).apply()
    }

    fun getEnabledLevels(): Set<String> {
        val stored = prefs.getStringSet("enabled_levels", DefaultLevels.toSet()).orEmpty()
        val normalized = stored.filter { it in DefaultLevels }.toSet()
        return normalized.ifEmpty { DefaultLevels.toSet() }
    }

    /** Stores the time of day (device local time) a new word should appear. */
    fun setDailyRefreshTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt("refresh_hour", hour.coerceIn(0, 23))
            .putInt("refresh_minute", minute.coerceIn(0, 59))
            .apply()
    }

    fun getDailyRefreshHour(): Int =
        prefs.getInt("refresh_hour", DEFAULT_REFRESH_HOUR).coerceIn(0, 23)

    fun getDailyRefreshMinute(): Int =
        prefs.getInt("refresh_minute", DEFAULT_REFRESH_MINUTE).coerceIn(0, 59)

    fun setCurrentWord(word: Word) {
        prefs.edit()
            .putLong("current_word_id", word.id)
            .putString("current_kanji", word.kanji)
            .putString("current_reading", word.reading)
            .putString("current_english", word.english)
            .putString("current_word_type", word.wordType)
            .putString("current_level", word.jlptLevel)
            .putString("current_example_ja", word.exampleSentenceJa)
            .putString("current_example_en", word.exampleSentenceEn)
            .apply()
    }

    fun getCurrentWord(): Word? {
        val id = prefs.getLong("current_word_id", -1L)
        if (id == -1L) return null
        return Word(
            id = id,
            kanji = prefs.getString("current_kanji", null),
            reading = prefs.getString("current_reading", "") ?: "",
            english = prefs.getString("current_english", "") ?: "",
            wordType = prefs.getString("current_word_type", "") ?: "",
            jlptLevel = prefs.getString("current_level", "N5") ?: "N5",
            exampleSentenceJa = prefs.getString("current_example_ja", "") ?: "",
            exampleSentenceEn = prefs.getString("current_example_en", "") ?: ""
        )
    }

    suspend fun ensureSeeded() {
        if (wordDao.getCount() == 0) {
            wordDao.insertAll(JlptVocabularyData.getAllWords())
        }
    }

    suspend fun isDatabaseSeeded(): Boolean {
        return wordDao.getCount() > 0
    }

    suspend fun refreshRandomWord(): Word? {
        val levels = getEnabledLevels().toList()
        val word = getRandomWord(levels)
        word?.let { setCurrentWord(it) }
        return word
    }

    companion object {
        val DefaultLevels = listOf("N5", "N4", "N3", "N2", "N1")
        const val DEFAULT_REFRESH_HOUR = 8
        const val DEFAULT_REFRESH_MINUTE = 0
    }
}
