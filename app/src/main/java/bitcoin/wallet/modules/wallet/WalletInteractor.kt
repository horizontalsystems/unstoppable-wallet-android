package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.subscribeAsync
import bitcoin.wallet.entities.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WalletInteractor(databaseManager: IDatabaseManager, unspentOutputUpdateSubject: PublishSubject<List<UnspentOutput>>, exchangeRateUpdateSubject: PublishSubject<List<ExchangeRate>>) : WalletModule.IInteractor {

    var delegate: WalletModule.IInteractorDelegate? = null
    private var exchangeRates = databaseManager.getExchangeRates()
    private var unspentOutputs = databaseManager.getUnspentOutputs()

    init {
        unspentOutputUpdateSubject.subscribeAsync(CompositeDisposable(), {
            unspentOutputs = it
            notifyWalletBalances()
        })

        exchangeRateUpdateSubject.subscribeAsync(CompositeDisposable(), {
            exchangeRates = it
            notifyWalletBalances()
        })
    }


    override fun notifyWalletBalances() {
        var totalValue = 0.0

        unspentOutputs.forEach {
            totalValue += it.value / 100000000.0
        }

        val bitcoin = Bitcoin()

        exchangeRates.firstOrNull { it.code == bitcoin.code }?.let {
            val walletBalanceItem = WalletBalanceItem(CoinValue(bitcoin, totalValue), it.value, DollarCurrency())
            delegate?.didFetchWalletBalances(listOf(walletBalanceItem))
        }
    }

}
