package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.Gson
import io.horizontalsystems.marketkit.models.BlockchainType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class USwapZcashAddressPolicyTest {

    private val rules: UnstoppableAPI.Response.ZcashAddressRules = Gson().fromJson(
        """
        {
          "destination": { "unified": { "receivers": ["orchard", "sapling"], "requiresOneOf": ["orchard", "transparent"] }, "transparent": true, "tex": true },
          "refund": { "transparent": true },
          "source": { "transparent": true, "sameAsRefund": true }
        }
        """.trimIndent(),
        UnstoppableAPI.Response.ZcashAddressRules::class.java,
    )

    @Test
    fun `unified present means a unified address built from the rule's receivers`() {
        val kind = USwapZcashAddressPolicy.destinationKind(rules, BlockchainType.Ethereum, "transfer")

        assertEquals(ZcashAddressKind.Unified(listOf("orchard", "sapling"), listOf("orchard", "transparent")), kind)
        assertFalse((kind as ZcashAddressKind.Unified).carriesTransparent)
        assertFalse(USwapZcashAddressPolicy.transparentRecipientOnly(rules, BlockchainType.Ethereum, "transfer"))
    }

    @Test
    fun `a rule without unified, and no rules at all, mean transparent`() {
        assertEquals(ZcashAddressKind.Transparent, USwapZcashAddressPolicy.refundKind(rules))
        assertEquals(ZcashAddressKind.Transparent, USwapZcashAddressPolicy.refundKind(null))
        assertEquals(ZcashAddressKind.Transparent, USwapZcashAddressPolicy.destinationKind(null, BlockchainType.Ethereum, "transfer"))
        assertTrue(USwapZcashAddressPolicy.transparentRecipientOnly(null, BlockchainType.Ethereum, "transfer"))
    }

    @Test
    fun `a vault deposit from an OP_RETURN chain forces a transparent destination`() {
        assertEquals(
            ZcashAddressKind.Transparent,
            USwapZcashAddressPolicy.destinationKind(rules, BlockchainType.Bitcoin, "thorchain_deposit"),
        )
        assertTrue(USwapZcashAddressPolicy.transparentRecipientOnly(rules, BlockchainType.Litecoin, "thorchain_deposit"))
        // the same provider from a chain whose memo is not an OP_RETURN keeps the recommendation
        assertTrue(USwapZcashAddressPolicy.destinationKind(rules, BlockchainType.Ethereum, "thorchain_deposit") is ZcashAddressKind.Unified)
        // and a transfer venue from a UTXO chain is not a vault
        assertTrue(USwapZcashAddressPolicy.destinationKind(rules, BlockchainType.Bitcoin, "transfer") is ZcashAddressKind.Unified)
    }

    @Test
    fun `the source follows its own rule`() {
        assertEquals(ZcashSourcePolicy.SameAsRefund, USwapZcashAddressPolicy.sourcePolicy(rules))
        assertEquals(ZcashSourcePolicy.Omit, USwapZcashAddressPolicy.sourcePolicy(null))

        val own = rules.copy(source = UnstoppableAPI.Response.ZcashAddressRules.Rule(transparent = true))
        assertEquals(ZcashSourcePolicy.Own(ZcashAddressKind.Transparent), USwapZcashAddressPolicy.sourcePolicy(own))
    }

    @Test
    fun `receivers naming transparent build the address with one`() {
        val kind = ZcashAddressKind.Unified(listOf("orchard", "sapling", "Transparent"), emptyList())

        assertTrue(kind.carriesTransparent)
    }
}
