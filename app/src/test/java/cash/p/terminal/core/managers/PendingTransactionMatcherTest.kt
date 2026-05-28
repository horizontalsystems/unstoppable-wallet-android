package cash.p.terminal.core.managers

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.tonkit.Address
import io.horizontalsystems.tonkit.models.Event
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

    @Test
    fun matchScoreForRealRecord_litecoinMwebPegOutCanonicalHashWithLocalOutputId_matches() {
        val localOutputId = "created-output-id"
        val pending = pendingMwebPegOut(
            amount = BigDecimal("0.01"),
            transactionHash = localOutputId,
        )
        val real = mwebLitecoinRecord(
            amount = BigDecimal("-0.01"),
            uid = "mweb-outgoing:$localOutputId",
            transactionHash = "canonical-public-hash",
            canonicalTransactionHash = "canonical-public-hash",
            to = listOf(PUBLIC_ADDRESS),
            timestamp = TIMESTAMP + 600,
        )

        val score = matcher.matchScoreForRealRecord(pending, real)

        assertTrue(score.isMatch)
    }

    @Test
    fun matchScoreForRealRecord_litecoinMwebPegOutCanonicalHashWithDifferentAddress_doesNotMatch() {
        val localOutputId = "created-output-id"
        val pending = pendingMwebPegOut(
            amount = BigDecimal("0.01"),
            transactionHash = localOutputId,
        )
        val real = mwebLitecoinRecord(
            amount = BigDecimal("-0.01"),
            uid = "mweb-outgoing:$localOutputId",
            transactionHash = "canonical-public-hash",
            canonicalTransactionHash = "canonical-public-hash",
            to = listOf("ltc1differentdestination"),
            timestamp = TIMESTAMP + 600,
        )

        val score = matcher.matchScoreForRealRecord(pending, real)

        assertFalse(score.isMatch)
    }

    @Test
    fun matchScoreForRealRecord_tonSwapSameRouterDifferentAddressFormat_matchesWithAddressConfidence() {
        val token = tonToken()
        val pending = pendingTonSwap(
            token = token,
            routerAddress = TON_ROUTER_RAW,
            amount = BigDecimal("0.5747"),
        )
        val real = tonSwapRecord(
            token = token,
            routerAddress = Address.parse(TON_ROUTER_RAW).toUserFriendly(bounceable = true),
            amountIn = BigDecimal("-0.5747"),
        )

        val score = matcher.matchScoreForRealRecord(pending, real)

        assertTrue(score.isMatch)
        assertEquals(0.9, score.confidence)
    }

    @Test
    fun matchScoreForRealRecord_tonSwapSameUnparseableAddress_matchesWithAddressConfidence() {
        val token = tonToken()
        val routerAddress = "same-ton-router-reference"
        val pending = pendingTonSwap(
            token = token,
            routerAddress = routerAddress,
            amount = BigDecimal("0.5747"),
        )
        val real = tonSwapRecord(
            token = token,
            routerAddress = routerAddress,
            amountIn = BigDecimal("-0.5747"),
        )

        val score = matcher.matchScoreForRealRecord(pending, real)

        assertTrue(score.isMatch)
        assertEquals(0.9, score.confidence)
    }

    @Test
    fun matchScoreForRealRecord_tonSwapCaseChangedUserFriendlyAddress_doesNotUseAddressConfidence() {
        val token = tonToken()
        val routerAddress = Address.parse(TON_ROUTER_RAW).toUserFriendly(bounceable = true)
        val pending = pendingTonSwap(
            token = token,
            routerAddress = routerAddress.lowercase(),
            amount = BigDecimal("0.5747"),
        )
        val real = tonSwapRecord(
            token = token,
            routerAddress = routerAddress,
            amountIn = BigDecimal("-0.5747"),
        )

        val score = matcher.matchScoreForRealRecord(pending, real)

        assertTrue(score.isMatch)
        assertEquals(0.8, score.confidence)
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

    private fun pendingMwebPegOut(
        amount: BigDecimal,
        transactionHash: String,
    ) = PendingTransactionRecord(
        uid = "pending-mweb-peg-out",
        transactionHash = transactionHash,
        timestamp = TIMESTAMP,
        source = mwebSource,
        token = mwebToken,
        amount = amount,
        toAddress = PUBLIC_ADDRESS,
        fromAddress = "",
        expiresAt = Long.MAX_VALUE,
        memo = null
    )

    private fun pendingTonSwap(
        token: Token,
        routerAddress: String,
        amount: BigDecimal,
    ) = PendingTransactionRecord(
        uid = "pending-ton-swap",
        transactionHash = "",
        timestamp = TIMESTAMP,
        source = transactionSource(token),
        token = token,
        amount = amount,
        toAddress = routerAddress,
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

    private fun mwebLitecoinRecord(
        amount: BigDecimal,
        uid: String,
        transactionHash: String,
        canonicalTransactionHash: String?,
        to: List<String>?,
        timestamp: Long,
    ) = BitcoinTransactionRecord(
        token = mwebToken,
        amount = amount,
        to = to,
        from = null,
        changeAddresses = null,
        uid = uid,
        transactionHash = transactionHash,
        transactionIndex = 0,
        blockHeight = null,
        confirmationsThreshold = 1,
        timestamp = timestamp,
        failed = false,
        memo = null,
        source = mwebSource,
        sentToSelf = false,
        transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
        fee = null,
        lockInfo = null,
        conflictingHash = null,
        showRawTransaction = false,
        replaceable = false,
        canonicalTransactionHash = canonicalTransactionHash,
    )

    private fun tonSwapRecord(
        token: Token,
        routerAddress: String,
        amountIn: BigDecimal,
    ) = TonTransactionRecord(
        source = transactionSource(token),
        event = Event(
            id = "ton-swap-tx",
            lt = 1L,
            timestamp = TIMESTAMP,
            scam = false,
            inProgress = false,
            extra = 0L,
            actions = emptyList()
        ),
        token = token,
        actions = listOf(
            TonTransactionRecord.Action(
                type = TonTransactionRecord.Action.Type.Swap(
                    routerName = "STON.fi DEX",
                    routerAddress = routerAddress,
                    valueIn = TransactionValue.CoinValue(token, amountIn),
                    valueOut = TransactionValue.JettonValue(
                        name = "Pirate Cash",
                        symbol = "PIRATE",
                        decimals = 9,
                        value = BigDecimal("62.6"),
                        image = null,
                    )
                ),
                status = TransactionStatus.Completed
            )
        )
    )

    private fun litecoinToken(
        type: TokenType = TokenType.Derived(TokenType.Derivation.Bip84),
    ): Token {
        val blockchain = Blockchain(BlockchainType.Litecoin, "Litecoin", null)
        return Token(
            coin = Coin(uid = "litecoin", name = "Litecoin", code = "LTC"),
            blockchain = blockchain,
            type = type,
            decimals = 8,
        )
    }

    private fun tonToken(): Token {
        val blockchain = Blockchain(BlockchainType.Ton, "The Open Network", null)
        return Token(
            coin = Coin(uid = "toncoin", name = "The Open Network", code = "TONCOIN"),
            blockchain = blockchain,
            type = TokenType.Native,
            decimals = 9,
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
        const val TON_ROUTER_RAW =
            "0:1111111111111111111111111111111111111111111111111111111111111111"
        const val PUBLIC_ADDRESS = "ltc1qpublicdestination"
        const val MWEB_ADDRESS =
            "ltcmweb1qq2nlqa567pq3hwgch23a9fhuvgfe96erem3v00gph7pjkmwrz09nkq5" +
                "fuwudw8gjmw59n6uv268r3ky23epxkr9fejdf9m6gxlkjy6lne5l82w3k"
    }

    private val mwebToken = litecoinToken(TokenType.Mweb)
    private val mwebSource = transactionSource(mwebToken)
}
