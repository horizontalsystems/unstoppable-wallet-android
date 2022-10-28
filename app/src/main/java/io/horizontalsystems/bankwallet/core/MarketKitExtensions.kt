package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.hdwalletkit.ExtendedKeyCoinType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.marketkit.models.*
import io.horizontalsystems.nftkit.models.NftType

val Token.protocolType: String?
    get() = tokenQuery.protocolType

val Token.isCustom: Boolean
    get() = coin.uid == tokenQuery.customCoinUid

val Token.isSupported: Boolean
    get() = tokenQuery.isSupported

val Token.iconPlaceholder: Int
    get() = when (type) {
        is TokenType.Eip20 -> blockchainType.tokenIconPlaceholder
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
        TokenType.Native -> {
            val parts = mutableListOf(Translator.getString(R.string.CoinPlatforms_Native))
            when (this.blockchainType) {
                BlockchainType.BinanceSmartChain -> parts.add("(BEP20)")
                BlockchainType.BinanceChain -> parts.add("(BEP2)")
                else -> {}
            }
            parts.joinToString(" ")
        }
        is TokenType.Eip20 -> type.address.shorten()
        is TokenType.Bep2 -> type.symbol
        is TokenType.Spl,
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
        is TokenType.Native -> {
            when (blockchainType) {
                BlockchainType.Optimism -> "Optimism"
                BlockchainType.ArbitrumOne -> "Arbitrum"
                BlockchainType.BinanceChain -> "BEP2"
                else -> null
            }
        }
        is TokenType.Eip20 -> {
            when (blockchainType) {
                BlockchainType.Ethereum -> "ERC20"
                BlockchainType.BinanceSmartChain -> "BEP20"
                BlockchainType.Polygon -> "Polygon"
                BlockchainType.Avalanche -> "Avalanche"
                BlockchainType.Optimism -> "Optimism"
                BlockchainType.ArbitrumOne -> "Arbitrum"
                else -> null
            }
        }
        is TokenType.Bep2 -> "BEP2"
        else -> null
    }

val TokenQuery.Companion.customCoinPrefix: String
    get() = "custom-"

val TokenQuery.customCoinUid: String
    get() = "${TokenQuery.customCoinPrefix}${id}"

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
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne,
        BlockchainType.Avalanche -> {
            tokenType is TokenType.Native || tokenType is TokenType.Eip20
        }
        BlockchainType.BinanceChain -> {
            tokenType is TokenType.Native || tokenType is TokenType.Bep2
        }
        else -> false
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
    get() = "https://cdn.blocksdecoded.com/blockchain-icons/32px/$uid@3x.png"

val BlockchainType.coinSettingType: CoinSettingType?
    get() = when (this) {
        BlockchainType.Bitcoin,
        BlockchainType.Litecoin -> CoinSettingType.derivation
        BlockchainType.BitcoinCash -> CoinSettingType.bitcoinCashCoinType
        else -> null
    }

fun BlockchainType.defaultSettingsArray(accountType: AccountType): List<CoinSettings> = when (this) {
        BlockchainType.Bitcoin,
        BlockchainType.Litecoin -> {
            when(accountType) {
                is AccountType.Mnemonic -> listOf(CoinSettings(mapOf(CoinSettingType.derivation to AccountType.Derivation.bip49.value)))
                is AccountType.HdExtendedKey -> listOf(CoinSettings(mapOf(CoinSettingType.derivation to accountType.hdExtendedKey.info.purpose.derivation.value)))
                else -> listOf()
            }
        }
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
        BlockchainType.BinanceSmartChain -> R.drawable.logo_binance_smart_chain_24
        BlockchainType.Polygon -> R.drawable.logo_polygon_24
        BlockchainType.Avalanche -> R.drawable.logo_avalanche_24
        BlockchainType.Optimism -> R.drawable.logo_optimism_24
        BlockchainType.ArbitrumOne -> R.drawable.logo_arbitrum_24
        BlockchainType.Zcash -> R.drawable.logo_zcash_24
        BlockchainType.BinanceChain -> R.drawable.logo_binancecoin_24
        BlockchainType.Solana -> R.drawable.logo_solana_24
        is BlockchainType.Unsupported -> {
            when (this._uid) {
                "fantom" -> R.drawable.logo_fantom_24
                "harmony-shard-0" -> R.drawable.logo_harmony_24
                "huobi-token" -> R.drawable.logo_huobi_token_24
                "iotex" -> R.drawable.logo_iotex_24
                "moonriver" -> R.drawable.logo_moonriver_24
                "okex-chain" -> R.drawable.logo_okex_24
                "solana" -> R.drawable.logo_solana_24
                "sora" -> R.drawable.logo_sora_24
                "tomochain" -> R.drawable.logo_tomochain_24
                "xdai" -> R.drawable.logo_xdai_24
                else -> R.drawable.ic_platform_placeholder_24
            }
        }
    }

