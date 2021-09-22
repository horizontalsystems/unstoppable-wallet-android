package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.coinkit.models.CoinType as CoinKitCoinType

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
        CoinType.Ethereum, CoinType.BinanceSmartChain -> "Native"
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

val CoinType.coinType: CoinKitCoinType
    get() = when (this) {
        CoinType.Bitcoin -> CoinKitCoinType.Bitcoin
        CoinType.BitcoinCash -> CoinKitCoinType.BitcoinCash
        CoinType.Litecoin -> CoinKitCoinType.Litecoin
        CoinType.Dash -> CoinKitCoinType.Dash
        CoinType.Zcash -> CoinKitCoinType.Zcash
        CoinType.Ethereum -> CoinKitCoinType.Ethereum
        CoinType.BinanceSmartChain -> CoinKitCoinType.BinanceSmartChain
        is CoinType.Erc20 -> CoinKitCoinType.Erc20(address)
        is CoinType.Bep20 -> CoinKitCoinType.Bep20(address)
        is CoinType.Bep2 -> CoinKitCoinType.Bep2(symbol)
        is CoinType.Sol20 -> CoinKitCoinType.Unsupported(address)
        is CoinType.Unsupported -> CoinKitCoinType.Unsupported(type)
    }

val Coin.imageUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/coin-icons/ios/${uid}@2x.png"

val CoinKitCoinType.coinType: CoinType
    get() = when (this) {
        CoinKitCoinType.Bitcoin -> CoinType.Bitcoin
        CoinKitCoinType.Litecoin -> CoinType.Litecoin
        CoinKitCoinType.BitcoinCash -> CoinType.BitcoinCash
        CoinKitCoinType.Dash -> CoinType.Dash
        CoinKitCoinType.Ethereum -> CoinType.Ethereum
        CoinKitCoinType.BinanceSmartChain -> CoinType.BinanceSmartChain
        CoinKitCoinType.Zcash -> CoinType.Zcash
        is CoinKitCoinType.Erc20 -> CoinType.Erc20(address)
        is CoinKitCoinType.Bep2 -> CoinType.Bep2(symbol)
        is CoinKitCoinType.Bep20 -> CoinType.Bep20(address)
        is CoinKitCoinType.Unsupported -> CoinType.Unsupported(id)
    }
