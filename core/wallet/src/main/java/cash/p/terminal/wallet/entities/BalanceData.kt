package cash.p.terminal.wallet.entities

import io.horizontalsystems.stellarkit.room.StellarAsset
import java.math.BigDecimal

data class BalanceData(
    val available: BigDecimal,
    val timeLocked: BigDecimal = BigDecimal.ZERO,
    val notRelayed: BigDecimal = BigDecimal.ZERO,
    val pending: BigDecimal = BigDecimal.ZERO,
    val stackingUnpaid: BigDecimal = BigDecimal.ZERO,
    val minimumBalance: BigDecimal = BigDecimal.ZERO,
    val stellarAssets: List<StellarAsset.Asset> = listOf()
) {
    val total get() = available + timeLocked + notRelayed + pending
}