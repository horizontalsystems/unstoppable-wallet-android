package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.HorsysBitcoinCashProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.HorsysBitcoinProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.HorsysEthereumProvider
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class FullTransactionInfoFactory(private val networkManager: INetworkManager, private val appConfig: IAppConfigProvider, private val providerMap: FullTransactionInfoModule.ProvidersMap)
    : FullTransactionInfoModule.ProviderFactory {

    override fun providerFor(coinCode: CoinCode): FullTransactionInfoModule.FullProvider {
        val providerName = "HorizontalSystems.xyz"

        val provider: FullTransactionInfoModule.Provider
        val adapter: FullTransactionInfoModule.Adapter

        when {
            // BTC, BTCt
            coinCode.contains("BTC") -> {
                val providerBTC = if (appConfig.testMode)
                    HorsysBitcoinProvider(testMode = true)
                else
                    providerMap.bitcoin(providerName)

                provider = providerBTC
                adapter = FullTransactionBitcoinAdapter(providerBTC, coinCode)
            }
            // BCH, BCHt
            coinCode.contains("BCH") -> {
                val providerBCH = if (appConfig.testMode)
                    HorsysBitcoinCashProvider(testMode = true)
                else
                    providerMap.bitcoinCash(providerName)

                provider = providerBCH
                adapter = FullTransactionBitcoinAdapter(providerBCH, coinCode)
            }
            // ETH, ETHt
            else -> {
                val providerETH = if (appConfig.testMode)
                    HorsysEthereumProvider(testMode = true)
                else
                    providerMap.ethereum(providerName)

                provider = providerETH
                adapter = FullTransactionEthereumAdapter(providerETH, coinCode)
            }
        }

        return FullTransactionInfoProvider(networkManager, adapter, provider)
    }
}
