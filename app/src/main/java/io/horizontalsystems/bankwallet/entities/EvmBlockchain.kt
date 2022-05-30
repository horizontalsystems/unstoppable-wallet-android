package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformType
import kotlinx.parcelize.Parcelize

sealed class EvmBlockchain(
    val uid: String,
    val name: String,
    val shortName: String,
    val description: String,
    val icon24: Int,
    val baseCoinType: CoinType
): Parcelable {

    @Parcelize
    object Ethereum: EvmBlockchain("ethereum", "Ethereum", "Ethereum", "ETH, ERC20 tokens", R.drawable.logo_ethereum_24, CoinType.Ethereum)

    @Parcelize
    object BinanceSmartChain: EvmBlockchain("binance-smart-chain", "Binance Smart Chain", "BSC", "BNB, BEP20 tokens", R.drawable.logo_binancesmartchain_24, CoinType.BinanceSmartChain)

    @Parcelize
    object Polygon: EvmBlockchain("polygon", "Polygon", "Polygon", "MATIC, MRC20 tokens", R.drawable.logo_polygon_24, CoinType.Polygon)

    @Parcelize
    object Optimism: EvmBlockchain("optimism", "Optimism", "Optimism", "L2 chain", R.drawable.logo_optimism_24, CoinType.EthereumOptimism)

    @Parcelize
    object ArbitrumOne: EvmBlockchain("arbitrum-one", "Arbitrum One", "Arbitrum One", "L2 chain", R.drawable.logo_arbitrum_24, CoinType.EthereumArbitrumOne)

    val platformType: PlatformType
        get() = when (this) {
            ArbitrumOne -> PlatformType.ArbitrumOne
            BinanceSmartChain -> PlatformType.BinanceSmartChain
            Ethereum -> PlatformType.Ethereum
            Optimism -> PlatformType.Optimism
            Polygon -> PlatformType.Polygon
        }

    fun getEvm20CoinType(address: String) =
        when (this) {
            Ethereum -> CoinType.Erc20(address)
            BinanceSmartChain -> CoinType.Bep20(address)
            Polygon -> CoinType.Mrc20(address)
            Optimism -> CoinType.OptimismErc20(address)
            ArbitrumOne -> CoinType.ArbitrumOneErc20(address)
        }

    fun supports(coinType: CoinType) : Boolean {
        return when (this) {
            Ethereum -> (coinType == CoinType.Ethereum || coinType is CoinType.Erc20)
            BinanceSmartChain -> (coinType == CoinType.BinanceSmartChain || coinType is CoinType.Bep20)
            Polygon -> (coinType == CoinType.Polygon || coinType is CoinType.Mrc20)
            Optimism -> (coinType == CoinType.EthereumOptimism || coinType is CoinType.OptimismErc20)
            ArbitrumOne -> (coinType == CoinType.EthereumArbitrumOne || coinType is CoinType.ArbitrumOneErc20)
        }
    }

}
