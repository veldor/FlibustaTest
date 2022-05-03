package net.veldor.flibusta_test.model.components

object Translator {
    val latinToCyrillicAlphabet: Map<String, String> by lazy {
        hashMapOf(
            "ey" to "ей",
            "ay" to "ай",
            "yy" to "ый",
            "A" to "А",
            "B" to "Б",
            "V" to "В",
            "G" to "Г",
            "D" to "Д",
            "E" to "Е",
            "Yo" to "Ё",
            "Zh" to "Ж",
            "Z" to "З",
            "I" to "И",
            "J" to "Й",
            "K" to "К",
            "L" to "Л",
            "M" to "М",
            "N" to "Н",
            "O" to "О",
            "P" to "П",
            "R" to "Р",
            "S" to "С",
            "T" to "Т",
            "U" to "У",
            "F" to "Ф",
            "H" to "Х",
            "C" to "Ц",
            "Ch" to "Ч",
            "Sh" to "Ш",
            "Ŝ" to "Щ",
            "Ye" to "Э",
            "Yu" to "Ю",
            "Ya" to "Я",
            "a" to "а",
            "b" to "б",
            "v" to "в",
            "g" to "г",
            "d" to "д",
            "e" to "е",
            "yo" to "ё",
            "zh" to "ж",
            "z" to "з",
            "i" to "и",
            "j" to "й",
            "k" to "к",
            "l" to "л",
            "m" to "м",
            "n" to "н",
            "o" to "о",
            "oy" to "ой",
            "p" to "п",
            "r" to "р",
            "s" to "с",
            "t" to "т",
            "u" to "у",
            "f" to "ф",
            "h" to "х",
            "c" to "ц",
            "ch" to "ч",
            "sh" to "ш",
            "ŝ" to "щ",
            "y" to "ы",
            "ye" to "э",
            "" to "ю",
            "ya" to "я",
            "ʹ" to "ь",
            "ʺ" to "ъ",
        )
    }

    fun translateToRussian(s: String): String{
        val convertedText = StringBuilder()
        var i = 0
        while (i < s.length) {
            val oneLookedUpChar = latinToCyrillicAlphabet[s[i].toString()]
            val twoLookedUpChars = if (i + 1 >= s.length) null else latinToCyrillicAlphabet[s[i].toString() + s[i + 1]]
            when {
                oneLookedUpChar.isNullOrEmpty() && twoLookedUpChars.isNullOrEmpty() -> convertedText.append(s[i]) // don't convert unknown chars
                twoLookedUpChars.isNullOrEmpty() -> convertedText.append(oneLookedUpChar)
                else -> {
                    convertedText.append(twoLookedUpChars)
                    i++
                }
            }
            i++
        }
        return convertedText.toString()
    }
}