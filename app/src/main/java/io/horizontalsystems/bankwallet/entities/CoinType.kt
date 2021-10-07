package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.marketkit.models.Coin
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
        is CoinType.Sol20 -> "Solana"
        else -> ""
    }

val CoinType.platformCoinType: String
    get() = when (this) {
        CoinType.Ethereum, CoinType.BinanceSmartChain -> Translator.getString(R.string.CoinPlatforms_Native)
        is CoinType.Erc20 -> "ERC20"
        is CoinType.Bep20 -> "BEP20"
        is CoinType.Bep2 -> "BEP2"
        is CoinType.Sol20 -> "SOL20"
        else -> ""
    }

val CoinType.title: String
    get() {
        return when (this) {
            is CoinType.Bitcoin -> "Bitcoin"
            is CoinType.Litecoin -> "Litecoin"
            is CoinType.BitcoinCash -> "BitcoinCash"
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
        is CoinType.Sol20,
        is CoinType.Unsupported -> false
    }
