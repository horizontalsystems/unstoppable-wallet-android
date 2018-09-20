package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.ExchangeRateManager
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.DollarCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class WalletInteractor(private val adapterManager: AdapterManager, private val exchangeRateManager: ExchangeRateManager, private val storage: ILocalStorage) : WalletModule.IInteractor {

    var delegate: WalletModule.IInteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    override fun notifyWalletBalances() {
        adapterManager.subject.subscribe {
            disposables.clear()
            initialFetchAndSubscribe()
        }

        initialFetchAndSubscribe()
    }

    private fun initialFetchAndSubscribe() {
        val coinValues = mutableMapOf<String, CoinValue>()
        val progresses = mutableMapOf<String, BehaviorSubject<Double>>()
        val currency = DollarCurrency()

        adapterManager.adapters.forEach { adapter ->
            coinValues[adapter.id] = CoinValue(adapter.coin, adapter.balance)
            progresses[adapter.id] = adapter.progressSubject
        }

        val exchangeRates = ExchangeRateManager.exchangeRates
        delegate?.didInitialFetch(coinValues, exchangeRates, progresses, currency)

        adapterManager.adapters.forEach { adapter ->
            disposables.add(adapter.balanceSubject.subscribe {
                delegate?.didUpdate(CoinValue(adapter.coin, it), adapterId = adapter.id)
            })
        }

        exchangeRateManager.subject.subscribe {
            delegate?.didUpdate(it)
        }

    }

    override fun checkIfPinSet() {
        if (storage.getPin() == null) {
            delegate?.onPinNotSet()
        }
    }
}
