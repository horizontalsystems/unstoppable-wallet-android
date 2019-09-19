package io.horizontalsystems.bankwallet.modules.balance

import android.app.Activity
import android.os.Handler
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.BackgroundManager
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.core.managers.StatsError
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class BalanceInteractor(
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val rateStatsManager: IRateStatsManager,
        private val currencyManager: ICurrencyManager,
        private val backgroundManager: BackgroundManager,
        private val rateStorage: IRateStorage,
        private val localStorage: ILocalStorage,
        private val rateManager: IRateManager,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val refreshTimeout: Long = 2) : BalanceModule.IInteractor,  BackgroundManager.Listener {

    var delegate: BalanceModule.IInteractorDelegate? = null

    private var disposables = CompositeDisposable()
    private var adapterDisposables = CompositeDisposable()
    private var rateDisposables = CompositeDisposable()

    // BackgroundManager.Listener

    override fun willEnterForeground(activity: Activity) {
        delegate?.willEnterForeground()
    }

    override fun didEnterBackground() {}

    // BalanceModule.IInteractor

    override fun syncStats(coinCode: String, currencyCode: String) {
        rateStatsManager.syncStats(coinCode, currencyCode)
    }

    override fun initWallets() {
        backgroundManager.registerListener(this)

        onUpdateWallets()
        onUpdateCurrency()

        walletManager.walletsUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { onUpdateWallets() }
                .let { disposables.add(it) }

        adapterManager.adapterCreationObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { subscribeToAdapterUpdates(it, true) }
                .let { disposables.add(it) }

        currencyManager.baseCurrencyUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { onUpdateCurrency() }
                .let { disposables.add(it) }

        rateStatsManager.statsFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    when (it) {
                        is StatsData -> delegate?.onReceiveRateStats(it)
                        is StatsError -> delegate?.onFailFetchChartStats(it.coinCode)
                    }
                }, {
                })
                .let { disposables.add(it) }
    }

    override fun fetchRates(currencyCode: String, coinCodes: List<CoinCode>) {
        rateDisposables.clear()
        coinCodes.forEach { getLatestRate(currencyCode, it) }
    }

    override fun getSortingType() = localStorage.sortType

    override fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter? {
        return adapterManager.getBalanceAdapterForWallet(wallet)
    }

    override fun predefinedAccountType(wallet: Wallet): IPredefinedAccountType? {
        return predefinedAccountTypeManager.predefinedAccountType(wallet.account.type)
    }

    private fun onUpdateCurrency() {
        delegate?.didUpdateCurrency(currencyManager.baseCurrency)
    }

    @Synchronized
    private fun onUpdateWallets() {
        adapterDisposables.clear()

        val wallets = walletManager.wallets

        delegate?.didUpdateWallets(wallets)

        wallets.forEach { subscribeToAdapterUpdates(it, false) }
    }

    @Synchronized
    private fun subscribeToAdapterUpdates(wallet: Wallet, initialUpdate: Boolean) {
        adapterManager.getBalanceAdapterForWallet(wallet)?.let { adapter ->

            if (initialUpdate) {
                delegate?.didUpdateBalance(wallet, adapter.balance)
                delegate?.didUpdateState(wallet, adapter.state)
            }

            adapterDisposables.add(adapter.balanceUpdatedFlowable
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        delegate?.didUpdateBalance(wallet, adapter.balance)
                    })

            adapterDisposables.add(adapter.stateUpdatedFlowable
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        delegate?.didUpdateState(wallet, adapter.state)
                    })
        }
    }

    override fun refresh() {
        adapterManager.refresh()
        rateManager.syncLatestRates()

        Handler().postDelayed({ delegate?.didRefresh() }, refreshTimeout * 1000)
    }

    override fun clear() {
        backgroundManager.unregisterListener(this)
        disposables.clear()
        adapterDisposables.clear()
        rateDisposables.clear()
    }

    override fun saveSortingType(sortType: BalanceSortType) {
        localStorage.sortType = sortType
    }

    private fun getLatestRate(currencyCode: String, coinCode: String) {
        rateStorage.latestRateObservable(coinCode, currencyCode)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    delegate?.didUpdateRate(it)
                }
                .let { rateDisposables.add(it) }
    }
}
