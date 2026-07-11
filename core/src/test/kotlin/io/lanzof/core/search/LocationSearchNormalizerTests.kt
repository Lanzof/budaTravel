package io.lanzof.core.search

import kotlin.test.Test
import kotlin.test.assertEquals

class LocationSearchNormalizerTests {
    @Test
    fun `normalize should remove diacritics lowercase and collapse separators`() {
        assertEquals(
            "deak ferenc ter m",
            LocationSearchNormalizer.normalize("Deák Ferenc tér M"),
        )
        assertEquals(
            "kobanya also vasutallomas",
            LocationSearchNormalizer.normalize("Kőbánya alsó vasútállomás"),
        )
        assertEquals(
            "puskas ferenc stadion m",
            LocationSearchNormalizer.normalize("  Puskás   Ferenc Stadion M  "),
        )
    }
}
