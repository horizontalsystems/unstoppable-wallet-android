package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.managers.PoisoningScorer
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.nft.NftUid
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Behavioral / end-to-end coverage of transaction spam (address-poisoning) filtering.
 *
 * Unlike PoisoningScorerTest (which mostly pins constants and the isMimicAddress helper), this
 * suite drives the real classification through the scorer's public API — calculateValueScore and
 * calculateCorrelationScore — and composes the two phases exactly as SpamManager.isSpam does, so a
 * regression in the actual decision is caught here.
 *
 * Production dust limits (AppConfigProvider.spamCoinValueLimits): SOL 0.0001, USDT 1.
 * Derived bands per coin: micro-dust < limit/10 (+7), dust < limit (+3), low < limit*5 (+2).
 */
class PoisoningScorerBehaviorTest {

    private val scorer = PoisoningScorer()

    private val limits = mapOf(
        "SOL" to BigDecimal("0.0001"),
        "USDT" to BigDecimal("1"),
    )

    // Real on-chain poisoning of watch address 3VHM…LH7Q (see investigation):
    private val realPayer = "6pRr7fQbrfpYUXZERLmxoKCaqXrz3ix6LQJGHVxSibvF"  // sent 100 SOL
    private val mimic = "6pRrcG2PQrFfUL4dmGQFfFoD6bUnBNFGQ6AyQS2wibvF"      // dust, mimics realPayer
    private val unrelated = "9xQeWvG816bUx9EPjHmaT23yvVM2ZWbrrpZb9PusVFin"

    // ---- helpers -----------------------------------------------------------------------------

    private fun coin(code: String, amount: String, decimals: Int = 9): TransactionValue.CoinValue {
        val token = Token(
            coin = Coin(uid = code.lowercase(), name = code, code = code),
            blockchain = Blockchain(BlockchainType.Solana, "Solana", null),
            type = TokenType.Native,
            decimals = decimals
        )
        return TransactionValue.CoinValue(token, BigDecimal(amount))
    }

    private fun valueScore(value: TransactionValue, address: String? = mimic): Int =
        scorer.calculateValueScore(listOf(TransferEvent(address, value)), limits).score

    /** Mirrors SpamManager.isSpam's composition of the two scoring phases. */
    private fun isSpam(
        value: TransactionValue,
        senderAddress: String = mimic,
        incomingTimestamp: Long = 1_000L,
        incomingBlockHeight: Int? = null,
        context: List<PoisoningScorer.OutgoingTxInfo> = emptyList()
    ): Boolean {
        val events = listOf(TransferEvent(senderAddress, value))
        val valueResult = scorer.calculateValueScore(events, limits)
        if (valueResult.score >= PoisoningScorer.SPAM_THRESHOLD) return true
        if (valueResult.score == 0) return false
        val correlation = scorer.calculateCorrelationScore(
            events, incomingTimestamp, incomingBlockHeight, context
        )
        return valueResult.score + correlation.points >= PoisoningScorer.SPAM_THRESHOLD
    }

    // ==================== Value scoring: auto-spam classes ====================

    @Test
    fun `unknown token (RawValue) is auto-spam`() {
        assertEquals(PoisoningScorer.POINTS_AUTO_SPAM, valueScore(TransactionValue.RawValue(BigInteger.TEN)))
        assertTrue(isSpam(TransactionValue.RawValue(BigInteger.TEN)))
    }

    @Test
    fun `unrecognized token metadata (TokenValue) is auto-spam`() {
        val v = TransactionValue.TokenValue("Free USDT", "USDT", 6, BigDecimal("1000"))
        assertEquals(PoisoningScorer.POINTS_AUTO_SPAM, valueScore(v))
        assertTrue(isSpam(v))
    }

    @Test
    fun `zero-value native coin transfer is auto-spam`() {
        val v = coin("SOL", "0")
        assertEquals(PoisoningScorer.POINTS_AUTO_SPAM, valueScore(v))
        assertTrue(isSpam(v))
    }

    // ==================== Value scoring: SOL dust bands (limit 0.0001) ====================

