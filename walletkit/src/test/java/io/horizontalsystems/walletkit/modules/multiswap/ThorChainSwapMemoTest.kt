package io.horizontalsystems.walletkit.modules.multiswap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ThorChainSwapMemoTest {

    private val recipient = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"

    @Test
    fun destination_isThirdField() {
        assertEquals(recipient, ThorChainSwapMemo.destination("=:BTC.BTC:$recipient:1234/1/0:t:10"))
    }

    @Test
    fun destination_ignoresTrailingRefundAddress() {
        assertEquals(recipient, ThorChainSwapMemo.destination("=:BTC.BTC:$recipient/thor1refund:0/1/0"))
    }

    @Test
    fun destination_missing() {
        assertNull(ThorChainSwapMemo.destination("=:BTC.BTC"))
        assertNull(ThorChainSwapMemo.destination("=:BTC.BTC::0/1/0"))
    }

    @Test
    fun requireDestination_acceptsMatchIgnoringCase() {
        ThorChainSwapMemo.requireDestination(
            "=:ETH.ETH:0xd8da6bf26964af9d7eed9e03e53415d37aa96045:0/1/0",
            "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"
        )
    }

    @Test
    fun requireDestination_rejectsOtherAddress() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainSwapMemo.requireDestination("=:BTC.BTC:bc1qattacker:0/1/0", recipient)
        }
    }

    @Test
    fun requireDestination_rejectsMemoWithoutDestination() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainSwapMemo.requireDestination("=:BTC.BTC", recipient)
        }
    }
}
