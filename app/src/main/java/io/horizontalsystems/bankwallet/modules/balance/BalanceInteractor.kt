package io.horizontalsystems.bankwallet.modules.balance

import android.os.Handler
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class BalanceInteractor(
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val rateStorage: IRateStorage,
        private val currencyManager: ICurrencyManager,
        private val localStorage: ILocalStorage,
        private val refreshTimeout: Double = 2.0
) : BalanceModule.IInteractor {

    var delegate: BalanceModule.IInteractorDelegate? = null

    private var disposables: CompositeDisposable = CompositeDisposable()
    private var adapterDisposables: CompositeDisposable = CompositeDisposable()
    private var rateDisposables: CompositeDisposable = CompositeDisposable()

    override fun initWallets() {
        onUpdateWallets()
        onUpdateCurrency()

        disposables.add(walletManager.walletsUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateWallets()
                })

        disposables.add(adapterManager.adapterCreationObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    subscribeToAdapterUpdates(it, true)
                })

        disposables.add(currencyManager.baseCurrencyUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateCurrency()
                })
    }

    override fun fetchRates(currencyCode: String, coinCodes: List<CoinCode>) {
        rateDisposables.clear()

        coinCodes.forEach {
            rateDisposables.add(rateStorage.latestRateObservable(it, currencyCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        delegate?.didUpdateRate(it)
                    })
        }
    }

    override fun getSortingType() = localStorage.sortType

    override fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter? {
        return adapterManager.getBalanceAdapterForWallet(wallet)
    }

    private fun onUpdateCurrency() {
        delegate?.didUpdateCurrency(currencyManager.baseCurrency)
    }

    @Synchronized
    private fun onUpdateWallets() {
        adapterDisposables.clear()

        val wallets = walletManager.wallets

        delegate?.didUpdateWallets(wallets)

        wallets.forEach { wallet ->
            subscribeToAdapterUpdates(wallet, false)
        }
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

        Handler().postDelayed({
            delegate?.didRefresh()
        }, (refreshTimeout * 1000).toLong())
    }

    override fun clear() {
        disposables.clear()
        adapterDisposables.clear()
        rateDisposables.clear()
    }

    override fun saveSortingType(sortType: BalanceSortType) {
        localStorage.sortType = sortType
    }
}
