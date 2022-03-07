package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.marketkit.models.CoinType

val CoinType.blockchainType: String?
    get() {
        return when (this) {
            is CoinType.Erc20 -> "ERC20"
            is CoinType.Bep20 -> "BEP20"
            is CoinType.Bep2 -> "BEP2"
            else -> null
        }
    }

val CoinType.platformType: String
    get() = when (this) {
        CoinType.Ethereum, is CoinType.Erc20 -> "Ethereum"
        CoinType.BinanceSmartChain, is CoinType.Bep20 -> "Binance Smart Chain"
        is CoinType.Bep2 -> "Binance"
        else -> ""
    }

val CoinType.platformCoinType: String
    get() = when (this) {
        CoinType.Ethereum, CoinType.BinanceSmartChain -> Translator.getString(R.string.CoinPlatforms_Native)
        is CoinType.Erc20 -> "ERC20"
        is CoinType.Bep20 -> "BEP20"
        is CoinType.Bep2 -> "BEP2"
        else -> ""
    }

val CoinType.title: String
    get() {
        return when (this) {
            is CoinType.Bitcoin -> "Bitcoin"
            is CoinType.Litecoin -> "Litecoin"
            is CoinType.BitcoinCash -> "Bitcoin Cash"
            is CoinType.Dash -> "Dash"
            else -> ""
        }
    }

val CoinType.label: String?
    get() = when (this) {
        is CoinType.Erc20 -> "ERC20"
        is CoinType.Bep20 -> "BEP20"
        is CoinType.Bep2 -> "BEP2"
        else -> null
    }

val CoinType.swappable: Boolean
    get() = this is CoinType.Ethereum || this is CoinType.Erc20 || this is CoinType.BinanceSmartChain || this is CoinType.Bep20

val CoinType.coinSettingTypes: List<CoinSettingType>
    get() = when (this) {
        CoinType.Bitcoin,
        CoinType.Litecoin -> listOf(CoinSettingType.derivation)
        CoinType.BitcoinCash -> listOf(CoinSettingType.bitcoinCashCoinType)
        else -> listOf()
    }

val CoinType.defaultSettingsArray: List<CoinSettings>
    get() = when (this) {
        CoinType.Bitcoin,
        CoinType.Litecoin -> listOf(CoinSettings(mapOf(CoinSettingType.derivation to AccountType.Derivation.bip49.value)))
        CoinType.BitcoinCash -> listOf(CoinSettings(mapOf(CoinSettingType.bitcoinCashCoinType to BitcoinCashCoinType.type145.value)))
        else -> listOf()
    }

val CoinType.restoreSettingTypes: List<RestoreSettingType>
    get() = when (this) {
        CoinType.Zcash -> listOf(RestoreSettingType.BirthdayHeight)
        else -> listOf()
    }

val CoinType.isSupported: Boolean
    get() = when (this) {
        CoinType.Bitcoin,
        CoinType.BitcoinCash,
        CoinType.Litecoin,
        CoinType.Dash,
        CoinType.Zcash,
        CoinType.Ethereum,
        CoinType.BinanceSmartChain,
        is CoinType.Erc20,
        is CoinType.Bep20,
        is CoinType.Bep2 -> true
        is CoinType.ArbitrumOne,
        is CoinType.Avalanche,
        is CoinType.Fantom,
        is CoinType.HarmonyShard0,
        is CoinType.HuobiToken,
        is CoinType.Iotex,
        is CoinType.Moonriver,
        is CoinType.OkexChain,
        is CoinType.Polygon, is CoinType.Mrc20, //todo add Polygon support
        is CoinType.Solana,
        is CoinType.Sora,
        is CoinType.Tomochain,
        is CoinType.Xdai,
        is CoinType.Unsupported -> false
    }

val CoinType.order: Int
    get() = when (this) {
        is CoinType.Bitcoin -> 1
        is CoinType.BitcoinCash -> 2
        is CoinType.Litecoin -> 3
        is CoinType.Dash -> 4
        is CoinType.Zcash -> 5
        is CoinType.Ethereum -> 6
        is CoinType.BinanceSmartChain -> 7
        is CoinType.Polygon -> 8
        is CoinType.Erc20 -> 9
        is CoinType.Bep20 -> 10
        is CoinType.Mrc20 -> 11
        is CoinType.Bep2 -> 12
        is CoinType.Solana -> 13
        is CoinType.Avalanche -> 14
        is CoinType.Fantom -> 15
        is CoinType.ArbitrumOne -> 16
        is CoinType.HuobiToken -> 17
        is CoinType.HarmonyShard0 -> 18
        is CoinType.Xdai -> 19
        is CoinType.Moonriver -> 20
        is CoinType.OkexChain -> 21
        is CoinType.Sora -> 22
        is CoinType.Tomochain -> 23
        is CoinType.Iotex -> 24
        else -> Int.MAX_VALUE
    }
