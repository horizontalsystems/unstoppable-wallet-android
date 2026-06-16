package cash.p.terminal.modules.send.zcash

import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import java.math.BigDecimal

internal fun IAdapterManager.getZcashAvailableToSend(
    wallet: Wallet,
    adapter: ISendZcashAdapter,
): BigDecimal = calculateZcashAvailableToSend(
    adjustedAvailable = getAdjustedBalanceData(wallet)?.available,
    adapterAvailable = adapter.balanceData.available,
    fee = adapter.fee.value,
)

internal fun calculateZcashAvailableToSend(
    adjustedAvailable: BigDecimal?,
    adapterAvailable: BigDecimal,
    fee: BigDecimal,
): BigDecimal {
    val available = adjustedAvailable ?: adapterAvailable
    return (available - fee).coerceAtLeast(BigDecimal.ZERO)
}

internal fun IAdapterManager.getZcashSdkBalance(
    wallet: Wallet,
    fallback: BigDecimal,
): BigDecimal =
    getBalanceAdapterForWallet(wallet)?.balanceData?.available ?: fallback
