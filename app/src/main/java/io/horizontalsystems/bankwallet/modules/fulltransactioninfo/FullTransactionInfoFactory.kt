package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.HorsysBitcoinCashProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.HorsysBitcoinProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.HorsysEthereumProvider
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class FullTransactionInfoFactory(private val networkManager: INetworkManager, private val appConfig: IAppConfigProvider, private val dataProviderManager: ITransactionDataProviderManager)
    : FullTransactionInfoModule.ProviderFactory {

    override fun providerFor(coinCode: CoinCode): FullTransactionInfoModule.FullProvider {
        val providerName = dataProviderManager.baseProvider(coinCode).name

        val provider: FullTransactionInfoModule.Provider
        val adapter: FullTransactionInfoModule.Adapter

        when {
            // BTC, BTCt
            coinCode.contains("BTC") -> {
                val providerBTC = if (appConfig.testMode)
                    HorsysBitcoinProvider(testMode = true)
                else
                    dataProviderManager.bitcoin(providerName)

                provider = providerBTC
                adapter = FullTransactionBitcoinAdapter(providerBTC, coinCode)
            }
            // BCH, BCHt
            coinCode.contains("BCH") -> {
                val providerBCH = if (appConfig.testMode)
                    HorsysBitcoinCashProvider(testMode = true)
                else
                    dataProviderManager.bitcoinCash(providerName)

                provider = providerBCH
                adapter = FullTransactionBitcoinAdapter(providerBCH, coinCode)
            }
            // ETH, ETHt
            else -> {
                val providerETH = if (appConfig.testMode)
                    HorsysEthereumProvider(testMode = true)
                else
                    dataProviderManager.ethereum(providerName)

                provider = providerETH
                adapter = FullTransactionEthereumAdapter(providerETH, coinCode)
            }
        }

        return FullTransactionInfoProvider(networkManager, adapter, provider)
    }
}