val BlockchainType.order: Int
    get() = when (this) {
        BlockchainType.Bitcoin -> 1
        BlockchainType.Ethereum -> 2
        BlockchainType.BinanceSmartChain -> 3
        BlockchainType.Polygon -> 4
        BlockchainType.Avalanche -> 5
        BlockchainType.Zcash -> 6
        BlockchainType.Dash -> 7
        BlockchainType.BitcoinCash -> 8
        BlockchainType.Litecoin -> 9
        BlockchainType.BinanceChain -> 10
        BlockchainType.ArbitrumOne -> 11
        BlockchainType.Optimism -> 12
        else -> Int.MAX_VALUE
    }

val BlockchainType.tokenIconPlaceholder: Int
    get() = when (this) {
        BlockchainType.Ethereum -> R.drawable.erc20
        BlockchainType.BinanceSmartChain -> R.drawable.bep20
        BlockchainType.BinanceChain -> R.drawable.bep2
        BlockchainType.Avalanche -> R.drawable.avalanche_erc20
        BlockchainType.Polygon -> R.drawable.polygon_erc20
        BlockchainType.Optimism -> R.drawable.optimism_erc20
        BlockchainType.ArbitrumOne -> R.drawable.arbitrum_erc20
        else -> R.drawable.coin_placeholder
    }

val BlockchainType.supportedNftTypes: List<NftType>
    get() = when (this) {
        BlockchainType.Ethereum -> listOf(NftType.Eip721, NftType.Eip1155)
//        BlockchainType.BinanceSmartChain -> listOf(NftType.Eip721)
//        BlockchainType.Polygon -> listOf(NftType.Eip721, NftType.Eip1155)
//        BlockchainType.Avalanche -> listOf(NftType.Eip721)
//        BlockchainType.ArbitrumOne -> listOf(NftType.Eip721)
        else -> listOf()
    }

val BlockchainType.feePriceScale: FeePriceScale
    get() = when (this) {
        BlockchainType.Avalanche -> FeePriceScale.Navax
        else -> FeePriceScale.Gwei
    }

fun BlockchainType.supports(accountType: AccountType): Boolean {
    return when (accountType) {
        is AccountType.Mnemonic -> true
        is AccountType.HdExtendedKey -> {
            val info = accountType.hdExtendedKey.info
            when (this) {
                BlockchainType.Bitcoin -> info.coinType == ExtendedKeyCoinType.Bitcoin
                BlockchainType.Litecoin -> info.coinType == ExtendedKeyCoinType.Litecoin && (info.purpose == HDWallet.Purpose.BIP44 || info.purpose == HDWallet.Purpose.BIP49)
                        || info.coinType == ExtendedKeyCoinType.Bitcoin && (info.purpose == HDWallet.Purpose.BIP44 || info.purpose == HDWallet.Purpose.BIP49 || info.purpose == HDWallet.Purpose.BIP84)
                BlockchainType.BitcoinCash -> info.coinType == ExtendedKeyCoinType.Bitcoin && info.purpose == HDWallet.Purpose.BIP44
                BlockchainType.Dash -> info.coinType == ExtendedKeyCoinType.Bitcoin && info.purpose == HDWallet.Purpose.BIP44
                else -> false
            }
        }
        is AccountType.EvmAddress,
        is AccountType.EvmPrivateKey -> {
            this == BlockchainType.Ethereum
                    || this == BlockchainType.BinanceSmartChain
                    || this == BlockchainType.Polygon
                    || this == BlockchainType.Avalanche
                    || this == BlockchainType.Optimism
                    || this == BlockchainType.ArbitrumOne
        }
        else -> false
    }
}

val TokenType.order: Int
    get() = when (this) {
        TokenType.Native -> 0
        else -> Int.MAX_VALUE
    }


val Coin.imageUrl: String
    get() = "https://cdn.blocksdecoded.com/coin-icons/32px/$uid@3x.png"

val TopPlatform.imageUrl
    get() = "https://cdn.blocksdecoded.com/blockchain-icons/32px/$uid@3x.png"

val FullCoin.typeLabel: String?
    get() = tokens.singleOrNull()?.protocolType

val FullCoin.supportedTokens
    get() = tokens
        .filter { it.isSupported }
        .sortedWith(compareBy({ it.type.order }, { it.blockchain.type.order }))

val FullCoin.iconPlaceholder: Int
    get() = if (tokens.size == 1) {
        tokens.first().iconPlaceholder
    } else {
        R.drawable.coin_placeholder
    }

fun FullCoin.eligibleTokens(accountType: AccountType) : List<Token> {
    return supportedTokens.filter { it.blockchainType.supports(accountType) }
}