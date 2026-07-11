package io.lanzof.core.search

import java.text.Normalizer
import java.util.Locale

object LocationSearchNormalizer {
    private val combiningMarksRegex = Regex("\\p{M}+")
    private val nonSearchCharacterRegex = Regex("[^\\p{IsAlphabetic}\\p{IsDigit}]+")
    private val whitespaceRegex = Regex("\\s+")

    fun normalize(value: String): String {
        val withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(combiningMarksRegex, "")

        return withoutDiacritics
            .lowercase(Locale.ROOT)
            .replace(nonSearchCharacterRegex, " ")
            .trim()
            .replace(whitespaceRegex, " ")
    }
}
