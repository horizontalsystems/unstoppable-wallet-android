package io.horizontalsystems.walletkit.modules.walletconnect.request

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WCEvmRequestParamsTest {

    private val address = "0x1111111111111111111111111111111111111111"
    private val typedData = """{"domain":{"name":"1inch Aggregation Router","chainId":56},"primaryType":"Order","message":{"makingAmount":"1000000000000000000"}}"""

    @Test
    fun typedDataSentAsObject() {
        val params = """["$address", $typedData]"""

        assertEquals(JsonParser.parseString(typedData), JsonParser.parseString(WCEvmRequestParams.jsonObjectParam(params)))
    }

    @Test
    fun typedDataSentAsJsonEncodedString() {
        val encoded = typedData.replace("\"", "\\\"")
        val params = """["$address", "$encoded"]"""

        assertEquals(JsonParser.parseString(typedData), JsonParser.parseString(WCEvmRequestParams.jsonObjectParam(params)))
    }

    @Test
    fun objectWinsOverStringWhenBothPresent() {
        val params = """["{\"a\":1}", {"b":2}]"""

        assertEquals("""{"b":2}""", WCEvmRequestParams.jsonObjectParam(params))
    }

    @Test
    fun addressAloneIsInvalid() {
        assertThrows(Exception::class.java) { WCEvmRequestParams.jsonObjectParam("""["$address"]""") }
    }

    @Test
    fun bigNumbersSurviveUntouched() {
        val params = """[{"value":115792089237316195423570985008687907853269984665640564039457584007913129639935}]"""

        assertEquals(
            """{"value":115792089237316195423570985008687907853269984665640564039457584007913129639935}""",
            WCEvmRequestParams.jsonObjectParam(params)
        )
    }

    @Test
    fun personalSignHexIsDecoded() {
        assertEquals("hello", WCEvmRequestParams.personalSignMessage("""["0x68656c6c6f", "$address"]"""))
    }

    @Test
    fun personalSignPlainTextIsKept() {
        assertEquals("hello", WCEvmRequestParams.personalSignMessage("""["hello", "$address"]"""))
    }
}
