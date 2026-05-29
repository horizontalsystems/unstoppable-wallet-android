package cash.p.terminal.core.managers

import cash.p.terminal.core.tryOrNull
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.litecoinkit.LitecoinKit
import io.horizontalsystems.litecoinkit.mweb.address.MwebAddressCodec
import io.horizontalsystems.tonkit.Address
import java.math.BigDecimal
import kotlin.math.abs

data class MatchScore(
    val isMatch: Boolean,
    val confidence: Double,
    val kind: PendingTransactionMatchKind = PendingTransactionMatchKind.Regular
)

const val PENDING_TRANSACTION_MATCH_TIMESTAMP_TOLERANCE_SECONDS = 10
const val LITECOIN_MWEB_PEG_IN_MATCH_TIMESTAMP_TOLERANCE_SECONDS = 10

enum class PendingTransactionMatchKind {
    Regular,
    LitecoinMwebPegIn
}

class PendingTransactionMatcher {
    private companion object {
        const val FUZZY_MATCH_CONFIDENCE = 0.8
        const val LITECOIN_MWEB_PEG_IN_CONFIDENCE = 0.85
        const val ADDRESS_MATCH_CONFIDENCE = 0.9
        // Pending and real transaction amounts may be rounded by different data sources.
        val AMOUNT_MATCH_THRESHOLD_RATE = BigDecimal("0.001")
        val LITECOIN_MWEB_PEG_IN_MAX_PUBLIC_AMOUNT_RATE = BigDecimal("10")

        val LITECOIN_MWEB_ADDRESS_CODECS = listOf(
            MwebAddressCodec(LitecoinKit.NetworkType.MainNet)
        )
    }

    fun matchScoreForRealRecord(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): MatchScore {
        if (pending.transactionHash.isNotEmpty() && pending.transactionHash == real.transactionHash) {
            return MatchScore(isMatch = true, confidence = 1.0)
        }

        if (real is PendingTransactionRecord || !real.isOutgoingForPendingMatch()) {
            return MatchScore(isMatch = false, confidence = 0.0)
        }

        if (real.assetTokenForPendingMatch() != pending.token) {
            return MatchScore(isMatch = false, confidence = 0.0)
        }

        litecoinMwebMatchScore(pending, real)?.let {
            return it
        }

        return calculateFuzzyMatchScore(
            timestampPending = pending.timestamp,
            blockchainTypeUid = pending.blockchainType.uid,
            pendingAmount = pending.amount.abs(),
            toAddress = pending.to?.firstOrNull() ?: "",
            real = real
        )
    }

    fun isMatchingRealRecord(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): Boolean = matchScoreForRealRecord(pending, real).isMatch

    private fun calculateFuzzyMatchScore(
        timestampPending: Long,
        blockchainTypeUid: String,
        pendingAmount: BigDecimal,
        toAddress: String,
        real: TransactionRecord
    ): MatchScore {
        val blockchainMatches = blockchainTypeUid == real.blockchainType.uid
        val amountMatches = compareAmounts(pendingAmount, real)
        val timestampMatches = compareTimestamps(timestampPending, real.timestamp)
        val realTo = real.to?.firstOrNull()

        if (blockchainMatches && amountMatches && timestampMatches) {
            val addressMatches = compareAddresses(blockchainTypeUid, toAddress, realTo)

            return MatchScore(
                isMatch = true,
                confidence = if (addressMatches) ADDRESS_MATCH_CONFIDENCE else FUZZY_MATCH_CONFIDENCE
            )
        }

        return MatchScore(isMatch = false, confidence = 0.0)
    }

    private fun litecoinMwebMatchScore(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): MatchScore? {
        if (!isLitecoinMwebMatchCandidate(pending, real)) {
            return null
        }

        if (isLitecoinMwebLocalIdentifierMatch(pending, real)) {
            return MatchScore(isMatch = true, confidence = 1.0)
        }

        if (isLitecoinMwebPegInPending(pending)) {
            return litecoinMwebPegInMatchScore(pending, real)
                ?: MatchScore(isMatch = false, confidence = 0.0)
        }

        return null
    }

