package com.jlpt.wordoftheday.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kanji: String?,
    val reading: String,
    val english: String,
    val wordType: String,
    val jlptLevel: String,
    val exampleSentenceJa: String,
    val exampleSentenceEn: String
) {
    val displayKanji: String
        get() = kanji?.takeIf { it.isNotBlank() } ?: reading

    val hiragana: String
        get() = KanaTransliterator.toHiragana(reading)

    val katakana: String
        get() = KanaTransliterator.toKatakana(reading)

    val pronunciation: String
        get() = KanaTransliterator.toRomaji(reading)

    val wordTypeLabel: String
        get() = wordType.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }
}
