package io.horizontalsystems.bankwallet.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsKtTest {

    @Test
    fun shorten() {
        assertShorten("0x1234567890abcdef", "0x1234...cdef")
        assertShorten("bc1234567890abcdef", "bc1234...cdef")
        assertShorten("bnb1234567890abcdef", "bnb1234...cdef")
        assertShorten("ltc1234567890abcdef", "ltc1234...cdef")
        assertShorten("bitcoincash:1234567890abcdef", "bitcoincash:1234...cdef")
        assertShorten("1234567890abcdef", "1234...cdef")
    }

    private fun assertShorten(raw: String, expected: String) {
        assertEquals(expected, raw.shorten())
    }
}