    private fun isLitecoinMwebMatchCandidate(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): Boolean {
        return pending.blockchainType == BlockchainType.Litecoin &&
            real.blockchainType == BlockchainType.Litecoin
    }

    private fun isLitecoinMwebLocalIdentifierMatch(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): Boolean {
        val pendingIdentifier = pending.transactionHash.takeIf { it.isNotBlank() } ?: return false
        val bitcoinRecord = real as? BitcoinTransactionRecord ?: return false
        val canonicalHash = bitcoinRecord.canonicalTransactionHash?.takeIf { it.isNotBlank() } ?: return false

        return pending.token.type == TokenType.Mweb &&
            canonicalHash.equals(real.transactionHash, ignoreCase = true) &&
            real.uid.matchesMwebLocalIdentifier(pendingIdentifier) &&
            compareAmounts(pending.amount.abs(), real) &&
            compareAddresses(
                blockchainTypeUid = pending.blockchainType.uid,
                pendingTo = pending.to?.firstOrNull(),
                realTo = real.to?.firstOrNull()
            )
    }

    private fun compareAddresses(
        blockchainTypeUid: String,
        pendingTo: String?,
        realTo: String?,
    ): Boolean {
        val pendingAddress = pendingTo?.takeIf { it.isNotBlank() } ?: return false
        val realAddress = realTo?.takeIf { it.isNotBlank() } ?: return false

        if (blockchainTypeUid == BlockchainType.Ton.uid) {
            val normalizedPending = normalizeTonAddress(pendingAddress)
            val normalizedReal = normalizeTonAddress(realAddress)
            if (normalizedPending != null || normalizedReal != null) {
                return normalizedPending == normalizedReal
            }
        }

        return pendingAddress.equals(realAddress, ignoreCase = true)
    }

    private fun normalizeTonAddress(address: String): String? {
        return tryOrNull { Address.parse(address).toRaw() }
    }

    private fun litecoinMwebPegInMatchScore(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): MatchScore? {
        if (!real.hasLitecoinMwebPegInDestination()) {
            return null
        }
        if (!compareTimestamps(
                timestampPending = pending.timestamp,
                timestampReal = real.timestamp,
                toleranceSeconds = LITECOIN_MWEB_PEG_IN_MATCH_TIMESTAMP_TOLERANCE_SECONDS
            )
        ) {
            return null
        }

        val pendingAmount = pending.amount.abs()
        val realAmount = getRealAmount(real)?.abs() ?: return null
        // Public peg-in spends selected public UTXOs into the extension output, including MWEB-side change.
        val maxPublicAmount = pendingAmount.multiply(LITECOIN_MWEB_PEG_IN_MAX_PUBLIC_AMOUNT_RATE)
        if (realAmount !in pendingAmount..maxPublicAmount) {
            return null
        }

        return MatchScore(
            isMatch = true,
            confidence = LITECOIN_MWEB_PEG_IN_CONFIDENCE,
            kind = PendingTransactionMatchKind.LitecoinMwebPegIn
        )
    }

    private fun isLitecoinMwebPegInPending(pending: PendingTransactionRecord): Boolean {
        return pending.to.orEmpty().any { it.isLitecoinMwebAddress() }
    }

    fun calculateMatchScore(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): MatchScore = matchScoreForRealRecord(pending, real)

