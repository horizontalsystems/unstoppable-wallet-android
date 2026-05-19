package cash.p.terminal.core.managers

import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingTransactionMatcherTest {

    private val matcher = PendingTransactionMatcher()
    private val token = litecoinToken()
    private val source = transactionSource(token)

    @Test
    fun matchScoreForRealRecord_litecoinMwebPegInWithoutRecipientsOrChange_doesNotMatch() {
        val pending = pendingMwebPegIn(amount = BigDecimal("0.01"))
        val real = publicLitecoinRecord(
            amount = BigDecimal("-0.01"),
            to = emptyList(),
            changeAddresses = emptyList()
        )

        val score = matcher.matchScoreForRealRecord(pending, real)

        assertFalse(score.isMatch)
    }

    @Test
    fun matchScoreForRealRecord_litecoinMwebPegInWithLargePublicAmount_doesNotMatch() {
        val pending = pendingMwebPegIn(amount = BigDecimal("0.01"))
        val changeAddress = "ltc1qchangeaddress"
        val real = publicLitecoinRecord(
            amount = BigDecimal("-0.50"),
            to = listOf(changeAddress),
            changeAddresses = listOf(changeAddress)
        )

        val score = matcher.matchScoreForRealRecord(pending, real)

        assertFalse(score.isMatch)
    }

    @Test
    fun matchScoreForRealRecord_litecoinMwebPegInWithChangeOnlyPublicOutput_matchesIgnoringCase() {
        val pending = pendingMwebPegIn(amount = BigDecimal("0.01"))
        val real = publicLitecoinRecord(
            amount = BigDecimal("-0.015"),
            to = listOf("LTC1QCHANGEADDRESS"),
            changeAddresses = listOf("ltc1qchangeaddress")
        )

        val score = matcher.matchScoreForRealRecord(pending, real)

        assertTrue(score.isMatch)
        assertEquals(PendingTransactionMatchKind.LitecoinMwebPegIn, score.kind)
    }

    @Test
    fun matchScoreForRealRecord_litecoinMwebPegInWithNullRecipientsAndChange_matches() {
        val pending = pendingMwebPegIn(amount = BigDecimal("0.01"))
        val real = publicLitecoinRecord(
            amount = BigDecimal("-0.015"),
            to = null,
            changeAddresses = listOf("ltc1qchangeaddress")
        )

        val score = matcher.matchScoreForRealRecord(pending, real)

        assertTrue(score.isMatch)
        assertEquals(PendingTransactionMatchKind.LitecoinMwebPegIn, score.kind)
    }

    @Test
    fun matchScoreForRealRecord_litecoinMwebPegInPublicAmountAroundMaxBoundary_matchesOnlyWithinLimit() {
        val pending = pendingMwebPegIn(amount = BigDecimal("0.01"))
        val changeAddress = "ltc1qchangeaddress"

        val withinLimit = matcher.matchScoreForRealRecord(
            pending = pending,
            real = publicLitecoinRecord(
                amount = BigDecimal("-0.0999"),
                to = listOf(changeAddress),
                changeAddresses = listOf(changeAddress)
            )
        )
        val aboveLimit = matcher.matchScoreForRealRecord(
            pending = pending,
            real = publicLitecoinRecord(
                amount = BigDecimal("-0.1001"),
                to = listOf(changeAddress),
                changeAddresses = listOf(changeAddress)
            )
        )

        assertTrue(withinLimit.isMatch)
        assertEquals(PendingTransactionMatchKind.LitecoinMwebPegIn, withinLimit.kind)
        assertFalse(aboveLimit.isMatch)
    }

    @Test
    fun matchScoreForRealRecord_litecoinMwebPegInPublicAmountAroundPendingBoundary_matchesOnlyAtOrAbovePending() {
        val pending = pendingMwebPegIn(amount = BigDecimal("0.01"))
        val changeAddress = "ltc1qchangeaddress"

        val equalPending = matcher.matchScoreForRealRecord(
            pending = pending,
            real = publicLitecoinRecord(
                amount = BigDecimal("-0.01"),
                to = listOf(changeAddress),
                changeAddresses = listOf(changeAddress)
            )
        )
        val belowPending = matcher.matchScoreForRealRecord(
            pending = pending,
            real = publicLitecoinRecord(
                amount = BigDecimal("-0.0099"),
                to = listOf(changeAddress),
                changeAddresses = listOf(changeAddress)
            )
        )

        assertTrue(equalPending.isMatch)
        assertEquals(PendingTransactionMatchKind.LitecoinMwebPegIn, equalPending.kind)
        assertFalse(belowPending.isMatch)
    }

    @Test
    fun matchScoreForRealRecord_litecoinMwebPegInTimestampAroundTolerance_matchesOnlyWithinTolerance() {
        val pending = pendingMwebPegIn(amount = BigDecimal("0.01"))
        val changeAddress = "ltc1qchangeaddress"

        val withinTolerance = matcher.matchScoreForRealRecord(
            pending = pending,
            real = publicLitecoinRecord(
                amount = BigDecimal("-0.01"),
                to = listOf(changeAddress),
                changeAddresses = listOf(changeAddress),
                timestamp = TIMESTAMP + LITECOIN_MWEB_PEG_IN_MATCH_TIMESTAMP_TOLERANCE_SECONDS
            )
        )
        val outsideTolerance = matcher.matchScoreForRealRecord(
            pending = pending,
            real = publicLitecoinRecord(
                amount = BigDecimal("-0.01"),
                to = listOf(changeAddress),
                changeAddresses = listOf(changeAddress),
                timestamp = TIMESTAMP + LITECOIN_MWEB_PEG_IN_MATCH_TIMESTAMP_TOLERANCE_SECONDS + 1
            )
        )

        assertTrue(withinTolerance.isMatch)
        assertEquals(PendingTransactionMatchKind.LitecoinMwebPegIn, withinTolerance.kind)
        assertFalse(outsideTolerance.isMatch)
    }

    private fun pendingMwebPegIn(amount: BigDecimal) = PendingTransactionRecord(
        uid = "pending-mweb",
        transactionHash = "mweb-hash",
        timestamp = TIMESTAMP,
        source = source,
        token = token,
        amount = amount,
        toAddress = MWEB_ADDRESS,
        fromAddress = "",
        expiresAt = Long.MAX_VALUE,
        memo = null
    )

    private fun publicLitecoinRecord(
        amount: BigDecimal,
        to: List<String>?,
        changeAddresses: List<String>,
        timestamp: Long = TIMESTAMP
    ) = BitcoinTransactionRecord(
        token = token,
        amount = amount,
        to = to,
        from = null,
        changeAddresses = changeAddresses,
        uid = "public-tx",
        transactionHash = "public-hash",
        transactionIndex = 0,
        blockHeight = null,
        confirmationsThreshold = 1,
        timestamp = timestamp,
        failed = false,
        memo = null,
        source = source,
        sentToSelf = false,
        transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
        fee = null,
        lockInfo = null,
        conflictingHash = null,
        showRawTransaction = true,
        replaceable = false,
    )

    private fun litecoinToken(): Token {
        val blockchain = Blockchain(BlockchainType.Litecoin, "Litecoin", null)
        return Token(
            coin = Coin(uid = "litecoin", name = "Litecoin", code = "LTC"),
            blockchain = blockchain,
            type = TokenType.Derived(TokenType.Derivation.Bip84),
            decimals = 8,
        )
    }

    private fun transactionSource(token: Token): TransactionSource {
        val account = Account(
            id = "account-1",
            name = "Main",
            type = mockk<AccountType>(relaxed = true),
            origin = AccountOrigin.Created,
            level = 0,
        )
        return TransactionSource(blockchain = token.blockchain, account = account, meta = null)
    }

    private companion object {
        const val TIMESTAMP = 1_715_000_000L
        const val MWEB_ADDRESS =
            "ltcmweb1qq2nlqa567pq3hwgch23a9fhuvgfe96erem3v00gph7pjkmwrz09nkq5" +
                "fuwudw8gjmw59n6uv268r3ky23epxkr9fejdf9m6gxlkjy6lne5l82w3k"
    }
}
