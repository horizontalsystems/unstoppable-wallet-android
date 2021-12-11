package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin

class EvmCoinServiceFactory(
    private val basePlatformCoin: PlatformCoin,
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager
) {
    val baseCoinService = EvmCoinService(basePlatformCoin, currencyManager, marketKit)

    fun getCoinService(contractAddress: Address) = getCoinService(contractAddress.hex)

    fun getCoinService(contractAddress: String) = getPlatformCoin(contractAddress)?.let { coin ->
        EvmCoinService(coin, currencyManager, marketKit)
    }

    private fun getPlatformCoin(contractAddress: String) = when (basePlatformCoin.coinType) {
        CoinType.Ethereum -> marketKit.platformCoin(CoinType.Erc20(contractAddress))
        CoinType.BinanceSmartChain -> marketKit.platformCoin(CoinType.Bep20(contractAddress))
        else -> null
    }

}
