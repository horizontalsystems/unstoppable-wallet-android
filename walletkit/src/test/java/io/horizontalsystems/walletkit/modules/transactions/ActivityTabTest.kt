package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import io.horizontalsystems.walletkit.modules.contacts.model.ContactAddress
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapOperation
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTabTest {

    private val confidential = setOf("u_NEAR_CONFIDENTIAL")

    @Test
    fun `transfers shows no swap records`() {
        SwapRecordKind.entries.forEach {
            assertFalse(it.name, ActivityTab.Transfers.shows(it))
        }
    }

    @Test
    fun `swaps shows swaps and split swaps only`() {
        assertTrue(ActivityTab.Swaps.shows(SwapRecordKind.Swap))
        assertTrue(ActivityTab.Swaps.shows(SwapRecordKind.PrivateSwap))
        assertFalse(ActivityTab.Swaps.shows(SwapRecordKind.PrivateSend))
        assertFalse(ActivityTab.Swaps.shows(SwapRecordKind.CrossPay))
    }

    @Test
    fun `privates shows private sends only`() {
        assertFalse(ActivityTab.Privates.shows(SwapRecordKind.Swap))
        assertFalse(ActivityTab.Privates.shows(SwapRecordKind.PrivateSwap))
        assertTrue(ActivityTab.Privates.shows(SwapRecordKind.PrivateSend))
        assertFalse(ActivityTab.Privates.shows(SwapRecordKind.CrossPay))
    }

    @Test
    fun `cross pays shows cross pays only`() {
        assertFalse(ActivityTab.CrossPays.shows(SwapRecordKind.Swap))
        assertFalse(ActivityTab.CrossPays.shows(SwapRecordKind.PrivateSwap))
        assertFalse(ActivityTab.CrossPays.shows(SwapRecordKind.PrivateSend))
        assertTrue(ActivityTab.CrossPays.shows(SwapRecordKind.CrossPay))
    }

    @Test
    fun `a swap is private when split or routed through a confidential provider`() {
        assertEquals(SwapRecordKind.Swap, SwapRecordKind.of(SwapOperation.Swap, "u_THORCHAIN", confidential))
        assertEquals(SwapRecordKind.PrivateSwap, SwapRecordKind.of(SwapOperation.Swap, "u_NEAR_CONFIDENTIAL", confidential))
        assertEquals(SwapRecordKind.PrivateSwap, SwapRecordKind.of(SwapOperation.SplitSwap, "u_SWAPUZ", confidential))
    }

    @Test
    fun `private sends and cross pays keep their kind whatever the provider`() {
        assertEquals(SwapRecordKind.PrivateSend, SwapRecordKind.of(SwapOperation.PrivateSend, "u_NEAR_CONFIDENTIAL", confidential))
        assertEquals(SwapRecordKind.CrossPay, SwapRecordKind.of(SwapOperation.CrossPay, "u_NEAR_CONFIDENTIAL", confidential))
        assertEquals(SwapRecordKind.CrossPay, SwapRecordKind.of(SwapOperation.CrossPay, "u_THORCHAIN", confidential))
    }

    @Test
    fun `a record in progress shows the pending state`() {
        assertEquals(TransactionIconState.Pending, swapRecordIconState(SwapStatus.Depositing))
        assertEquals(TransactionIconState.Pending, swapRecordIconState(SwapStatus.Swapping))
        assertEquals(TransactionIconState.Pending, swapRecordIconState(SwapStatus.Sending))
    }

    @Test
    fun `a completed record shows the completed state`() {
        assertEquals(TransactionIconState.Completed, swapRecordIconState(SwapStatus.Completed))
    }

    @Test
    fun `a failed record shows the failed state`() {
        assertEquals(TransactionIconState.Failed, swapRecordIconState(SwapStatus.Failed))
    }

    @Test
    fun `refunded and expired records are muted`() {
        assertEquals(TransactionIconState.Muted, swapRecordIconState(SwapStatus.Refunded))
        assertEquals(TransactionIconState.Muted, swapRecordIconState(SwapStatus.Expired))
    }

    @Test
    fun `a record needing action shows a warning`() {
        assertEquals(TransactionIconState.Warning, swapRecordIconState(SwapStatus.ActionRequired))
    }

    @Test
    fun `a coin filter matches a record with the coin on either side`() {
        assertTrue(btcToSol.matchesActivityFilter("bitcoin|native", null, null))
        assertTrue(btcToSol.matchesActivityFilter("solana|native", null, null))
        assertFalse(btcToSol.matchesActivityFilter("ethereum|native", null, null))
    }

    @Test
    fun `a blockchain filter matches a record with the blockchain on either side`() {
        assertTrue(btcToSol.matchesActivityFilter(null, "bitcoin", null))
        assertTrue(btcToSol.matchesActivityFilter(null, "solana", null))
        assertFalse(btcToSol.matchesActivityFilter(null, "ethereum", null))
    }

    @Test
    fun `a contact filter matches the recipient or the source address, ignoring case`() {
        assertTrue(btcToSol.matchesActivityFilter(null, null, contact(solana to "solrecipient111")))
        assertTrue(btcToSol.matchesActivityFilter(null, null, contact(bitcoin to "BC1SOURCE")))
        assertFalse(btcToSol.matchesActivityFilter(null, null, contact(solana to "someoneElse")))
    }

    @Test
    fun `with a blockchain chosen only the contact's addresses on it count`() {
        val sourceOnBitcoin = contact(bitcoin to "bc1source")

        assertTrue(btcToSol.matchesActivityFilter(null, "bitcoin", sourceOnBitcoin))
        assertFalse(btcToSol.matchesActivityFilter(null, "solana", sourceOnBitcoin))
    }

    private val bitcoin = Blockchain(BlockchainType.Bitcoin, "Bitcoin", null)
    private val solana = Blockchain(BlockchainType.Solana, "Solana", null)

    private fun contact(vararg addresses: Pair<Blockchain, String>) =
        Contact("uid", "Contact", addresses.map { (blockchain, address) -> ContactAddress(blockchain, address) })

    private val btcToSol = SwapRecord(
        accountId = "account-1",
        timestamp = 0,
        providerId = "u_NEAR",
        providerName = "NEAR Intents",
        tokenInUid = "bitcoin|native",
        tokenInCoinCode = "BTC",
        tokenInCoinUid = "bitcoin",
        tokenInBadge = null,
        tokenInBlockchainTypeUid = "bitcoin",
        tokenOutUid = "solana|native",
        tokenOutCoinCode = "SOL",
        tokenOutCoinUid = "solana",
        tokenOutBadge = null,
        tokenOutBlockchainTypeUid = "solana",
        amountIn = "0.01",
        amountOut = "1.5",
        amountOutMin = null,
        recipientAddress = "SoLRecipient111",
        sourceAddress = "bc1source",
        transactionHash = null,
        providerSwapId = null,
        fromAsset = null,
        toAsset = null,
        depositAddress = null,
        status = "Completed",
    )
}
