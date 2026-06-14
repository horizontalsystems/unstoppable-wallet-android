package cash.p.terminal.core.adapters

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.managers.SolanaKitWrapper
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.models.FullTokenTransfer
import io.horizontalsystems.solanakit.models.FullTransaction
import io.horizontalsystems.solanakit.models.MintAccount
import io.horizontalsystems.solanakit.models.TokenTransfer
import io.horizontalsystems.solanakit.models.Transaction
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * The Solana transaction converter must flag dust-spam incoming transfers as spam so that
 * Hide Suspicious Transactions can filter them, mirroring the EVM converter behavior.
 */
class SolanaTransactionConverterSpamTest {

    private val userAddress = "USER_ADDRESS"
    private val spammerAddress = "SPAMMER_ADDRESS"

    private val solanaBlockchain = Blockchain(BlockchainType.Solana, "Solana", null)

    private val baseToken = Token(
        coin = Coin(uid = "solana", name = "Solana", code = "SOL"),
        blockchain = solanaBlockchain,
        type = TokenType.Native,
        decimals = 9
    )

    private val source = TransactionSource(
        blockchain = solanaBlockchain,
        account = mockk<Account>(relaxed = true),
        meta = null
    )

    private val coinManager: ICoinManager = mockk(relaxed = true)

    private fun createConverter(): SolanaTransactionConverter {
        every { coinManager.getToken(any()) } returns null
        val solanaKit = mockk<SolanaKit>(relaxed = true)
        every { solanaKit.receiveAddress } returns userAddress
        val solanaKitWrapper = mockk<SolanaKitWrapper>(relaxed = true)
        every { solanaKitWrapper.solanaKit } returns solanaKit

        return SolanaTransactionConverter(
            coinManager = coinManager,
            source = source,
            baseToken = baseToken,
            solanaKitWrapper = solanaKitWrapper
        )
    }

    private fun unknownTokenTransaction(hash: String, incoming: Boolean): FullTransaction {
        val transaction = Transaction(
            hash = hash,
            timestamp = 1_700_000_000L,
            from = if (incoming) spammerAddress else userAddress,
            to = if (incoming) userAddress else spammerAddress,
            amount = null,
            pending = false
        )
        val tokenTransfer = FullTokenTransfer(
            tokenTransfer = TokenTransfer(
                transactionHash = hash,
                mintAddress = "SPAM_MINT",
                incoming = incoming,
                amount = BigDecimal.ONE
            ),
            mintAccount = MintAccount(address = "SPAM_MINT", decimals = 0, isNft = false)
        )
        return FullTransaction(transaction, listOf(tokenTransfer))
    }

    private fun mixedIncomingSolOutgoingUnknownTokenTransaction(hash: String): FullTransaction {
        val oneSolInLamports = BigDecimal("1000000000")
        val transaction = Transaction(
            hash = hash,
            timestamp = 1_700_000_000L,
            from = spammerAddress,
            to = userAddress,
            amount = oneSolInLamports,
            pending = false
        )
        val outgoingUnknownToken = FullTokenTransfer(
            tokenTransfer = TokenTransfer(
                transactionHash = hash,
                mintAddress = "UNKNOWN_MINT",
                incoming = false,
                amount = BigDecimal.ONE
            ),
            mintAccount = MintAccount(address = "UNKNOWN_MINT", decimals = 0, isNft = false)
        )
        return FullTransaction(transaction, listOf(outgoingUnknownToken))
    }

    private fun incomingNativeTransaction(hash: String, lamports: BigDecimal): FullTransaction {
        val transaction = Transaction(
            hash = hash,
            timestamp = 1_700_000_000L,
            from = spammerAddress,
            to = userAddress,
            amount = lamports,
            pending = false
        )
        return FullTransaction(transaction, emptyList())
    }

    @Test
    fun transactionRecord_dustIncomingUnknownToken_isIncomingAndSpam() {
        val record = createConverter().transactionRecord(
            unknownTokenTransaction(hash = "spam_hash", incoming = true)
        )

        assertEquals(TransactionRecordType.SOLANA_INCOMING, record.transactionRecordType)
        assertTrue("Unknown-token dust incoming transfer must be marked as spam", record.spam)
    }

    @Test
    fun transactionRecord_dustOutgoingUnknownToken_isOutgoingAndNotSpam() {
        val record = createConverter().transactionRecord(
            unknownTokenTransaction(hash = "out_hash", incoming = false)
        )

        assertEquals(TransactionRecordType.SOLANA_OUTGOING, record.transactionRecordType)
        assertFalse("Transfers the owner sent must never be marked as spam", record.spam)
    }

    @Test
    fun transactionRecord_dustIncomingNativeSol_isIncomingAndSpam() {
        val oneLamport = BigDecimal.ONE
        val record = createConverter().transactionRecord(
            incomingNativeTransaction(hash = "sol_dust_hash", lamports = oneLamport)
        )

        assertEquals(TransactionRecordType.SOLANA_INCOMING, record.transactionRecordType)
        assertTrue("Dust native SOL below the spam limit must be marked as spam", record.spam)
    }

    @Test
    fun transactionRecord_mixedLegitIncomingSolAndOutgoingUnknownToken_isNotSpam() {
        val record = createConverter().transactionRecord(
            mixedIncomingSolOutgoingUnknownTokenTransaction(hash = "mixed_hash")
        )

        assertEquals(TransactionRecordType.SOLANA_UNKNOWN, record.transactionRecordType)
        assertFalse(
            "An owner-sent (outgoing) unknown token must never make the record spam",
            record.spam
        )
    }

    @Test
    fun transactionRecord_legitIncomingNativeSol_isIncomingAndNotSpam() {
        val oneSolInLamports = BigDecimal("1000000000")
        val record = createConverter().transactionRecord(
            incomingNativeTransaction(hash = "legit_hash", lamports = oneSolInLamports)
        )

        assertEquals(TransactionRecordType.SOLANA_INCOMING, record.transactionRecordType)
        assertFalse("A real, above-limit SOL transfer must not be marked as spam", record.spam)
    }
}
