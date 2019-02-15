package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.BitcoinForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.EthereumForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Provider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.*
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.subjects.PublishSubject

class TransactionDataProviderManager(private val appConfig: IAppConfigProvider, private val localStorage: ILocalStorage)
    : ITransactionDataProviderManager {

    private val bitcoinProviders
        get() = when {
            appConfig.testMode -> arrayListOf(HorsysBitcoinProvider(testMode = true))
            else -> arrayListOf(
                    HorsysBitcoinProvider(testMode = false),
                    BlockChairBitcoinProvider(),
                    BlockExplorerBitcoinProvider(),
                    BtcComBitcoinProvider())
        }

    private val bitcoinCashProviders
        get() = when {
            appConfig.testMode -> arrayListOf(HorsysBitcoinCashProvider(testMode = true))
            else -> arrayListOf(
                    HorsysBitcoinCashProvider(testMode = false),
                    BlockChairBitcoinCashProvider(),
                    BlockExplorerBitcoinCashProvider(),
                    BtcComBitcoinCashProvider())
        }

    private val ethereumProviders
        get() = when {
            appConfig.testMode -> arrayListOf(HorsysEthereumProvider(testMode = true))
            else -> arrayListOf(
                    HorsysEthereumProvider(testMode = false),
                    EtherscanEthereumProvider(),
                    BlockChairEthereumProvider())
        }

    override val baseProviderUpdatedSignal = PublishSubject.create<Unit>()

    override fun providers(coinCode: CoinCode): List<Provider> {
        return when {
            coinCode.contains("BTC") -> bitcoinProviders
            coinCode.contains("BCH") -> bitcoinCashProviders
            else -> ethereumProviders
        }
    }

    override fun baseProvider(coinCode: CoinCode): Provider {
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
    override fun bitcoin(name: String): BitcoinForksProvider {
        bitcoinProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun bitcoinCash(name: String): BitcoinForksProvider {
        bitcoinCashProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun ethereum(name: String): EthereumForksProvider {
        ethereumProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }
}
