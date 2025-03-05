package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.BitcoinCashCoinType
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.entities.ZCashCoinType
import io.horizontalsystems.core.entities.BlockchainType

val Token.badge: String?
    get() = when (val tokenType = type) {
        is TokenType.Derived -> {
            tokenType.derivation.accountTypeDerivation.value.uppercase()
        }

        is TokenType.AddressTyped -> {
            tokenType.type.bitcoinCashCoinType.value.uppercase()
        }

        is TokenType.AddressSpecTyped -> {
            tokenType.type.name.uppercase()
        }

        else -> {
            protocolType?.uppercase()
        }
    }

val TokenType.Derivation.accountTypeDerivation: Derivation
    get() = when (this) {
        TokenType.Derivation.Bip44 -> Derivation.bip44
        TokenType.Derivation.Bip49 -> Derivation.bip49
        TokenType.Derivation.Bip84 -> Derivation.bip84
        TokenType.Derivation.Bip86 -> Derivation.bip86
    }


val TokenType.AddressType.bitcoinCashCoinType: BitcoinCashCoinType
    get() = when (this) {
        TokenType.AddressType.Type0 -> BitcoinCashCoinType.type0
        TokenType.AddressType.Type145 -> BitcoinCashCoinType.type145
    }

val TokenType.AddressSpecType.zCashCoinType: ZCashCoinType
    get() = when (this) {
        TokenType.AddressSpecType.Shielded -> ZCashCoinType.Shielded
        TokenType.AddressSpecType.Transparent -> ZCashCoinType.Transparent
        TokenType.AddressSpecType.Unified -> ZCashCoinType.Unified
    }


val Token.protocolType: String?
    get() = tokenQuery.protocolType

val TokenQuery.protocolType: String?
    get() = when (tokenType) {
        is TokenType.Native -> {
            when (blockchainType) {
                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Tron,
                BlockchainType.Ton -> null

                BlockchainType.BinanceChain -> "BEP2"
                else -> blockchainType.title
            }
        }

        is TokenType.Eip20 -> {
            when (blockchainType) {
                BlockchainType.Ethereum -> "ERC20"
                BlockchainType.BinanceSmartChain -> "BEP20"
                BlockchainType.Tron -> "TRC20"
                else -> blockchainType.title
            }
        }

        is TokenType.Bep2 -> "BEP2"
        is TokenType.Jetton -> "JETTON"
        else -> blockchainType.title
    }

val BlockchainType.title: String
    get() = when (this) {
        BlockchainType.Bitcoin -> "Bitcoin"
        BlockchainType.BitcoinCash -> "Bitcoin Cash"
        BlockchainType.ECash -> "Ecash"
        BlockchainType.Litecoin -> "Litecoin"
        BlockchainType.Dash -> "Dash"
        BlockchainType.Zcash -> "Zcash"
        BlockchainType.Ethereum -> "Ethereum"
        BlockchainType.BinanceSmartChain -> "BNB Smart Chain"
        BlockchainType.Polygon -> "Polygon"
        BlockchainType.Avalanche -> "Avalanche"
        BlockchainType.ArbitrumOne -> "ArbitrumOne"
        BlockchainType.BinanceChain -> "BNB Beacon Coin"
        BlockchainType.Optimism -> "Optimism"
        BlockchainType.Base -> "Base"
        BlockchainType.ZkSync -> "ZKsync"
        BlockchainType.Solana -> "Solana"
        BlockchainType.Gnosis -> "Gnosis"
        BlockchainType.Fantom -> "Fantom"
        BlockchainType.Tron -> "Tron"
        BlockchainType.Ton -> "Ton"
        is BlockchainType.Unsupported -> this.uid
    }

val TokenQuery.Companion.customCoinPrefix: String
    get() = "custom-"

val TokenQuery.customCoinUid: String
    get() = "${TokenQuery.customCoinPrefix}${id}"

val TokenType.meta: String?
    get() = when (this) {
        is TokenType.Derived -> this.derivation.name
        is TokenType.AddressTyped -> this.type.name
        is TokenType.Bep2 -> this.symbol
        else -> null
    }

fun Wallet.isOldZCash() = token.type == TokenType.Native &&
        token.blockchainType == BlockchainType.Zcash

fun Wallet.isPirateCash() = (token.type == TokenType.Eip20(BuildConfig.PIRATE_CONTRACT) ||
        token.type == TokenType.Eip20(BuildConfig.PIRATE_CONTRACT.lowercase())) &&
        token.blockchainType == BlockchainType.BinanceSmartChain

fun Wallet.isCosanta() = (token.type == TokenType.Eip20(BuildConfig.COSANTA_CONTRACT) ||
        token.type == TokenType.Eip20(BuildConfig.COSANTA_CONTRACT.lowercase())) &&
        token.blockchainType == BlockchainType.BinanceSmartChain

fun Wallet.getUniqueKey() = listOf(token.tokenQuery.id, account.id).joinToString()