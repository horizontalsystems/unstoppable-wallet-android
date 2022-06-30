package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.xxxkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

sealed class EvmBlockchain(
    val uid: String,
    val name: String,
    val shortName: String,
    val description: String,
    val icon24: Int
): Parcelable {

    @Parcelize
    object Ethereum: EvmBlockchain(
        "ethereum",
        "Ethereum",
        "Ethereum",
        "ETH, ERC20 tokens",
        R.drawable.logo_ethereum_24
    )

    @Parcelize
    object BinanceSmartChain: EvmBlockchain(
        "binance-smart-chain",
        "Binance Smart Chain",
        "BSC",
        "BNB, BEP20 tokens",
        R.drawable.logo_binancesmartchain_24
    )

    @Parcelize
    object Polygon: EvmBlockchain(
        "polygon",
        "Polygon",
        "Polygon",
        "MATIC, MRC20 tokens",
        R.drawable.logo_polygon_24
    )

    @Parcelize
    object Optimism: EvmBlockchain(
        "optimism",
        "Optimism",
        "Optimism",
        "L2 chain",
        R.drawable.logo_optimism_24
    )

    @Parcelize
    object ArbitrumOne: EvmBlockchain(
        "arbitrum-one",
        "Arbitrum One",
        "Arbitrum One",
        "L2 chain",
        R.drawable.logo_arbitrum_24
    )

    val blockchainType: BlockchainType
        get() = when (this) {
            ArbitrumOne -> BlockchainType.ArbitrumOne
            BinanceSmartChain -> BlockchainType.BinanceSmartChain
            Ethereum -> BlockchainType.Ethereum
            Optimism -> BlockchainType.Optimism
            Polygon -> BlockchainType.Polygon
        }
}
