package io.horizontalsystems.bankwallet.modules.wallet

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.disposables.CompositeDisposable

class WalletInteractor(
        private val walletManager: IWalletManager,
        private val rateManager: RateManager,
        private val currencyManager: ICurrencyManager) : WalletModule.IInteractor {

    var delegate: WalletModule.IInteractorDelegate? = null
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
            disposables.add(wallet.adapter.balanceSubject.subscribe {
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

    override fun rate(coin: String): Rate? {
        return rateManager.rate(coin, currencyManager.baseCurrency.code)
    }

    override fun refresh() {
        walletManager.refreshWallets()

        delegate?.didRefresh()
    }

    override val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    override val wallets: List<Wallet>
        get() = walletManager.wallets

}
