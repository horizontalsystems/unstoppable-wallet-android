package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.IExchangeRateProvider
import bitcoin.wallet.core.IUnspentOutputProvider
import bitcoin.wallet.core.subscribeAsync
import bitcoin.wallet.entities.Bitcoin
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.WalletBalanceItem
import io.reactivex.disposables.CompositeDisposable

class WalletInteractor(unspentOutputProvider: IUnspentOutputProvider, exchangeRateProvider: IExchangeRateProvider) : WalletModule.IInteractor {

    var delegate: WalletModule.IInteractorDelegate? = null
    private var exchangeRate = exchangeRateProvider.getExchangeRateForCoin(Bitcoin())
    private var unspentOutputs = unspentOutputProvider.unspentOutputs

    init {
        unspentOutputProvider.subject.subscribeAsync(CompositeDisposable(), {
            unspentOutputs = it
            notifyWalletBalances()
        })

        exchangeRateProvider.subject.subscribeAsync(CompositeDisposable(), { rates ->
            rates[Bitcoin()]?.let {
                exchangeRate = it
                notifyWalletBalances()
            }
        })
    }


    override fun notifyWalletBalances() {
        var totalValue = 0.0
        unspentOutputs.forEach {
            totalValue += it.value / 100000000.0
        }
        val bitcoin = Bitcoin()
        val walletBalanceItem = WalletBalanceItem(CoinValue(bitcoin, totalValue), exchangeRate, DollarCurrency())
        delegate?.didFetchWalletBalances(listOf(walletBalanceItem))
    }

}
