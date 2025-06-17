package cash.p.terminal.tangem.domain

import cash.p.terminal.tangem.domain.derivation.DerivationStyle
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import java.util.Locale

private val firstCardSeries = listOf("CB61", "CB64")
private val secondCardSeries = listOf("CB62", "CB65")

val RING_BATCH_IDS = listOf("AC17", "BA01")
const val RING_BATCH_PREFIX = "BA"

fun Card.isTangemTwins(): Boolean = when {
    firstCardSeries.map { cardId.startsWith(it) }.contains(true) -> {
        TwinCardNumber.First
    }

    secondCardSeries.map { cardId.startsWith(it) }.contains(true) -> {
        TwinCardNumber.Second
    }

    else -> {
        null
    }
} != null

fun Card.getDerivationStyle(): DerivationStyle? {
    return when {
        !settings.isHDWalletAllowed -> null
        firstBatchesOfWallet1() -> DerivationStyle.V1
        isWallet2() -> DerivationStyle.V3
        else -> DerivationStyle.V2
    }
}

private fun Card.firstBatchesOfWallet1(): Boolean {
    return batchId == "AC01" || batchId == "AC02" || batchId == "CB95"
}

fun Card.isWallet2(): Boolean =
    firmwareVersion >= FirmwareVersion.Ed25519Slip0010Available && settings.isKeysImportAllowed

fun Card.isExcluded(): Boolean {
    val excludedBatch = excludedBatches.contains(batchId)
    val excludedIssuerName = excludedIssuers.contains(issuer.name.uppercase(Locale.ROOT))
    return excludedBatch || excludedIssuerName
}

private val excludedBatches = listOf("0027", "0030", "0031", "0035", "DA88", "AF56")
private val excludedIssuers = listOf("TTM BANK")

fun Card.totalSignedHashes() =
    wallets.sumOf { it.totalSignedHashes ?: 0 }

private enum class TwinCardNumber(val number: Int) {
    First(1), Second(2);
}