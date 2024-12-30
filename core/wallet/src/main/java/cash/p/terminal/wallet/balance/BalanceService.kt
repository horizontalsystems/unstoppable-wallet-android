package cash.p.terminal.wallet.balance

import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.BalanceSortType
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.Currency
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.StateFlow

interface BalanceService : Clearable {
    val networkAvailable: Boolean
    val baseCurrency: Currency
    var sortType: BalanceSortType
    val isWatchAccount: Boolean
    val account: Account?
    val disabledWalletSubject: PublishSubject<Wallet>
    fun start()

    suspend fun refresh()

    override fun clear()
    fun disable(wallet: Wallet)
    fun enable(wallet: Wallet)
    val balanceItemsFlow: StateFlow<List<BalanceItem>?>
}