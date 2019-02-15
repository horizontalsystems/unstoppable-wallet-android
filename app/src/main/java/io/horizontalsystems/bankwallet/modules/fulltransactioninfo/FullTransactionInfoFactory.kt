package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class FullTransactionInfoFactory(private val networkManager: INetworkManager, private val dataProviderManager: ITransactionDataProviderManager)
    : FullTransactionInfoModule.ProviderFactory {

    override fun providerFor(coinCode: CoinCode): FullTransactionInfoModule.FullProvider {
        val baseProvider = dataProviderManager.baseProvider(coinCode)

        val provider: FullTransactionInfoModule.Provider
        val adapter: FullTransactionInfoModule.Adapter

        when {
            // BTC, BTCt
            coinCode.contains("BTC") -> {
                val providerBTC = dataProviderManager.bitcoin(baseProvider.name)

                provider = providerBTC
                adapter = FullTransactionBitcoinAdapter(providerBTC, coinCode)
            }
            // BCH, BCHt
            coinCode.contains("BCH") -> {
                val providerBCH = dataProviderManager.bitcoinCash(baseProvider.name)

                provider = providerBCH
                adapter = FullTransactionBitcoinAdapter(providerBCH, coinCode)
            }
            // ETH, ETHt
            else -> {
                val providerETH = dataProviderManager.ethereum(baseProvider.name)

                provider = providerETH
                adapter = FullTransactionEthereumAdapter(providerETH, coinCode)
            }
        }

        return FullTransactionInfoProvider(networkManager, adapter, provider)
    }
}