    @Test
    fun `micro-dust SOL below limit over ten is auto-spam by value alone`() {
        // 0.000005 < 0.00001 (limit/10)
        assertEquals(PoisoningScorer.POINTS_MICRO_DUST, valueScore(coin("SOL", "0.000005")))
        assertTrue(isSpam(coin("SOL", "0.000005"), context = emptyList()))
    }

    @Test
    fun `dust SOL below limit scores gray-zone three`() {
        // 0.00001 <= 0.00005 < 0.0001
        assertEquals(PoisoningScorer.POINTS_DUST_BELOW_LIMIT, valueScore(coin("SOL", "0.00005")))
    }

    @Test
    fun `low-value SOL below five times limit scores two`() {
        // 0.0001 <= 0.0003 < 0.0005
        assertEquals(PoisoningScorer.POINTS_DUST_BELOW_5X_LIMIT, valueScore(coin("SOL", "0.0003")))
    }

    @Test
    fun `normal SOL amount scores zero`() {
        assertEquals(0, valueScore(coin("SOL", "0.01")))
        assertEquals(0, valueScore(coin("SOL", "100")))
    }

    @Test
    fun `SOL dust band boundaries`() {
        assertEquals(PoisoningScorer.POINTS_DUST_BELOW_LIMIT, valueScore(coin("SOL", "0.00001"))) // == limit/10
        assertEquals(PoisoningScorer.POINTS_DUST_BELOW_5X_LIMIT, valueScore(coin("SOL", "0.0001"))) // == limit
        assertEquals(0, valueScore(coin("SOL", "0.0005"))) // == limit*5
    }

    @Test
    fun `dust SOL alone is not spam without correlation context`() {
        // The exact watch-only gap: dust scores 3, no outgoing/incoming context -> stays below 7.
        assertFalse(isSpam(coin("SOL", "0.00005"), context = emptyList()))
    }

    // ==================== Value scoring: token not in limits map ====================

    @Test
    fun `known token absent from limits map gets no dust score`() {
        // BONK is a real (known) SPL token but not in spamCoinValueLimits -> dust scoring skipped.
        // Documents the current limitation: tiny known-token airdrops rely on correlation only.
        assertEquals(0, valueScore(coin("BONK", "0.00000001", decimals = 5)))
        assertFalse(isSpam(coin("BONK", "0.00000001", decimals = 5), context = emptyList()))
    }

    @Test
    fun `USDT dust below limit scores gray-zone three`() {
        assertEquals(PoisoningScorer.POINTS_DUST_BELOW_LIMIT, valueScore(coin("USDT", "0.5", decimals = 6)))
    }

    // ==================== Value scoring: NFTs ====================

    @Test
    fun `zero-value NFT scores three`() {
        val v = TransactionValue.NftValue(NftUid.Solana("mintAddr"), BigDecimal.ZERO, "Scam", "SCAM")
        assertEquals(PoisoningScorer.POINTS_ZERO_NFT, valueScore(v))
    }

    @Test
    fun `non-zero NFT scores zero`() {
        val v = TransactionValue.NftValue(NftUid.Solana("mintAddr"), BigDecimal.ONE, "Real", "REAL")
        assertEquals(0, valueScore(v))
    }

    // ==================== Value scoring: aggregation ====================

    @Test
    fun `value score takes the max across multiple events`() {
        val events = listOf(
            TransferEvent(unrelated, coin("SOL", "100")),            // 0
            TransferEvent(mimic, TransactionValue.RawValue(BigInteger.ONE)) // 7
        )
        assertEquals(PoisoningScorer.POINTS_AUTO_SPAM, scorer.calculateValueScore(events, limits).score)
    }

    @Test
    fun `event with null address contributes nothing`() {
        val events = listOf(TransferEvent(null, TransactionValue.RawValue(BigInteger.ONE)))
        assertEquals(0, scorer.calculateValueScore(events, limits).score)
    }

    @Test
    fun `empty events score zero`() {
        assertEquals(0, scorer.calculateValueScore(emptyList(), limits).score)
    }

    // ==================== Correlation scoring ====================

