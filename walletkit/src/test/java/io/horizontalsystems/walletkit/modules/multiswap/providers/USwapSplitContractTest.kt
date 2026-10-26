package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.horizontalsystems.walletkit.modules.multiswap.SplitRateException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Pins the split wire contract. The rate and preview fixtures are captured dev-server answers
 * for SWAPUZ_SPLIT; the commit and track bodies follow the documented shape. The request-body
 * tests guard what the server is strict about: the shares as percents, a destination per
 * output on a preview only, and the split's own handle on the signed and track calls.
 */
class USwapSplitContractTest {

    private val gson = Gson()

    private fun fixture(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()

    @Test
    fun `a split rate prices every output and carries the deposit side at the top level`() {
        val response = gson.fromJson(fixture("split-rate-dev.json"), UnstoppableAPI.Response.SplitRate::class.java)
        val route = response.routes!!.single()

        assertEquals("SWAPUZ_SPLIT", route.providerId)
        assertEquals(BigDecimal("0.05"), route.sellAmount)
        assertEquals("percent", route.amountType)
        assertEquals(listOf(0, 1), route.outputsByIndex.map { it.index })
        assertEquals(BigDecimal("0.025"), route.outputsByIndex[0].sellAmount)
        assertEquals(BigDecimal("52.12") + BigDecimal("173.3907"), route.expectedBuyAmountTotal)
        // Split rates are floating: no output has an enforced floor.
        assertTrue(route.outputsByIndex.all { it.minBuyAmount == null })
        assertEquals(426L, route.estimatedTime?.total)
        assertNull(route.previewToken)
        assertTrue(response.providerErrors!!.isEmpty())
    }

    @Test
    fun `a split preview carries a token, a stand-in and the destinations, ordered by index`() {
        val route = gson.fromJson(fixture("split-preview-dev.json"), UnstoppableAPI.Response.SplitRoute::class.java)

        assertEquals("split-preview-token-fixture", route.previewToken)
        assertEquals("0x9B82eF38C3a7e68e0379D640b9a7A4F3Ecf82617", route.stubDepositAddress)
        // The server may list outputs in any order; the app matches them by index.
        assertEquals(listOf(0, 1), route.outputsByIndex.map { it.index })
        assertEquals("0x3f4E0668C20E100d7C2A27D4b177Ac65B2875D26", route.outputsByIndex[0].destinationAddress)
        assertEquals("TJRabPrwbZy45sbavfcjinPJC18kjpRTv8", route.outputsByIndex[1].destinationAddress)
    }

    @Test
    fun `a split commit names the split, one uuid per output and a single transfer`() {
        val commit = gson.fromJson(
            """
            {
              "status": "ok",
              "splitUuid": "be52c0bd-0000-4000-8000-000000000000",
              "sellAmount": "0.05",
              "execution": {
                "method": "transfer", "chain": "ETH",
                "depositAddress": "0xff02deposit", "amount": "0.05", "asset": "ETH.ETH"
              },
              "expiresAt": 1791461854172,
              "outputs": [
                { "index": 1, "uuid": "fa76d5ea-1", "buyAsset": "ETH.USDT-0X", "destinationAddress": "0x38e5", "sellAmount": "0.025", "expectedBuyAmount": "48.26", "minBuyAmount": null },
                { "index": 0, "uuid": "ba72a16c-0", "buyAsset": "ETH.USDT-0X", "destinationAddress": "0x3f4E", "sellAmount": "0.025", "expectedBuyAmount": "38.45", "minBuyAmount": null }
              ],
              "replayed": false
            }
            """.trimIndent(),
            UnstoppableAPI.Response.SplitCommit::class.java,
        )

        assertEquals("ok", commit.status)
        assertEquals("be52c0bd-0000-4000-8000-000000000000", commit.splitUuid)
        assertEquals("transfer", commit.execution?.method)
        assertEquals("0xff02deposit", commit.execution?.resolvedDepositAddress())
        assertEquals("0.05", commit.execution?.amount)
        assertEquals(listOf("ba72a16c-0", "fa76d5ea-1"), commit.outputsByIndex.map { it.uuid })
        assertFalse(commit.replayed)
    }

    @Test
    fun `a split track tags payout legs with their output index`() {
        val track = gson.fromJson(
            """
            {
              "splitUuid": "be52c0bd", "status": "swapping", "providers": ["SWAPUZ_SPLIT"], "hash": "0x99e7",
              "fromAsset": "ETH.ETH", "fromAmount": "0.039", "fromAddress": "0x3f4E",
              "toAsset": "ETH.USDT-0X", "toAmount": "", "toAddress": "",
              "legs": [
                { "type": "native_send", "status": "completed", "fromAmount": "0.039", "toAddress": "0xff02", "hash": "0x99e7" },
                { "type": "native_send", "status": "completed", "toAmount": "39.00", "toAddress": "0x3f4E", "hash": "0xeea4", "meta": { "outputIndex": 0 } },
                { "type": "native_send", "status": "pending", "toAddress": "0x38e5", "meta": { "outputIndex": 1, "refund": true } }
              ]
            }
            """.trimIndent(),
            UnstoppableAPI.Response.Track::class.java,
        )

        assertEquals("be52c0bd", track.splitUuid)
        val legs = track.legs!!
        assertNull(legs[0].outputIndex)
        assertEquals(0, legs[1].outputIndex)
        assertFalse(legs[1].isRefund)
        assertEquals(1, legs[2].outputIndex)
        assertTrue(legs[2].isRefund)
    }

    @Test
    fun `a split rate request sends percent shares and no destinations`() {
        val request = UnstoppableAPI.Request.SplitRate(
            sellAsset = "ETH.ETH",
            sellAmount = "0.05",
            slippage = BigDecimal("1"),
            outputs = listOf(
                UnstoppableAPI.Request.SplitOutput("ETH.USDT-0X", "60"),
                UnstoppableAPI.Request.SplitOutput("ETH.USDT-0X", "40"),
            ),
            providers = setOf("SWAPUZ_SPLIT"),
        )
        val body = JsonParser.parseString(gson.toJson(request)).asJsonObject

        assertEquals("percent", body["amountType"].asString)
        assertEquals(setOf("sellAsset", "sellAmount", "amountType", "slippage", "outputs", "providers"), body.keySet())
        val output = body["outputs"].asJsonArray[0].asJsonObject
        assertEquals(setOf("buyAsset", "amount"), output.keySet())
        assertEquals("60", output["amount"].asString)
    }

    @Test
    fun `a split preview request names the provider, the refund address and a destination per output`() {
        val request = UnstoppableAPI.Request.SplitPreview(
            sellAsset = "ETH.ETH",
            sellAmount = "0.05",
            slippage = BigDecimal("1"),
            provider = "SWAPUZ_SPLIT",
            refundAddress = "0xrefund",
            sourceAddress = "0xsource",
            networkFee = UnstoppableAPI.Request.NetworkFee(kind = "legacy", gasPrice = "30000000000"),
            outputs = listOf(
                UnstoppableAPI.Request.SplitOutput("ETH.USDT-0X", "50", "0xa"),
                UnstoppableAPI.Request.SplitOutput("ETH.USDT-0X", "50", "0xb"),
            ),
        )
        val body = JsonParser.parseString(gson.toJson(request)).asJsonObject

        assertEquals("SWAPUZ_SPLIT", body["provider"].asString)
        assertEquals("0xrefund", body["refundAddress"].asString)
        assertEquals("legacy", body["networkFee"].asJsonObject["kind"].asString)
        assertEquals("0xb", body["outputs"].asJsonArray[1].asJsonObject["destinationAddress"].asString)
    }

    @Test
    fun `the split signed and track calls are addressed by the split's id`() {
        val signed = JsonParser.parseString(gson.toJson(UnstoppableAPI.Request.SplitSigned("split", "0xhash"))).asJsonObject
        assertEquals(setOf("splitUuid", "inboundTxHash"), signed.keySet())

        val track = JsonParser.parseString(gson.toJson(UnstoppableAPI.Request.SplitTrack("split"))).asJsonObject
        assertEquals(setOf("splitUuid"), track.keySet())
    }

    @Test
    fun `a refused split rate is read from the provider errors in the error body`() {
        val body = """{"providerErrors":[{"provider":"SWAPUZ_SPLIT","error":"Swapuz: deposit is below the minimum 0.01721","errorCode":"amountTooSmall"}]}"""
        val exception = SplitRateException.from(httpException(404, body))

        assertEquals("amountTooSmall", exception.errorCode)
        assertEquals("Swapuz: deposit is below the minimum 0.01721", exception.message)
    }

    @Test
    fun `a rejected split body is read from its validation message`() {
        val body = """{"message":"outputs[].amount must sum to sellAmount (absolute) or to 100 (percent)"}"""
        val exception = SplitRateException.from(httpException(400, body))

        assertNull(exception.errorCode)
        assertEquals("outputs[].amount must sum to sellAmount (absolute) or to 100 (percent)", exception.message)
    }

    @Test
    fun `providers are read with their split capability`() {
        val providers = gson.fromJson(
            """[{"provider":"SWAPUZ","quotes":true,"splitSwaps":false},{"provider":"SWAPUZ_SPLIT","quotes":false,"splitSwaps":true},{"provider":"OLD"}]""",
            Array<UnstoppableAPI.Response.Provider>::class.java,
        )

        assertFalse(providers[0].splitSwaps)
        assertEquals(true, providers[0].quotes)
        assertTrue(providers[1].splitSwaps)
        assertEquals(false, providers[1].quotes)
        // An entry predating the fields reads as a non-split provider that says nothing about quotes.
        assertFalse(providers[2].splitSwaps)
        assertNull(providers[2].quotes)
    }

    private fun httpException(code: Int, body: String): retrofit2.HttpException {
        val response = retrofit2.Response.error<Any>(code, body.toResponseBody("application/json".toMediaType()))
        return retrofit2.HttpException(response)
    }
}
