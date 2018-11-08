package io.horizontalsystems.bankwallet.modules.wallet

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IExchangeRateManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class WalletInteractor(
        private val adapterManager: IAdapterManager,
        private val exchangeRateManager: IExchangeRateManager) : WalletModule.IInteractor {

    var delegate: WalletModule.IInteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()
    private var adapterManagerDisposable: Disposable? = null

    override fun notifyWalletBalances() {
        adapterManagerDisposable = adapterManager.subject.subscribe {
            disposables.clear()
            initialFetchAndSubscribe()
        }

        initialFetchAndSubscribe()
    }

    private fun initialFetchAndSubscribe() {
        val coinValues = mutableMapOf<String, CoinValue>()
        val progresses = mutableMapOf<String, BehaviorSubject<Double>>()

        adapterManager.adapters.forEach { adapter ->
            coinValues[adapter.id] = CoinValue(adapter.coin, adapter.balance)
            progresses[adapter.id] = adapter.progressSubject
        }

        delegate?.didInitialFetch(coinValues, exchangeRateManager.getExchangeRates(), progresses)

        adapterManager.adapters.forEach { adapter ->
            disposables.add(adapter.balanceSubject.subscribe {
                delegate?.didUpdate(CoinValue(adapter.coin, it), adapterId = adapter.id)
            })
        }

        disposables.add(exchangeRateManager.getLatestExchangeRateSubject().subscribe {
            delegate?.didExchangeRateUpdate(it)
        })

    }

}