    private fun context(addr: String, ts: Long = 900L, block: Int? = null) =
        listOf(PoisoningScorer.OutgoingTxInfo(addr, ts, block))

    private fun correlation(
        sender: String,
        incomingTs: Long = 1_000L,
        incomingBlock: Int? = null,
        ctx: List<PoisoningScorer.OutgoingTxInfo>
    ) = scorer.calculateCorrelationScore(
        listOf(TransferEvent(sender, TransactionValue.RawValue(BigInteger.ONE))),
        incomingTs, incomingBlock, ctx
    ).points

    @Test
    fun `prefix and suffix match of a counterparty scores eight`() {
        // realPayer as counterparty; mimic shares first-3 and last-3. Far-apart timestamps so no time bonus.
        val points = correlation(mimic, incomingTs = 10_000_000L, ctx = context(realPayer, ts = 1L))
        assertEquals(PoisoningScorer.POINTS_ADDRESS_PREFIX_MATCH + PoisoningScorer.POINTS_ADDRESS_SUFFIX_MATCH, points)
    }

    @Test
    fun `time correlation within twenty minutes scores three`() {
        // Unrelated address (no prefix/suffix match), within 20 minutes.
        val points = correlation(unrelated, incomingTs = 1_000L, ctx = context(realPayer, ts = 900L))
        assertEquals(PoisoningScorer.POINTS_TIME_WITHIN_20_MINUTES, points)
    }

    @Test
    fun `block correlation within five blocks scores four and suppresses time`() {
        // Same block window AND same time window, but the two are mutually exclusive: block wins.
        val points = correlation(
            unrelated, incomingTs = 1_000L, incomingBlock = 100,
            ctx = context(realPayer, ts = 900L, block = 103)
        )
        assertEquals(PoisoningScorer.POINTS_TIME_WITHIN_5_BLOCKS, points)
    }

    @Test
    fun `no correlation when context is empty`() {
        assertEquals(0, correlation(mimic, ctx = emptyList()))
    }

    @Test
    fun `identical address is not treated as a mimic`() {
        // Address equal to a counterparty: prefix/suffix helper rejects equal addresses; only time may apply.
        val points = correlation(realPayer, incomingTs = 10_000_000L, ctx = context(realPayer, ts = 1L))
        assertEquals(0, points)
    }

    @Test
    fun `unrelated dust far in time yields no correlation`() {
        val points = correlation(unrelated, incomingTs = 10_000_000L, ctx = context(realPayer, ts = 1L))
        assertEquals(0, points)
    }

    // ==================== End-to-end: the real poisoning scenario ====================

    @Test
    fun `dust mimic of a counterparty is spam when context is present`() {
        // 100 SOL received from realPayer, then 0.00005 SOL dust from the look-alike, seconds later.
        // dust(3) + prefix(4) + suffix(4) + time(3) = 14 >= 7.
        val caught = isSpam(
            value = coin("SOL", "0.00005"),
            senderAddress = mimic,
            incomingTimestamp = 1_000L,
            context = context(realPayer, ts = 985L)
        )
        assertTrue(caught)
    }

    @Test
    fun `same dust mimic is missed without any counterparty context`() {
        // Regression guard for the watch-only / outgoing-only gap that originally hid this scam.
        val caught = isSpam(
            value = coin("SOL", "0.00005"),
            senderAddress = mimic,
            context = emptyList()
        )
        assertFalse(caught)
    }

    @Test
    fun `low-value transfer that merely correlates in time is not spam`() {
        // low-value(2) + time(3) = 5 < 7. Time proximity alone must not over-flag near-normal amounts.
        val caught = isSpam(
            value = coin("SOL", "0.0003"),
            senderAddress = unrelated,
            incomingTimestamp = 1_000L,
            context = context(realPayer, ts = 950L)
        )
        assertFalse(caught)
    }

    @Test
    fun `normal-value transfer never reaches correlation and is not spam`() {
        // A mimic address that sends a NORMAL amount scores 0 in phase 1 and is never correlated.
        val caught = isSpam(
            value = coin("SOL", "5"),
            senderAddress = mimic,
            context = context(realPayer, ts = 985L)
        )
        assertFalse(caught)
    }
}
