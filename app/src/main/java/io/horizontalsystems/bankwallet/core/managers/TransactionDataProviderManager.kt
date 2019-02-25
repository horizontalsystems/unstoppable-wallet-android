package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.BitcoinForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.EthereumForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Provider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.*
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

    override fun providers(coin: Coin): List<Provider> {
        return when {
            coin.type is CoinType.Bitcoin -> bitcoinProviders
            coin.type is CoinType.BitcoinCash -> bitcoinCashProviders
            else -> ethereumProviders
        }
    }

    override fun baseProvider(coin: Coin): Provider {
        if (coin.type is CoinType.Ethereum || coin.type is CoinType.Erc20) {
            return ethereum(localStorage.baseEthereumProvider ?: ethereumProviders[0].name)
        }

        return bitcoin(localStorage.baseBitcoinProvider ?: bitcoinProviders[0].name)
    }

    override fun setBaseProvider(name: String, coin: Coin) {
        if (coin.type is CoinType.Ethereum || coin.type is CoinType.Erc20) {
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
