package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.IExchangeRateManager
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.entities.CoinValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class WalletInteractor(
        private val adapterManager: AdapterManager,
        private val exchangeRateManager: IExchangeRateManager,
        private val storage: ILocalStorage) : WalletModule.IInteractor {

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

    override fun checkIfPinSet() {
        if (storage.pinIsEmpty()) {
            delegate?.onPinNotSet()
        }
    }
}