    private fun TransactionRecord.isOutgoingForPendingMatch(): Boolean {
        return when (transactionRecordType) {
            TransactionRecordType.BITCOIN_OUTGOING,
            TransactionRecordType.EVM_OUTGOING,
            TransactionRecordType.EVM_SWAP,
            TransactionRecordType.EVM_UNKNOWN_SWAP,
            TransactionRecordType.TRON_OUTGOING,
            TransactionRecordType.SOLANA_OUTGOING,
            TransactionRecordType.MONERO_OUTGOING,
            TransactionRecordType.STELLAR_OUTGOING -> true

            TransactionRecordType.TON -> to != null && from == null

            TransactionRecordType.UNKNOWN,
            TransactionRecordType.SOLANA_INCOMING,
            TransactionRecordType.SOLANA_UNKNOWN,
            TransactionRecordType.BITCOIN_INCOMING,
            TransactionRecordType.EVM,
            TransactionRecordType.EVM_APPROVE,
            TransactionRecordType.EVM_CONTRACT_CALL,
            TransactionRecordType.EVM_CONTRACT_CREATION,
            TransactionRecordType.EVM_INCOMING,
            TransactionRecordType.EVM_EXTERNAL_CONTRACT_CALL,
            TransactionRecordType.TRON,
            TransactionRecordType.TRON_APPROVE,
            TransactionRecordType.TRON_CONTRACT_CALL,
            TransactionRecordType.TRON_EXTERNAL_CONTRACT_CALL,
            TransactionRecordType.TRON_INCOMING,
            TransactionRecordType.MONERO_INCOMING,
            TransactionRecordType.STELLAR_INCOMING -> false
        }
    }

    private fun TransactionRecord.assetTokenForPendingMatch(): Token? {
        return (getRealValue(this) as? TransactionValue.CoinValue)?.token
            ?: token
    }

    private fun compareAmounts(
        pendingAmount: BigDecimal,
        real: TransactionRecord
    ): Boolean {
        val realAmount = getRealAmount(real)?.abs() ?: return false

        val difference = (pendingAmount - realAmount).abs()
        val threshold = pendingAmount.multiply(AMOUNT_MATCH_THRESHOLD_RATE)
        return difference <= threshold
    }

    private fun compareTimestamps(
        timestampPending: Long,
        timestampReal: Long,
        toleranceSeconds: Int = PENDING_TRANSACTION_MATCH_TIMESTAMP_TOLERANCE_SECONDS
    ): Boolean {
        val differenceSeconds = abs(timestampPending - timestampReal)
        return differenceSeconds <= toleranceSeconds.toLong()
    }

    private fun getRealAmount(
        real: TransactionRecord
    ): BigDecimal? {
        return getRealValue(real)?.decimalValue
    }

    private fun getRealValue(real: TransactionRecord): TransactionValue? {
        if (real is TonTransactionRecord) {
            return real.actions.firstOrNull { it.type is TonTransactionRecord.Action.Type.Swap }
                ?.let { action ->
                    (action.type as? TonTransactionRecord.Action.Type.Swap)?.valueIn
                }
                ?: real.actions.firstOrNull { it.type is TonTransactionRecord.Action.Type.Send }
                    ?.let { action ->
                        (action.type as? TonTransactionRecord.Action.Type.Send)?.value
                    }
        }

        if (real is EvmTransactionRecord) {
            return real.valueIn ?: real.mainValue
        }

        return real.mainValue
    }

    private fun String.isLitecoinMwebAddress(): Boolean {
        return LITECOIN_MWEB_ADDRESS_CODECS.any { it.isValid(this) }
    }

    private fun String.matchesMwebLocalIdentifier(identifier: String): Boolean {
        return equals(identifier, ignoreCase = true) ||
            substringAfterLast(':').equals(identifier, ignoreCase = true)
    }

    private fun TransactionRecord.hasLitecoinMwebPegInDestination(): Boolean {
        if (this !is BitcoinTransactionRecord) {
            return false
        }

        val changeAddresses = changeAddresses.orEmpty()
        val recipients = to ?: return changeAddresses.isNotEmpty()
        val publicRecipients = recipients.filter { it.isNotBlank() }
        if (publicRecipients.isEmpty()) {
            return changeAddresses.isNotEmpty()
        }

        return changeAddresses.isNotEmpty() && publicRecipients.all { recipient ->
            changeAddresses.any { changeAddress ->
                recipient.equals(changeAddress, ignoreCase = true)
            }
        }
    }
}
