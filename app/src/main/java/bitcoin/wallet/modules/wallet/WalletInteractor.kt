package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.Bitcoin
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.WalletBalanceItem

class WalletInteractor(private val databaseManager: IDatabaseManager) : WalletModule.IInteractor {

    var delegate: WalletModule.IInteractorDelegate? = null

    private var exchangeRates = mutableMapOf<String, Double>()
    private var totalValues = mutableMapOf<String, Double>()

    override fun notifyWalletBalances() {
        databaseManager.getUnspentOutputs().subscribe {
            totalValues[Bitcoin().code] = it.array.map { it.value }.sum() / 100000000.0

            refresh()
        }

        databaseManager.getExchangeRates().subscribe {
            exchangeRates = it.array.associateBy({ it.code }, { it.value }).toMutableMap()

            refresh()
        }
    }

    private fun refresh() {
        val walletBalances = exchangeRates.mapNotNull {
            totalValues[it.key]?.let { totalValue ->
                WalletBalanceItem(CoinValue(Bitcoin(), totalValue), it.value, DollarCurrency())
            }
        }

        if (walletBalances.isNotEmpty()) {
            delegate?.didFetchWalletBalances(walletBalances)
        }
    }

}
