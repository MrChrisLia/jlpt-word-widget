package com.jlpt.wordoftheday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words WHERE jlptLevel IN (:levels) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWord(levels: List<String>): Word?

    @Query("SELECT * FROM words WHERE jlptLevel = :level ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWordByLevel(level: String): Word?

    @Query("SELECT * FROM words WHERE jlptLevel = :level ORDER BY id ASC")
    fun getAllWordsByLevel(level: String): Flow<List<Word>>

    @Query("SELECT * FROM words ORDER BY id ASC")
    fun getAllWords(): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<Word>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: Word)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM words WHERE jlptLevel = :level")
    suspend fun getCountByLevel(level: String): Int
}
