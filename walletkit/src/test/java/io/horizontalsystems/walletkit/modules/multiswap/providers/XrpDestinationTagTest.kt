package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The XRP deposit identifier. Every case here is a way to lose a deposit: a tag dropped, a tag
 * truncated into somebody else's order, or a `text` attachment sent where no provider reads it.
 */
class XrpDestinationTagTest {

    private fun transfer(type: String?, value: String?) = UnstoppableAPI.Response.Execution(
        method = "transfer",
        chain = "ripple",
        transactions = null,
        approval = null,
        depositAddress = "rDeposit",
        amount = "10",
        asset = "XRP.XRP",
        attachment = type?.let { UnstoppableAPI.Response.Attachment(it, value.orEmpty()) },
        unsignedTx = null,
        protocol = null,
        inboundAddress = null,
        memo = null,
        delivery = null,
    )

    // --- parsing ---

    @Test
    fun `a whole number within the 32-bit unsigned range is a tag`() {
        assertEquals(0L, XrpDestinationTag.parse("0"))
        assertEquals(123456L, XrpDestinationTag.parse(" 123456 "))
        assertEquals(XrpDestinationTag.MAX, XrpDestinationTag.parse("4294967295"))
    }

    @Test
    fun `anything the field cannot hold is not a tag`() {
        assertNull(XrpDestinationTag.parse("4294967296"))
        assertNull(XrpDestinationTag.parse("-1"))
        assertNull(XrpDestinationTag.parse("1.5"))
        assertNull(XrpDestinationTag.parse("abc"))
        assertNull(XrpDestinationTag.parse(""))
    }

    // --- resolution off the server's execution ---

    @Test
    fun `a numeric destination_tag resolves`() {
        assertEquals(99L, transfer("destination_tag", "99").resolvedDestinationTag())
    }

    @Test
    fun `no attachment means the provider credits by address alone`() {
        assertNull(transfer(null, null).resolvedDestinationTag())
    }

    @Test
    fun `a text attachment fails the route rather than riding a memo`() {
        assertThrows(IllegalStateException::class.java) {
            transfer("text", "order-42").resolvedDestinationTag()
        }
    }

    @Test
    fun `a tag the field cannot hold fails the route rather than being truncated`() {
        assertThrows(IllegalStateException::class.java) {
            transfer("destination_tag", "4294967296").resolvedDestinationTag()
        }
    }

    @Test
    fun `a tx-only method carries no tag`() {
        val signed = transfer("destination_tag", "99").copy(method = "signed_transaction")
        assertNull(signed.resolvedDestinationTag())
    }

    // --- suspension rules reach XRP ---

    @Test
    fun `native XRP has a canonical id, so a rule can suspend it`() {
        val xrp = Token(
            coin = Coin("ripple", "XRP", "XRP"),
            blockchain = Blockchain(BlockchainType.Xrp, "XRP Ledger", null),
            type = TokenType.Native,
            decimals = 6,
        )
        assertEquals("XRP.XRP", CanonicalAssetId.of(xrp))
    }
}
