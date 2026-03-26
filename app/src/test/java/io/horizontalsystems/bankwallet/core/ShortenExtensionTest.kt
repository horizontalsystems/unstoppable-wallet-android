package io.horizontalsystems.bankwallet.core

import cash.p.terminal.strings.helpers.shorten
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortenExtensionTest {

    @Test
    fun shorten_evmAddress40chars_correctFormat() {
        val address = "0xABCDEF1234567890abcdef1234567890ABCDEF12"
        assertEquals("0xABCDEF1234...cdef...90ABCDEF12", address.shorten())
    }

    @Test
    fun shorten_realEvmAddress_correctFormat() {
        val address = "0xb74B5f93093f06e4c9dA1231B3b968cc5ABCDEFa"
        assertEquals("0xb74B5f9309...dA12...cc5ABCDEFa", address.shorten())
    }

    @Test
    fun shorten_bitcoinAddress_correctFormat() {
        val address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"
        val withoutPrefix = address.removePrefix("bc")
        val expected = "bc" + withoutPrefix.take(10) + "..." +
            withoutPrefix.substring(withoutPrefix.length / 2 - 2, withoutPrefix.length / 2 + 2) + "..." +
            withoutPrefix.takeLast(10)
        assertEquals(expected, address.shorten())
    }

    @Test
    fun shorten_bnbAddress_correctFormat() {
        val address = "bnb1grpf0955h0ykzq3ar5nmum7y6gdfl6lxfn46h2"
        val withoutPrefix = address.removePrefix("bnb")
        val expected = "bnb" + withoutPrefix.take(10) + "..." +
            withoutPrefix.substring(withoutPrefix.length / 2 - 2, withoutPrefix.length / 2 + 2) + "..." +
            withoutPrefix.takeLast(10)
        assertEquals(expected, address.shorten())
    }

    @Test
    fun shorten_noPrefixLongAddress_correctFormat() {
        val address = "ABCDEF1234567890abcdef1234567890ABCDEF1234"
        assertEquals("ABCDEF1234...def1...ABCDEF1234", address.shorten())
    }

    @Test
    fun shorten_exactly24charsNoPrefix_shortFormat() {
        val address = "ABCDEFGHIJKLMNOPQRSTUVWX"
        assertEquals("ABCD...UVWX", address.shorten())
    }

    @Test
    fun shorten_exactly24charsWithPrefix_shortFormat() {
        val address = "0x" + "A".repeat(24)
        assertEquals("0xAAAA...AAAA", address.shorten())
    }

    @Test
    fun shorten_25charsNoPrefix_shortens() {
        val address = "ABCDEFGHIJKLMNOPQRSTUVWXY"
        assertEquals("ABCDEFGHIJ...KLMN...PQRSTUVWXY", address.shorten())
    }

    @Test
    fun shorten_25charsWithPrefix_shortens() {
        val address = "0xABCDEFGHIJKLMNOPQRSTUVWXY"
        assertEquals("0xABCDEFGHIJ...KLMN...PQRSTUVWXY", address.shorten())
    }

    @Test
    fun shorten_20charsBody_shortFormat() {
        val address = "0x" + "A".repeat(20)
        assertEquals("0xAAAA...AAAA", address.shorten())
    }

    @Test
    fun shorten_12charsBody_shortFormat() {
        val address = "0x123456789ABC"
        assertEquals("0x1234...9ABC", address.shorten())
    }

    @Test
    fun shorten_11charsBody_shortFormat() {
        val address = "0x12345678ABC"
        assertEquals("0x1234...8ABC", address.shorten())
    }

    @Test
    fun shorten_3chars_returnsUnchanged() {
        assertEquals("abc", "abc".shorten())
    }

    @Test
    fun shorten_1char_returnsUnchanged() {
        assertEquals("a", "a".shorten())
    }

    @Test
    fun shorten_emptyString_returnsEmpty() {
        assertEquals("", "".shorten())
    }

    @Test
    fun shorten_prefixOnly_returnsUnchanged() {
        assertEquals("0x", "0x".shorten())
    }

    @Test
    fun shorten_prefixPlusFewChars_returnsUnchanged() {
        assertEquals("0xABCD", "0xABCD".shorten())
    }

    @Test
    fun shorten_ltcAddress_preservesPrefix() {
        val address = "ltc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"
        val result = address.shorten()
        assert(result.startsWith("ltc"))
        assert(result.contains("..."))
    }

    @Test
    fun shorten_bitcoinCashAddress_preservesLongPrefix() {
        val address = "bitcoincash:qpm2qsznhks23z7629mms6s4cwef74vcwvy22gdx6a"
        val result = address.shorten()
        assert(result.startsWith("bitcoincash:"))
        assert(result.contains("..."))
    }

    @Test
    fun shorten_ecashAddress_preservesPrefix() {
        val address = "ecash:qpm2qsznhks23z7629mms6s4cwef74vcwvy22gdx6a"
        val result = address.shorten()
        assert(result.startsWith("ecash:"))
        assert(result.contains("..."))
    }
}
