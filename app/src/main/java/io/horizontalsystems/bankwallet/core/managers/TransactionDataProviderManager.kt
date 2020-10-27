package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.BitcoinForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.EthereumForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Provider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.*
import io.reactivex.subjects.PublishSubject

class TransactionDataProviderManager(
        private val testMode: Boolean,
        private val etherscanApiKey: String,
        private val localStorage: ILocalStorage
) : ITransactionDataProviderManager {

    private val bitcoinProviders: List<BitcoinForksProvider> by lazy {
        when {
            testMode -> listOf()
            else -> listOf(
                    BlockChairBitcoinProvider(),
                    BtcComBitcoinProvider())
        }
    }

    private val litecoinProviders: List<BitcoinForksProvider> by lazy {
        when {
            testMode -> listOf()
            else -> listOf(
                    HorsysLitecoinProvider(testMode),
                    BlockChairLitecoinProvider()
            )
        }
    }

    private val bitcoinCashProviders: List<BitcoinForksProvider> by lazy {
        when {
            testMode -> listOf()
            else -> listOf(
                    BlockChairBitcoinCashProvider(),
                    BtcComBitcoinCashProvider())
        }
    }

    private val ethereumProviders: List<EthereumForksProvider> by lazy {
        when {
            testMode -> listOf(
                    EtherscanProvider(testMode, etherscanApiKey))
            else -> listOf(
                    EtherscanProvider(testMode, etherscanApiKey),
                    BlockChairEthereumProvider())
        }
    }

    private val dashProviders: List<BitcoinForksProvider> by lazy {
        when {
            testMode -> listOf(HorsysDashProvider(testMode))
            else -> listOf(
                    HorsysDashProvider(testMode),
                    BlockChairDashProvider(),
                    InsightDashProvider()
            )
        }
    }

    private val binanceProviders: List<FullTransactionInfoModule.BinanceProvider> by lazy {
        when {
            testMode -> listOf(BinanceChainProvider(testMode))
            else -> listOf(BinanceChainProvider(testMode))
        }
    }

    private val eosProviders: List<FullTransactionInfoModule.EosProvider> by lazy {
        listOf(EosGreymassProvider())
    }

    private val zcashProviders: List<BitcoinForksProvider> by lazy {
        listOf(ZcashProvider())
    }

    override val baseProviderUpdatedSignal = PublishSubject.create<Unit>()

    override fun providers(coin: Coin): List<Provider> = when (coin.type) {
        is CoinType.Bitcoin -> bitcoinProviders
        is CoinType.Litecoin -> litecoinProviders
        is CoinType.BitcoinCash -> bitcoinCashProviders
        is CoinType.Ethereum, is CoinType.Erc20 -> ethereumProviders
        is CoinType.Dash -> dashProviders
        is CoinType.Binance -> binanceProviders
        is CoinType.Eos -> eosProviders
        is CoinType.Zcash -> zcashProviders
    }

    override fun baseProvider(coin: Coin) = when (coin.type) {
        is CoinType.Bitcoin, is CoinType.BitcoinCash -> {
            bitcoin(localStorage.baseBitcoinProvider ?: bitcoinProviders[0].name)
        }
        is CoinType.Litecoin -> {
            litecoin(localStorage.baseLitecoinProvider ?: litecoinProviders[0].name)
        }
        is CoinType.Ethereum, is CoinType.Erc20 -> {
            ethereum(localStorage.baseEthereumProvider ?: ethereumProviders[0].name)
        }
        is CoinType.Dash -> {
            dash(localStorage.baseDashProvider ?: dashProviders[0].name)
        }
        is CoinType.Binance -> {
            binance(localStorage.baseBinanceProvider ?: binanceProviders[0].name)
        }
        is CoinType.Eos -> {
            eos(localStorage.baseEosProvider ?: eosProviders[0].name)
        }
        is CoinType.Zcash -> {
            zcash(localStorage.baseZcashProvider ?: zcashProviders[0].name)
        }
    }

    override fun setBaseProvider(name: String, coin: Coin) {
        when (coin.type) {
            is CoinType.Bitcoin, is CoinType.BitcoinCash -> {
                localStorage.baseBitcoinProvider = name
            }
            is CoinType.Litecoin -> {
                localStorage.baseLitecoinProvider = name
            }
            is CoinType.Ethereum, is CoinType.Erc20 -> {
                localStorage.baseEthereumProvider = name
            }
            is CoinType.Dash -> {
                localStorage.baseDashProvider = name
            }
            is CoinType.Eos -> {
                localStorage.baseEosProvider = name
            }
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

    override fun litecoin(name: String): BitcoinForksProvider {
        litecoinProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun bitcoinCash(name: String): BitcoinForksProvider {
        bitcoinCashProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun dash(name: String): BitcoinForksProvider {
        dashProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun ethereum(name: String): EthereumForksProvider {
        ethereumProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun binance(name: String): FullTransactionInfoModule.BinanceProvider {
        binanceProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun eos(name: String): FullTransactionInfoModule.EosProvider {
        eosProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun zcash(name: String): BitcoinForksProvider {
        zcashProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

}
