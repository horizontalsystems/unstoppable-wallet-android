package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin

class EvmCoinServiceFactory(
    private val basePlatformCoin: PlatformCoin,
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager,
    private val rateManager: IRateManager
) {
    val baseCoinService = EvmCoinService(basePlatformCoin, currencyManager, rateManager)

    fun getCoinService(contractAddress: Address) = getCoinService(contractAddress.hex)

    fun getCoinService(contractAddress: String) = getPlatformCoin(contractAddress)?.let { coin ->
        EvmCoinService(coin, currencyManager, rateManager)
    }

    private fun getPlatformCoin(contractAddress: String) = when (basePlatformCoin.coinType) {
        CoinType.Ethereum -> marketKit.platformCoin(CoinType.Erc20(contractAddress))
        CoinType.BinanceSmartChain -> marketKit.platformCoin(CoinType.Bep20(contractAddress))
        else -> null
    }

}
