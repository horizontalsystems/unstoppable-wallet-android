package io.horizontalsystems.walletkit.ui.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SignMessageFormatterTest {

    @Test
    fun typedDataShowsValuesBeforeTypes() {
        val text = """{"types":{"Order":[{"name":"salt","type":"uint256"}]},"domain":{"name":"1inch"},"primaryType":"Order","message":{"salt":"1"}}"""

        val formatted = SignMessageFormatter.format(text)

        val order = listOf("\"primaryType\"", "\"message\"", "\"domain\"", "\"types\"").map { formatted.indexOf(it) }
        assertEquals(order.sorted(), order)
        assertEquals("{\n  \"primaryType\": \"Order\",", formatted.lines().take(2).joinToString("\n"))
    }

    @Test
    fun bigNumbersAndNullsSurvive() {
        val text = """{"value":115792089237316195423570985008687907853269984665640564039457584007913129639935,"receiver":null}"""

        val formatted = SignMessageFormatter.format(text)

        assertEquals(
            "{\n  \"value\": 115792089237316195423570985008687907853269984665640564039457584007913129639935,\n  \"receiver\": null\n}",
            formatted
        )
    }

    @Test
    fun htmlCharactersAreNotEscaped() {
        assertEquals("{\n  \"a\": \"<b> & c=d\"\n}", SignMessageFormatter.format("""{"a":"<b> & c=d"}"""))
    }

    @Test
    fun duplicateMemberNamesAreShownVerbatim() {
        // A parser keeps one "spender" and drops the other; which one is not guaranteed to match
        // the signer's parser, so the text must be shown exactly as it will be signed.
        val text = """{"message":{"spender":"0xGood","spender":"0xEvil"},"types":{}}"""

        assertSame(text, SignMessageFormatter.format(text))
    }

    @Test
    fun nestedDuplicateInsideArrayIsDetected() {
        val text = """{"a":[{"x":1,"x":2}]}"""

        assertSame(text, SignMessageFormatter.format(text))
    }

    @Test
    fun plainTextIsUnchanged() {
        val text = "Sign in to Example\n\nNonce: 12345"

        assertSame(text, SignMessageFormatter.format(text))
    }

    @Test
    fun malformedJsonIsUnchanged() {
        val text = """{"a":1,"""

        assertSame(text, SignMessageFormatter.format(text))
    }
}
