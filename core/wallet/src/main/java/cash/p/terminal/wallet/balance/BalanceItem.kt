package cash.p.terminal.wallet.balance

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.models.CoinPrice

data class BalanceItem(
    val wallet: Wallet,
    val balanceData: BalanceData,
    val state: AdapterState,
    val sendAllowed: Boolean,
    val coinPrice: CoinPrice?,
    val warning: BalanceWarning? = null
) {
    val fiatValue get() = coinPrice?.value?.let { balanceData.available.times(it) }
    val balanceFiatTotal get() = coinPrice?.value?.let { balanceData.total.times(it) }
}