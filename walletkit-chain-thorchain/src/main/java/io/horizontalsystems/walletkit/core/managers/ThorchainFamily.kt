package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.thorchainkit.network.Network

// THORChain and MayaChain are served by the same kit; these map the app's BlockchainType to the
// kit's per-chain Network and to the matching secured-asset TokenType. Native RUNE/CACAO is
// TokenType.Native; other denoms are ThorchainAsset / MayachainAsset respectively.

fun BlockchainType.thorchainNetwork(): Network = when (this) {
    BlockchainType.Mayachain -> Network.MayaMainnet
    else -> Network.Mainnet
}

// Maya support is CACAO-only for now: the native coin is TokenType.Native and Maya has no
// secured-asset tokens enabled, so every non-native denom here is a THORChain asset. Maya
// secured assets would additionally need TokenType.MayachainAsset in the published marketkit.
fun thorchainAssetType(blockchainType: BlockchainType, denom: String): TokenType =
    TokenType.ThorchainAsset(denom)
