package io.horizontalsystems.walletkit.modules.multiswap.history

import com.google.gson.Gson
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class SplitSwapTrackerTest {

    private val gson = Gson()

    private val outputs = listOf(
        SwapRecordOutput(index = 0, uuid = "u0", percent = 60, address = "0xaaa", amountOut = "60", status = "Depositing"),
        SwapRecordOutput(index = 1, uuid = "u1", percent = 40, address = "0xaaa", amountOut = "40", status = "Depositing"),
    )

    private fun track(json: String) = gson.fromJson(json, UnstoppableAPI.Response.Track::class.java)

    @Test
    fun `before anything is observed every output is depositing`() {
        val result = SplitSwapTracker.map(track("""{"splitUuid":"s","status":"signed","providers":["SWAPUZ_SPLIT"],"legs":[]}"""), outputs)

        assertEquals(SwapStatus.Depositing, result.status)
        assertEquals(listOf(SwapStatus.Depositing, SwapStatus.Depositing), result.outputs.map { it.swapStatus })
        assertNull(result.amountOut)
    }

    @Test
    fun `payout legs are matched to outputs by index even when the addresses are the same`() {
        val body = """
            {"splitUuid":"s","status":"swapping","toAddress":"0xaaa","toAmount":"",
             "legs":[
               {"type":"native_send","status":"completed","hash":"0xdep","toAddress":"0xff02"},
               {"type":"native_send","status":"pending","toAddress":"0xaaa","hash":"0xp1","meta":{"outputIndex":1}},
               {"type":"native_send","status":"completed","toAmount":"59.5","toAddress":"0xaaa","hash":"0xp0","meta":{"outputIndex":0}}
             ]}
        """
        val result = SplitSwapTracker.map(track(body), outputs)

        assertEquals(SwapStatus.Sending, result.status)
        assertEquals(SwapStatus.Completed, result.outputs[0].swapStatus)
        assertEquals("59.5", result.outputs[0].amountOut)
        assertEquals("0xp0", result.outputs[0].txHash)
        assertEquals(SwapStatus.Sending, result.outputs[1].swapStatus)
        // The committed expectation stays until the payout is observed.
        assertEquals("40", result.outputs[1].amountOut)
        assertEquals("0xp1", result.outputs[1].txHash)
        assertNull(result.amountOut)
    }

    @Test
    fun `an output without a leg on this poll keeps what it last had`() {
        val started = outputs.map { it.copy(status = SwapStatus.Sending.name, txHash = "0xold") }
        val body = """
            {"splitUuid":"s","status":"swapping",
             "legs":[
               {"type":"native_send","status":"completed","toAddress":"0xff02"},
               {"type":"native_send","status":"not_started","meta":{"outputIndex":0}}
             ]}
        """
        val result = SplitSwapTracker.map(track(body), started)

        assertEquals(SwapStatus.Swapping, result.status)
        assertEquals(SwapStatus.Swapping, result.outputs[0].swapStatus)
        assertEquals(SwapStatus.Sending, result.outputs[1].swapStatus)
        assertEquals("0xold", result.outputs[1].txHash)
    }

    @Test
    fun `the deposit leg keeps the split depositing until it confirms`() {
        val body = """
            {"splitUuid":"s","status":"pending",
             "legs":[
               {"type":"native_send","status":"pending","toAddress":"0xff02"},
               {"type":"native_send","status":"not_started","meta":{"outputIndex":0}},
               {"type":"native_send","status":"not_started","meta":{"outputIndex":1}}
             ]}
        """
        val result = SplitSwapTracker.map(track(body), outputs)

        assertEquals(SwapStatus.Depositing, result.status)
        assertEquals(listOf(SwapStatus.Depositing, SwapStatus.Depositing), result.outputs.map { it.swapStatus })
    }

    @Test
    fun `completed sums the payouts into the amount out`() {
        val body = """
            {"splitUuid":"s","status":"completed","toAmount":"99.5",
             "legs":[
               {"type":"native_send","status":"completed","toAddress":"0xff02"},
               {"type":"native_send","status":"completed","toAmount":"59.5","meta":{"outputIndex":0}},
               {"type":"native_send","status":"completed","toAmount":"40","meta":{"outputIndex":1}}
             ]}
        """
        val result = SplitSwapTracker.map(track(body), outputs)

        assertEquals(SwapStatus.Completed, result.status)
        assertEquals(BigDecimal("99.5"), result.amountOut)
        assertEquals(listOf(SwapStatus.Completed, SwapStatus.Completed), result.outputs.map { it.swapStatus })
    }

    @Test
    fun `a held split marks the unpaid outputs and keeps the paid one`() {
        val body = """
            {"splitUuid":"s","status":"action_required","meta":{"pauseReason":"manual_review"},
             "legs":[
               {"type":"native_send","status":"completed","toAddress":"0xff02"},
               {"type":"native_send","status":"completed","toAmount":"59.5","meta":{"outputIndex":0}},
               {"type":"native_send","status":"not_started","meta":{"outputIndex":1}}
             ]}
        """
        val result = SplitSwapTracker.map(track(body), outputs)

        assertEquals(SwapStatus.ActionRequired, result.status)
        assertEquals("manual_review", result.pauseReason)
        assertEquals(SwapStatus.Completed, result.outputs[0].swapStatus)
        assertEquals(SwapStatus.ActionRequired, result.outputs[1].swapStatus)
    }

    @Test
    fun `a refund leg marks its output refunded`() {
        val body = """
            {"splitUuid":"s","status":"failed",
             "legs":[
               {"type":"native_send","status":"completed","toAddress":"0xff02"},
               {"type":"native_send","status":"completed","toAmount":"59.5","meta":{"outputIndex":0}},
               {"type":"native_send","status":"completed","hash":"0xref","meta":{"outputIndex":1,"refund":true}}
             ]}
        """
        val result = SplitSwapTracker.map(track(body), outputs)

        assertEquals(SwapStatus.Failed, result.status)
        assertEquals(SwapStatus.Completed, result.outputs[0].swapStatus)
        assertEquals(SwapStatus.Refunded, result.outputs[1].swapStatus)
        assertEquals("0xref", result.outputs[1].txHash)
    }

    @Test
    fun `an unknown status leaves the record alone`() {
        val result = SplitSwapTracker.map(track("""{"splitUuid":"s","status":"unknown","legs":[]}"""), outputs)

        assertNull(result.status)
        assertEquals(outputs, result.outputs)
    }

    @Test
    fun `outputs round-trip through the record column`() {
        val restored = SwapRecordOutput.fromJson(SwapRecordOutput.toJson(outputs.reversed()))

        assertEquals(outputs, restored)
        assertEquals(emptyList<SwapRecordOutput>(), SwapRecordOutput.fromJson(null))
        assertEquals(emptyList<SwapRecordOutput>(), SwapRecordOutput.fromJson("not json"))
    }
}
