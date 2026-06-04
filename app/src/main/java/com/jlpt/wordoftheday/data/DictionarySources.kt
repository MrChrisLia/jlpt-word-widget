package com.jlpt.wordoftheday.data

data class DictionarySource(
    val name: String,
    val use: String,
    val license: String,
    val url: String
)

object DictionarySources {
    val all = listOf(
        DictionarySource(
            name = "JMdict/EDICT",
            use = "Headwords, readings, glosses, and part-of-speech metadata",
            license = "CC BY-SA 4.0",
            url = "https://www.edrdg.org/wiki/index.php/JMdict-EDICT_Dictionary_Project"
        ),
        DictionarySource(
            name = "Tanaka Corpus",
            use = "Japanese-English example sentence corpus",
            license = "Public domain",
            url = "https://www.edrdg.org/wiki/Tanaka_Corpus.html"
        ),
        DictionarySource(
            name = "Tatoeba",
            use = "Freely reusable Japanese-English sentence pairs",
            license = "CC BY 2.0 FR / CC0 per sentence",
            url = "https://tatoeba.org/en/downloads"
        )
    )

    const val acknowledgement =
        "This app uses a compact bundled JLPT vocabulary seed sourced from freely reusable Japanese dictionary and example-sentence projects. JMdict/EDICT data is copyright Electronic Dictionary Research and Development Group and used under CC BY-SA 4.0."
}
