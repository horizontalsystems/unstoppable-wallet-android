package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters.FullTransactionBinanceAdapter
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters.FullTransactionBitcoinAdapter
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters.FullTransactionEosAdapter
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters.FullTransactionEthereumAdapter

class FullTransactionInfoFactory(private val networkManager: INetworkManager, private val dataProviderManager: ITransactionDataProviderManager)
    : FullTransactionInfoModule.ProviderFactory {

    override fun providerFor(wallet: Wallet): FullTransactionInfoModule.FullProvider {
        val coin = wallet.coin
        val baseProvider = dataProviderManager.baseProvider(coin)

        val provider: FullTransactionInfoModule.Provider
        val adapter: FullTransactionInfoModule.Adapter

        when {
            // BTC, BTCt
            coin.type is CoinType.Bitcoin -> {
                val providerBTC = dataProviderManager.bitcoin(baseProvider.name)

                provider = providerBTC
                adapter = FullTransactionBitcoinAdapter(providerBTC, coin, "satoshi")
            }
            coin.type is CoinType.Litecoin -> {
                val providerLTC = dataProviderManager.litecoin(baseProvider.name)

                provider = providerLTC
                adapter = FullTransactionBitcoinAdapter(providerLTC, coin, "satoshi")
            }
            // BCH, BCHt
            coin.type is CoinType.BitcoinCash -> {
                val providerBCH = dataProviderManager.bitcoinCash(baseProvider.name)

                provider = providerBCH
                adapter = FullTransactionBitcoinAdapter(providerBCH, coin, "satoshi")
            }
            // DASH, DASHt
            coin.type is CoinType.Dash -> {
                val providerDASH = dataProviderManager.dash(baseProvider.name)

                provider = providerDASH
                adapter = FullTransactionBitcoinAdapter(providerDASH, coin, "duff")
            }
            // BNB
            coin.type is CoinType.Binance -> {
                val providerBinance = dataProviderManager.binance(baseProvider.name)

                provider = providerBinance
                adapter = FullTransactionBinanceAdapter(providerBinance, App.feeCoinProvider, coin)
            }
            //EOS
            coin.type is CoinType.Eos -> {
                val providerEos = dataProviderManager.eos(baseProvider.name)

                provider = providerEos
                adapter = FullTransactionEosAdapter(providerEos, wallet)
            }
            //ZCASH
            coin.type is CoinType.Zcash -> {
                val providerZcash = dataProviderManager.zcash(baseProvider.name)

                provider = providerZcash
                adapter = FullTransactionBitcoinAdapter(providerZcash, coin, "zatoshi")
            }
            // ETH, ETHt
            else -> {
                val providerETH = dataProviderManager.ethereum(baseProvider.name)

                provider = providerETH
                adapter = FullTransactionEthereumAdapter(providerETH, App.feeCoinProvider, coin)
            }
        }

        return FullTransactionInfoProvider(networkManager, adapter, provider)
    }
}
