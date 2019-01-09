package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.*
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class FullTransactionInfoFactory(private val networkManager: INetworkManager, private val appConfig: IAppConfigProvider)
    : FullTransactionInfoModule.ProviderFactory {

    override fun providerFor(coinCode: CoinCode): FullTransactionInfoModule.Provider {

        val adapter: FullTransactionInfoModule.Adapter
        val provider: FullTransactionInfoModule.Provider
        val explorer = if (coinCode.contains("ETH"))
            "BlockChair.com" else
            "HorizontalSystems.xyz"


        if (coinCode.contains("BTC")) {

            adapter = FullTransactionBitcoinAdapter(coinCode)
            provider = if (explorer == "BlockChair.com" && !appConfig.testMode) {
                BlockchairBitcoinProvider(explorer, coinCode, networkManager, adapter)
            } else if (explorer == "Btc.com" && !appConfig.testMode) {
                BtcComProvider(explorer, coinCode, networkManager, adapter)
            } else if (explorer == "BlockExplorer.com") {
                BlockexplorerBitcoinProvider(explorer, coinCode, networkManager, adapter)
            } else {
                HorsysBitcoinProvider(explorer, coinCode, networkManager, adapter)
            }

        } else if (coinCode.contains("BCH")) {

            adapter = FullTransactionBitcoinAdapter(coinCode)
            provider = if (explorer == "BlockChair.com" && !appConfig.testMode) {
                BlockchairBitcoinProvider(explorer, coinCode, networkManager, adapter)
            } else if (explorer == "BlockExplorer.com" && !appConfig.testMode) {
                BlockexplorerBitcoinProvider(explorer, coinCode, networkManager, adapter)
            } else if (explorer == "Btc.com" && !appConfig.testMode) {
                BtcComProvider(explorer, coinCode, networkManager, adapter)
            } else {
                HorsysBitcoinProvider(explorer, coinCode, networkManager, adapter)
            }

        } else {

            adapter = FullTransactionEthereumAdapter(coinCode)
            provider = if (explorer == "BlockChair.com" && !appConfig.testMode) {
                BlockchairEthProvider(explorer, coinCode, networkManager, adapter)
            } else if (explorer == "Etherscan.io") {
                EtherscanProvider(explorer, coinCode, networkManager, adapter)
            } else {
                HorsysEthProvider(explorer, coinCode, networkManager, adapter)
            }

        }

        return provider
    }
}
