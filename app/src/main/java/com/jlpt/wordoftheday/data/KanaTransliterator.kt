package com.jlpt.wordoftheday.data

object KanaTransliterator {
    private val singleKana = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'を' to "o", 'ん' to "n",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
        'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゔ' to "vu"
    )

    private val compoundKana = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "ぢゃ" to "ja", "ぢゅ" to "ju", "ぢょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
        "うぃ" to "wi", "うぇ" to "we", "うぉ" to "wo",
        "てぃ" to "ti", "でぃ" to "di", "とぅ" to "tu", "どぅ" to "du",
        "ゔぁ" to "va", "ゔぃ" to "vi", "ゔぇ" to "ve", "ゔぉ" to "vo"
    )

    fun toHiragana(value: String): String = buildString {
        value.forEach { char ->
            append(
                when (char) {
                    in 'ァ'..'ヶ' -> (char.code - 0x60).toChar()
                    'ヴ' -> 'ゔ'
                    else -> char
                }
            )
        }
    }

    fun toKatakana(value: String): String = buildString {
        toHiragana(value).forEach { char ->
            append(
                when (char) {
                    in 'ぁ'..'ゖ' -> (char.code + 0x60).toChar()
                    'ゔ' -> 'ヴ'
                    else -> char
                }
            )
        }
    }

    fun toRomaji(value: String): String {
        val kana = toHiragana(value)
        val output = StringBuilder()
        var index = 0
        var geminate = false

        while (index < kana.length) {
            val char = kana[index]
            if (char == 'っ') {
                geminate = true
                index += 1
                continue
            }

            if (char == 'ー') {
                output.lastVowel()?.let(output::append)
                index += 1
                continue
            }

            val compound = if (index + 1 < kana.length) {
                compoundKana[kana.substring(index, index + 2)]
            } else {
                null
            }
            val romanized = compound ?: singleKana[char]

            if (romanized == null) {
                output.append(char)
                index += 1
                geminate = false
                continue
            }

            if (geminate) {
                output.append(geminatePrefix(romanized))
                geminate = false
            }
            output.append(romanized)
            index += if (compound != null) 2 else 1
        }

        return output.toString()
    }

    private fun geminatePrefix(romanized: String): String = when {
        romanized.startsWith("ch") -> "t"
        romanized.startsWith("sh") -> "s"
        romanized.firstOrNull()?.isLetter() == true -> romanized.first().toString()
        else -> ""
    }

    private fun StringBuilder.lastVowel(): Char? =
        lastOrNull { it in setOf('a', 'i', 'u', 'e', 'o') }
}
