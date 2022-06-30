package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.CoinSettingType
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.xxxkit.models.*
import java.util.*

val Token.protocolType: String?
    get() = tokenQuery.protocolType

val Token.isCustom: Boolean
    get() = coin.uid == tokenQuery.customCoinUid

val Token.isSupported: Boolean
    get() = tokenQuery.isSupported

val Token.placeholderImageName: String
    get() = protocolType?.let { "Coin Icon Placeholder - $it" } ?: "icon_placeholder_24"

val Token.iconPlaceholder: Int
    get() = when (type) {
        is TokenType.Eip20 -> {
            when (blockchainType) {
                BlockchainType.Ethereum -> R.drawable.erc20
                BlockchainType.BinanceSmartChain -> R.drawable.bep20
                else -> R.drawable.coin_placeholder
            }
        }
        is TokenType.Bep2 -> R.drawable.bep2
        else -> R.drawable.coin_placeholder
    }

val Token.swappable: Boolean
    get() = when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne -> true
        else -> false
    }

val Token.protocolInfo: String
    get() = when (type) {
        TokenType.Native -> blockchain.name
        is TokenType.Eip20,
        is TokenType.Bep2 -> protocolType ?: ""
        else -> ""
    }

val Token.typeInfo: String
    get() = when (val type = type) {
        TokenType.Native -> Translator.getString(R.string.CoinPlatforms_Native)
        is TokenType.Eip20 -> type.address.shortenedAddress()
        is TokenType.Bep2 -> type.symbol
        is TokenType.Unsupported -> ""
    }


val TokenQuery.protocolType: String?
    get() = when (tokenType) {
        is TokenType.Eip20 -> {
            when (blockchainType) {
                BlockchainType.Ethereum -> "ERC20"
                BlockchainType.BinanceSmartChain -> "BEP20"
                BlockchainType.Polygon -> "POLYGON"
                BlockchainType.Optimism -> "OPTIMISM"
                BlockchainType.ArbitrumOne -> "ARBITRUM"
                else -> null
            }
        }
        is TokenType.Bep2 -> "BEP2"
        else -> null
    }

val TokenQuery.customCoinUid: String
    get() = "custom-$id"

val TokenQuery.isSupported: Boolean
    get() = when (blockchainType) {
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.Litecoin,
        BlockchainType.Dash,
        BlockchainType.Zcash -> {
            tokenType is TokenType.Native
        }
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon -> {
            tokenType is TokenType.Native || tokenType is TokenType.Eip20
        }
        BlockchainType.BinanceChain -> {
            tokenType is TokenType.Native || tokenType is TokenType.Bep2
        }
        else -> false
    }


val Blockchain.shortName: String
    get() = when (type) {
        BlockchainType.BinanceSmartChain -> "BSC"
        else -> name
    }

val Blockchain.description: String
    get() = when (type) {
        BlockchainType.Ethereum -> "ETH, ERC20 tokens"
        BlockchainType.BinanceSmartChain -> "BNB, BEP20 tokens"
        BlockchainType.Polygon -> "MATIC, MRC20 tokens"
        BlockchainType.Optimism -> "L2 chain"
        BlockchainType.ArbitrumOne -> "L2 chain"
        else -> ""
    }


val BlockchainType.imageUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/platform-icons/$uid@3x.png"

val BlockchainType.coinSettingTypes: List<CoinSettingType>
    get() = when (this) {
        BlockchainType.Bitcoin,
        BlockchainType.Litecoin -> listOf(CoinSettingType.derivation)
        BlockchainType.BitcoinCash -> listOf(CoinSettingType.bitcoinCashCoinType)
        else -> listOf()
    }

val BlockchainType.defaultSettingsArray: List<CoinSettings>
    get() = when (this) {
        BlockchainType.Bitcoin,
        BlockchainType.Litecoin -> listOf(CoinSettings(mapOf(CoinSettingType.derivation to AccountType.Derivation.bip49.value)))
        BlockchainType.BitcoinCash -> listOf(CoinSettings(mapOf(CoinSettingType.bitcoinCashCoinType to BitcoinCashCoinType.type145.value)))
        else -> listOf()
    }

