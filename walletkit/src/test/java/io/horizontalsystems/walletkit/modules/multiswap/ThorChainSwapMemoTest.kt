package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.marketkit.models.BlockchainType
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
    fun requireDestination_evmAcceptsLowercasedEcho() {
        ThorChainSwapMemo.requireDestination(
            "=:ETH.ETH:0xd8da6bf26964af9d7eed9e03e53415d37aa96045:0/1/0",
            "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045",
            BlockchainType.Ethereum
        )
    }

    @Test
    fun requireDestination_exactMatchOnNonEvm() {
        ThorChainSwapMemo.requireDestination("=:BTC.BTC:$recipient:0/1/0", recipient, BlockchainType.Bitcoin)
    }

    @Test
    fun requireDestination_rejectsCaseAlteredBase58() {
        // Base58 is case-sensitive: a case change is a different (or invalid) address.
        val solana = "4vJ9JU1bJJE96FWSJKvHsmmFADCg4gpZQff4P3bkLKi"
        assertThrows(IllegalStateException::class.java) {
            ThorChainSwapMemo.requireDestination("=:SOL.SOL:${solana.lowercase()}:0/1/0", solana, BlockchainType.Solana)
        }
    }

    @Test
    fun requireDestination_acceptsUppercaseBech32() {
        // BIP-173: the all-uppercase form is the same address.
        ThorChainSwapMemo.requireDestination("=:BTC.BTC:${recipient.uppercase()}:0/1/0", recipient, BlockchainType.Bitcoin)
        ThorChainSwapMemo.requireDestination("=:THOR.RUNE:THOR1GM00VWSFCP48ENM4UV9E5DHM37JTD0YE27WRX0:0/1/0", "thor1gm00vwsfcp48enm4uv9e5dhm37jtd0ye27wrx0", BlockchainType.Thorchain)
    }

    @Test
    fun requireDestination_rejectsMixedCaseBech32() {
        val mixed = recipient.replaceFirst("q", "Q")
        assertThrows(IllegalStateException::class.java) {
            ThorChainSwapMemo.requireDestination("=:BTC.BTC:$mixed:0/1/0", recipient, BlockchainType.Bitcoin)
        }
    }

    @Test
    fun requireDestination_rejectsCaseAlteredLegacyBitcoinAddress() {
        // Base58 legacy addresses are case-sensitive; folding must not apply to them.
        val legacy = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2"
        assertThrows(IllegalStateException::class.java) {
            ThorChainSwapMemo.requireDestination("=:BTC.BTC:${legacy.lowercase()}:0/1/0", legacy, BlockchainType.Bitcoin)
        }
    }

    @Test
    fun requireDestination_rejectsOtherAddress() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainSwapMemo.requireDestination("=:BTC.BTC:bc1qattacker:0/1/0", recipient, BlockchainType.Bitcoin)
        }
    }

    @Test
    fun requireDestination_rejectsMemoWithoutDestination() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainSwapMemo.requireDestination("=:BTC.BTC", recipient, BlockchainType.Bitcoin)
        }
    }
}
