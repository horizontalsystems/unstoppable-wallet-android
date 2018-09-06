package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.DollarCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class WalletInteractor(private val adapterManager: AdapterManager, private val databaseManager: IDatabaseManager) : WalletModule.IInteractor {

    var delegate: WalletModule.IInteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()
    private var exchangeRates = mutableMapOf<String, Double>()

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

        delegate?.didInitialFetch(coinValues, exchangeRates, progresses, currency)

        adapterManager.adapters.forEach { adapter ->
            disposables.add(adapter.balanceSubject.subscribe {
                delegate?.didUpdate(CoinValue(adapter.coin, it), adapterId = adapter.id)
            })
        }

        disposables.add(databaseManager.getExchangeRates().subscribe {
            exchangeRates = it.array.associateBy({ it.code }, { it.value }).toMutableMap()
            delegate?.didUpdate(exchangeRates)
        })
    }

}
