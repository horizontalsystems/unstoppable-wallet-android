package io.horizontalsystems.bankwallet.core

import androidx.compose.ui.graphics.Color
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.CoinSettingType
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.FeePriceScale
import io.horizontalsystems.bankwallet.entities.derivation
import io.horizontalsystems.hdwalletkit.ExtendedKeyCoinType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.marketkit.models.TopPlatform
import io.horizontalsystems.nftkit.models.NftType

val Token.protocolType: String?
    get() = tokenQuery.protocolType

val Token.isCustom: Boolean
    get() = coin.uid == tokenQuery.customCoinUid

val Coin.isCustom: Boolean
    get() = uid.startsWith(TokenQuery.customCoinPrefix)

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
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne -> true
        else -> false
    }

val Token.protocolInfo: String
    get() = when (type) {
        TokenType.Native -> {
            val parts = mutableListOf(blockchain.name)
            when (this.blockchainType) {
                BlockchainType.Ethereum -> parts.add("(ERC20)")
                BlockchainType.BinanceSmartChain -> parts.add("(BEP20)")
                BlockchainType.BinanceChain -> parts.add("(BEP2)")
                else -> {}
            }
            parts.joinToString(" ")
        }
        is TokenType.Eip20,
        is TokenType.Bep2,
        is TokenType.Spl -> protocolType ?: ""
        else -> ""
    }

val Token.typeInfo: String
    get() = when (val type = type) {
        TokenType.Native -> Translator.getString(R.string.CoinPlatforms_Native)
        is TokenType.Eip20 -> type.address.shorten()
        is TokenType.Bep2 -> type.symbol
        is TokenType.Spl -> type.address.shorten()
        is TokenType.Unsupported -> ""
    }

val Token.copyableTypeInfo: String?
    get() = when (val type = type) {
        is TokenType.Eip20 -> type.address
        is TokenType.Bep2 -> type.symbol
        is TokenType.Spl -> type.address
        else -> null
    }


val TokenQuery.protocolType: String?
    get() = when (tokenType) {
        is TokenType.Native -> {
            when (blockchainType) {
                BlockchainType.Optimism -> "Optimism"
                BlockchainType.ArbitrumOne -> "Arbitrum"
                BlockchainType.BinanceChain -> "BEP2"
                BlockchainType.Gnosis -> "Gnosis"
                BlockchainType.Fantom -> "Fantom"
                else -> null
            }
        }
        is TokenType.Eip20 -> {
            when (blockchainType) {
                BlockchainType.Ethereum -> "ERC20"
                BlockchainType.BinanceSmartChain -> "BEP20"
                BlockchainType.Tron -> "TRC20"
                BlockchainType.Polygon -> "Polygon"
                BlockchainType.Avalanche -> "Avalanche"
                BlockchainType.Optimism -> "Optimism"
                BlockchainType.ArbitrumOne -> "Arbitrum"
                BlockchainType.Gnosis -> "Gnosis"
                BlockchainType.Fantom -> "Fantom"
                else -> null
            }
        }
        is TokenType.Bep2 -> "BEP2"
        is TokenType.Spl -> "Solana"
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
        BlockchainType.ECash,
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
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.Avalanche -> {
            tokenType is TokenType.Native || tokenType is TokenType.Eip20
        }
        BlockchainType.BinanceChain -> {
            tokenType is TokenType.Native || tokenType is TokenType.Bep2
        }
        BlockchainType.Solana -> {
            tokenType is TokenType.Native || tokenType is TokenType.Spl
        }
        BlockchainType.Tron -> {
            tokenType is TokenType.Native || tokenType is TokenType.Eip20
        }
        else -> false
    }

val Blockchain.description: String
    get() = when (type) {
        BlockchainType.Bitcoin -> "BTC (BIP44, BIP49, BIP84, BIP86)"
        BlockchainType.BitcoinCash -> "BCH (Legacy, CashAddress)"
        BlockchainType.ECash -> "XEC"
        BlockchainType.Zcash -> "ZEC"
        BlockchainType.Litecoin -> "LTC (BIP44, BIP49, BIP84, BIP86)"
        BlockchainType.Dash -> "DASH"
        BlockchainType.BinanceChain -> "BNB, BEP2 tokens"
        BlockchainType.Ethereum -> "ETH, ERC20 tokens"
        BlockchainType.BinanceSmartChain -> "BNB, BEP20 tokens"
        BlockchainType.Polygon -> "MATIC, ERC20 tokens"
        BlockchainType.Avalanche -> "AVAX, ERC20 tokens"
        BlockchainType.Optimism -> "L2 chain"
        BlockchainType.ArbitrumOne -> "L2 chain"
        BlockchainType.Solana -> "SOL, SPL tokens"
        BlockchainType.Gnosis -> "xDAI, ERC20 tokens"
        BlockchainType.Fantom -> "FTM, ERC20 tokens"
        BlockchainType.Tron -> "TRX, TRC20 tokens"
        else -> ""
    }

fun Blockchain.eip20TokenUrl(address: String) = eip3091url?.replace("\$ref", address)

