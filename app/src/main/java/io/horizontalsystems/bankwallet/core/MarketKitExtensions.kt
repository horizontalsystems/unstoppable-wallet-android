package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.CoinSettingType
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.marketkit.models.*

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
        BlockchainType.Avalanche,
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

val Token.copyableTypeInfo: String?
    get() = when (val type = type) {
        is TokenType.Eip20 -> type.address
        is TokenType.Bep2 -> type.symbol
        else -> null
    }


val TokenQuery.protocolType: String?
    get() = when (tokenType) {
        is TokenType.Eip20 -> {
            when (blockchainType) {
                BlockchainType.Ethereum -> "ERC20"
                BlockchainType.BinanceSmartChain -> "BEP20"
                BlockchainType.Polygon -> "POLYGON"
                BlockchainType.Avalanche -> "AVALANCHE"
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
        BlockchainType.Polygon,
        BlockchainType.Avalanche -> {
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
        BlockchainType.Bitcoin -> "BTC (BIP44, BIP49, BIP84)"
        BlockchainType.BitcoinCash -> "BCH (Legacy, CashAddress)"
        BlockchainType.Zcash -> "ZEC"
        BlockchainType.Litecoin -> "LTC (BIP44, BIP49, BIP84)"
        BlockchainType.Dash -> "DASH"
        BlockchainType.BinanceChain -> "BNB, BEP2 tokens"
        BlockchainType.Ethereum -> "ETH, ERC20 tokens"
        BlockchainType.BinanceSmartChain -> "BNB, BEP20 tokens"
        BlockchainType.Polygon -> "MATIC, ERC20 tokens"
        BlockchainType.Avalanche -> "AVAX, ERC20 tokens"
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
        BlockchainType.Avalanche -> R.drawable.logo_avalanche_24
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
        BlockchainType.Avalanche -> 9
        BlockchainType.Optimism -> 10
        BlockchainType.ArbitrumOne -> 11
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
    get() = if (tokens.size == 1) {
        tokens.first().iconPlaceholder
    } else {
        R.drawable.coin_placeholder
    }
