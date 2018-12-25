package io.horizontalsystems.bankwallet.modules.balance

import android.os.Handler
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class BalanceInteractor(
        private val walletManager: IWalletManager,
        private val rateManager: RateManager,
        private val currencyManager: ICurrencyManager,
        private val refreshTimeout: Double = 2.0
) : BalanceModule.IInteractor {

    var delegate: BalanceModule.IInteractorDelegate? = null

    private var disposables: CompositeDisposable = CompositeDisposable()

    override fun loadWallets() {
        val walletManagerDisposable = walletManager.walletsSubject.subscribe {
            disposables.clear()
            initialFetchAndSubscribe()
        }
        initialFetchAndSubscribe()
    }

    private fun initialFetchAndSubscribe() {
        walletManager.wallets.forEach { wallet ->
            disposables.add(wallet.adapter.balanceSubject
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        delegate?.didUpdate()
                    })
            disposables.add(wallet.adapter.stateSubject.subscribe {
                delegate?.didUpdate()
            })
        }

        disposables.add(rateManager.subject.subscribe {
            delegate?.didUpdate()
        })

        disposables.add(currencyManager.subject.subscribe {
            delegate?.didUpdate()
        })
    }

    override fun rate(coin: String): Maybe<Rate> {
        return rateManager.rate(coin, currencyManager.baseCurrency.code)
    }

    override fun refresh() {
        walletManager.refreshWallets()

        Handler().postDelayed({
            delegate?.didRefresh()
        }, (refreshTimeout * 1000).toLong())
    }

    override val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    override val wallets: List<Wallet>
        get() = walletManager.wallets

}
