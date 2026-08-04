package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.send.address.BitcoinAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.EvmAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.MoneroAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.SolanaAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.StellarAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.ThorchainAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.TonAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.TronAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.ZcashAddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

object AddressValidatorFactory {

    // allowOwnAddress: swap recipients may name the user's own address (the default
    // delivery target anyway); the Send flow keeps the send-to-self protection
    fun get(token: Token, allowOwnAddress: Boolean = false): EnterAddressValidator {
        ChainRegistry[token.blockchainType]?.addressValidator(token)?.let {
            return it
        }

        return when (token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                BitcoinAddressValidator(token, App.adapterManager)
            }

            BlockchainType.Zcash -> {
                ZcashAddressValidator(token, App.adapterManager, allowOwnAddress)
            }

            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                EvmAddressValidator()
            }

            BlockchainType.Solana -> {
                SolanaAddressValidator()
            }

            BlockchainType.Tron -> {
                TronAddressValidator(token, App.adapterManager, allowOwnAddress)
            }

            BlockchainType.Ton -> {
                TonAddressValidator()
            }

            is BlockchainType.Stellar -> {
                StellarAddressValidator(token)
            }

            is BlockchainType.Thorchain -> {
                ThorchainAddressValidator(token)
            }

            is BlockchainType.Monero -> {
                MoneroAddressValidator()
            }

            else -> throw IllegalStateException("Unsupported blockchain type: ${token.blockchainType}")
        }
    }

}
