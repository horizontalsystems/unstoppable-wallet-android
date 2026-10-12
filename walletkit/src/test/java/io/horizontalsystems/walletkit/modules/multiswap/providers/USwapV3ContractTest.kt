package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitFailure
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Pins the v3 wire contract on both sides of the swap API client.
 *
 * The preview fixture is a captured dev-server answer for a transfer provider: it carries a
 * token and a stand-in deposit address and no execution or uuid, which is exactly the shape the
 * confirmation screen must price without ever showing a deposit address. The request-body tests
 * guard what the server is strict about: the track body is the uuid alone, and the fee setting
 * goes out only in the server's own shape.
 */
class USwapV3ContractTest {

    private val gson = Gson()

    private val previewTransfer: UnstoppableAPI.Response.Route = gson.fromJson(
        javaClass.classLoader!!.getResource("preview-transfer-dev.json")!!.readText(),
        UnstoppableAPI.Response.Route::class.java,
    )

    @Test
    fun `a transfer preview carries a token and a stand-in but no execution or uuid`() {
        assertEquals("preview-token-fixture", previewTransfer.previewToken)
        assertEquals("bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh", previewTransfer.stubDepositAddress)
        assertNull(previewTransfer.execution)
        assertNull(previewTransfer.uuid)
        assertEquals("QUICKEX", previewTransfer.providerId)
        assertEquals(BigDecimal("0.002"), previewTransfer.sellAmount)
        assertTrue(previewTransfer.expectedBuyAmountOrZero > BigDecimal.ZERO)
        // A floating-rate route: no enforced floor, so the confirm page shows no Guaranteed row.
        assertNull(previewTransfer.minBuyAmount)
    }

    @Test
    fun `merging a commit adds the uuid and the deposit and drops the token and the stand-in`() {
        val commit = gson.fromJson(
            """
            {
              "status": "ok",
              "uuid": "b5b1b8c1-0000-4000-8000-000000000000",
              "sellAmount": "0.002",
              "expectedBuyAmount": "0.307",
              "replayed": false,
              "execution": {
                "method": "transfer",
                "chain": "bitcoin",
                "depositAddress": "bc1qdeposit",
                "amount": "0.002",
                "asset": "BTC.BTC",
                "attachment": { "type": "text", "value": "order-42" }
              }
            }
            """.trimIndent(),
            UnstoppableAPI.Response.Commit::class.java,
        )

        val route = previewTransfer.merging(commit)

        assertEquals("b5b1b8c1-0000-4000-8000-000000000000", route.uuid)
        assertEquals("bc1qdeposit", route.execution?.resolvedDepositAddress())
        assertEquals("order-42", route.execution?.resolvedMemo())
        assertEquals(BigDecimal("0.307"), route.expectedBuyAmount)
        assertNull(route.previewToken)
        assertNull(route.stubDepositAddress)
    }

    @Test
    fun `a commit without an execution keeps the previewed one`() {
        val commit = UnstoppableAPI.Response.Commit(status = "ok", uuid = "uuid")
        val previewed = previewTransfer.copy(
            execution = UnstoppableAPI.Response.Execution(
                method = "signed_transaction", chain = "1", transactions = emptyList(), approval = null,
                depositAddress = null, amount = null, asset = null, attachment = null, unsignedTx = null,
                protocol = null, inboundAddress = null, memo = null, delivery = null,
            )
        )

        assertEquals("signed_transaction", previewed.merging(commit).execution?.method)
    }

    @Test
    fun `a recorded swap is tracked by its uuid alone`() {
        val body = JsonParser.parseString(gson.toJson(UnstoppableAPI.Request.Track(uuid = "uuid"))).asJsonObject

        assertEquals(setOf("uuid"), body.keySet())
    }

    @Test
    fun `the fee setting is sent in the server's shape and only for its own chain`() {
        val legacy = JsonParser.parseString(
            gson.toJson(UnstoppableAPI.Request.NetworkFee(kind = "legacy", gasPrice = "30000000000"))
        ).asJsonObject
        assertEquals(setOf("kind", "gasPrice"), legacy.keySet())

        val utxo = JsonParser.parseString(
            gson.toJson(UnstoppableAPI.Request.NetworkFee(kind = "utxo", feeRate = 12))
        ).asJsonObject
        assertEquals(setOf("kind", "feeRate"), utxo.keySet())
        assertEquals(12, utxo["feeRate"].asInt)
    }

    @Test
    fun `a refused commit is read from its status and body`() {
        val body = """{"status":"rate_changed","uuid":"u","field":"expectedBuyAmount","previewed":"1","committed":"0.9"}"""
        val exception = UnstoppableAPI.CommitException.from(httpException(409, body))

        assertEquals(409, exception.httpStatus)
        assertEquals("rate_changed", exception.status)
        assertEquals(SwapCommitFailure.RateChanged, exception.toSwapCommitFailure())
        assertNull(exception.providerError)
    }

    @Test
    fun `a provider refusal at commit carries its provider error`() {
        val body = """{"status":"provider_error","error":"Amount too small","provider":"QUICKEX","errorCode":"amountOutOfRange","minimumAmount":"0.001"}"""
        val exception = UnstoppableAPI.CommitException.from(httpException(400, body))

        assertEquals("provider_error", exception.status)
        assertEquals("amountOutOfRange", exception.providerError?.errorCode)
        assertEquals(BigDecimal("0.001"), exception.providerError?.minimumAmount)
        assertEquals(SwapCommitFailure.Failed, exception.toSwapCommitFailure())
    }

    private fun httpException(code: Int, body: String): retrofit2.HttpException {
        val response = retrofit2.Response.error<Any>(code, body.toResponseBody("application/json".toMediaType()))
        return retrofit2.HttpException(response)
    }
}
