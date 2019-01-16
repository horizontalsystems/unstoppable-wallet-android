package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule

class ProvidersMap : FullTransactionInfoModule.ProvidersMap {

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
