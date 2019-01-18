package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.*
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.subjects.PublishSubject

class TransactionDataProviderManager(val localStorage: ILocalStorage) : ITransactionDataProviderManager {

    override val baseProviderUpdatedSignal = PublishSubject.create<Unit>()

    //
    // For interactor
    //
    override fun providers(coinCode: CoinCode): List<FullTransactionInfoModule.Provider> {
        return when {
            coinCode.contains("BTC") -> bitcoinProviders
            coinCode.contains("BCH") -> bitcoinCashProviders
            else -> ethereumProviders
        }
    }

    override fun baseProvider(coinCode: CoinCode): FullTransactionInfoModule.Provider {
        if (coinCode.contains("ETH")) {
            return ethereum(localStorage.baseEthereumProvider ?: ethereumProviders[0].name)
        }

        return bitcoin(localStorage.baseBitcoinProvider ?: bitcoinProviders[0].name)
    }

    override fun setBaseProvider(name: String, coinCode: CoinCode) {
        if (coinCode.contains("ETH")) {
            localStorage.baseEthereumProvider = name
        } else {
            localStorage.baseBitcoinProvider = name
        }

        baseProviderUpdatedSignal.onNext(Unit)
    }

    //
    // Providers
    //
    override fun bitcoin(name: String): FullTransactionInfoModule.BitcoinForksProvider {
        return bitcoinProviders.find { it.name == name }
                ?: HorsysBitcoinProvider(testMode = false)
    }

    override fun bitcoinCash(name: String): FullTransactionInfoModule.BitcoinForksProvider {
        return bitcoinCashProviders.find { it.name == name }
                ?: HorsysBitcoinCashProvider(testMode = false)
    }

    override fun ethereum(name: String): FullTransactionInfoModule.EthereumForksProvider {
        return ethereumProviders.find { it.name == name }
                ?: HorsysEthereumProvider(testMode = false)
    }

    companion object {
        val bitcoinProviders = arrayListOf(
                HorsysBitcoinProvider(testMode = false),
                BlockChairBitcoinProvider(),
                BlockExplorerBitcoinProvider(),
                BtcComBitcoinProvider()
        )

        val bitcoinCashProviders = arrayListOf(
                HorsysBitcoinCashProvider(testMode = false),
                BlockChairBitcoinCashProvider(),
                BlockExplorerBitcoinCashProvider(),
                BtcComBitcoinCashProvider()
        )

        val ethereumProviders = arrayListOf(
                HorsysEthereumProvider(testMode = false),
                EtherscanEthereumProvider(),
                BlockChairEthereumProvider()
        )
    }
}