fun Blockchain.bep2TokenUrl(symbol: String) = "https://explorer.binance.org/asset/$symbol"

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
        when (accountType) {
            is AccountType.Mnemonic -> listOf(CoinSettings(mapOf(CoinSettingType.derivation to AccountType.Derivation.bip84.value)))
            is AccountType.HdExtendedKey -> {
                accountType.hdExtendedKey.purposes.firstOrNull()?.let { purpose ->
                    listOf(CoinSettings(mapOf(CoinSettingType.derivation to purpose.derivation.value)))
                } ?: listOf()
            }
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

private val blockchainOrderMap: Map<BlockchainType, Int> by lazy {
    val map = mutableMapOf<BlockchainType, Int>()
    listOf(
        BlockchainType.Bitcoin,
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Tron,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Zcash,
        BlockchainType.BitcoinCash,
        BlockchainType.Litecoin,
        BlockchainType.Dash,
        BlockchainType.BinanceChain,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne,
        BlockchainType.Optimism,
        BlockchainType.Solana,
        BlockchainType.ECash,
    ).forEachIndexed { index, blockchainType ->
        map[blockchainType] = index
    }
    map
}

val BlockchainType.order: Int
    get() = blockchainOrderMap[this] ?: Int.MAX_VALUE

val BlockchainType.tokenIconPlaceholder: Int
    get() = when (this) {
        BlockchainType.Ethereum -> R.drawable.erc20
        BlockchainType.BinanceSmartChain -> R.drawable.bep20
        BlockchainType.BinanceChain -> R.drawable.bep2
        BlockchainType.Avalanche -> R.drawable.avalanche_erc20
        BlockchainType.Polygon -> R.drawable.polygon_erc20
        BlockchainType.Optimism -> R.drawable.optimism_erc20
        BlockchainType.ArbitrumOne -> R.drawable.arbitrum_erc20
        BlockchainType.Gnosis -> R.drawable.gnosis_erc20
        BlockchainType.Fantom -> R.drawable.fantom_erc20
        BlockchainType.Tron -> R.drawable.tron_trc20
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

val BlockchainType.brandColor: Color?
    get() = when (this) {
        BlockchainType.Ethereum -> Color(0xFF6B7196)
        BlockchainType.BinanceSmartChain -> Color(0xFFF3BA2F)
        BlockchainType.Polygon -> Color(0xFF8247E5)
        BlockchainType.Avalanche -> Color(0xFFD74F49)
        BlockchainType.Optimism -> Color(0xFFEB3431)
        BlockchainType.ArbitrumOne -> Color(0xFF96BEDC)
        else -> null
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
            val coinTypes = accountType.hdExtendedKey.coinTypes
            when (this) {
                BlockchainType.Bitcoin -> coinTypes.contains(ExtendedKeyCoinType.Bitcoin)
                BlockchainType.Litecoin -> coinTypes.contains(ExtendedKeyCoinType.Litecoin)
                BlockchainType.BitcoinCash,
                BlockchainType.Dash,
                BlockchainType.ECash -> coinTypes.contains(ExtendedKeyCoinType.Bitcoin) && accountType.hdExtendedKey.purposes.contains(HDWallet.Purpose.BIP44)
                else -> false
            }
        }
        is AccountType.EvmAddress ->
            this == BlockchainType.Ethereum
                    || this == BlockchainType.BinanceSmartChain
                    || this == BlockchainType.Polygon
                    || this == BlockchainType.Avalanche
                    || this == BlockchainType.Optimism
                    || this == BlockchainType.ArbitrumOne
                    || this == BlockchainType.Gnosis
                    || this == BlockchainType.Fantom
        is AccountType.EvmPrivateKey -> {
            this == BlockchainType.Ethereum
                    || this == BlockchainType.BinanceSmartChain
                    || this == BlockchainType.Polygon
                    || this == BlockchainType.Avalanche
                    || this == BlockchainType.Optimism
                    || this == BlockchainType.ArbitrumOne
                    || this == BlockchainType.Gnosis
                    || this == BlockchainType.Fantom
        }
        is AccountType.SolanaAddress ->
            this == BlockchainType.Solana

        is AccountType.TronAddress ->
            this == BlockchainType.Tron

        is AccountType.Cex -> false
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
    get() = "https://cdn.blocksdecoded.com/blockchain-icons/32px/${blockchain.uid}@3x.png"

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

fun FullCoin.eligibleTokens(accountType: AccountType): List<Token> {
    return supportedTokens.filter { it.blockchainType.supports(accountType) }
}

val HsPointTimePeriod.title: Int
    get() = when(this){
        HsPointTimePeriod.Minute30 -> R.string.Coin_Analytics_Period_30m
        HsPointTimePeriod.Hour1 -> R.string.Coin_Analytics_Period_1h
        HsPointTimePeriod.Hour4 -> R.string.Coin_Analytics_Period_4h
        HsPointTimePeriod.Hour8 ->R.string.Coin_Analytics_Period_8h
        HsPointTimePeriod.Day1 -> R.string.Coin_Analytics_Period_1d
        HsPointTimePeriod.Week1 -> R.string.Coin_Analytics_Period_1w
    }