val BlockchainType.restoreSettingTypes: List<RestoreSettingType>
    get() = when (this) {
        BlockchainType.Zcash -> listOf(RestoreSettingType.BirthdayHeight)
        else -> listOf()
    }

val BlockchainType.icon24: Int
    get() = when (this) {
        BlockchainType.Bitcoin -> R.drawable.logo_bitcoin_24
        BlockchainType.BitcoinCash -> R.drawable.logo_bitcoincash_24
        BlockchainType.Litecoin -> R.drawable.logo_litecoin_24
        BlockchainType.Dash -> R.drawable.logo_dash_24
        BlockchainType.Ethereum -> R.drawable.logo_ethereum_24
        BlockchainType.BinanceSmartChain -> R.drawable.logo_binancesmartchain_24
        BlockchainType.Polygon -> R.drawable.logo_polygon_24
        BlockchainType.Optimism -> R.drawable.logo_optimism_24
        BlockchainType.ArbitrumOne -> R.drawable.logo_arbitrum_24
        BlockchainType.Zcash -> R.drawable.logo_zcash_24
        BlockchainType.BinanceChain -> R.drawable.logo_bep2_24
        is BlockchainType.Unsupported -> R.drawable.ic_platform_placeholder_24
    }

val BlockchainType.order: Int
    get() = when (this) {
        BlockchainType.Bitcoin -> 1
        BlockchainType.BitcoinCash -> 2
        BlockchainType.Litecoin -> 3
        BlockchainType.Dash -> 4
        BlockchainType.Zcash -> 5
        BlockchainType.Ethereum -> 6
        BlockchainType.BinanceSmartChain -> 7
        BlockchainType.Polygon -> 8
        BlockchainType.Optimism -> 9
        BlockchainType.ArbitrumOne -> 10
        else -> Int.MAX_VALUE
    }


val Coin.imageUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/coin-icons/$uid@3x.png"

val TopPlatform.imageUrl
    get() = "https://markets.nyc3.digitaloceanspaces.com/platform-icons/$uid@3x.png"

val FullCoin.typeLabel: String?
    get() = tokens.singleOrNull()?.protocolType

val FullCoin.supportedTokens
    get() = tokens.filter { it.isSupported }

val FullCoin.iconPlaceholder: Int
    get() {
        return R.drawable.coin_placeholder
//        if (tokens.size == 1) {
//            tokens.first().blockchainType.iconPlaceholder
//        } else {
//            R.drawable.coin_placeholder
//        }
    }

val CoinCategory.imageUrl
    get() = "https://markets.nyc3.digitaloceanspaces.com/category-icons/$uid@3x.png"

val CoinInvestment.Fund.logoUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/fund-icons/$uid@3x.png"

val CoinTreasury.logoUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/treasury-icons/$fundUid@3x.png"

val Auditor.logoUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/auditor-icons/$name@3x.png"


fun List<FullCoin>.sortedByFilter(filter: String, enabled: (FullCoin) -> Boolean): List<FullCoin> {
    var comparator: Comparator<FullCoin> = compareByDescending {
        enabled.invoke(it)
    }
    if (filter.isNotBlank()) {
        val lowercasedFilter = filter.lowercase()
        comparator = comparator
            .thenByDescending {
                it.coin.code.lowercase() == lowercasedFilter
            }.thenByDescending {
                it.coin.code.lowercase().startsWith(lowercasedFilter)
            }.thenByDescending {
                it.coin.name.lowercase().startsWith(lowercasedFilter)
            }
    }
    comparator = comparator.thenBy {
        it.coin.marketCapRank ?: Int.MAX_VALUE
    }
    comparator = comparator.thenBy {
        it.coin.name.lowercase(Locale.ENGLISH)
    }

    return sortedWith(comparator)
}

//extension Array where Element == Token {
//
//    var sorted: [Token] {
//        sorted { $0.blockchainType.order < $1.blockchainType.order }
//    }
//
//}