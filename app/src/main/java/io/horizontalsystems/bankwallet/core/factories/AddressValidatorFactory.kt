package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.send.address.BitcoinAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.EvmAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.SolanaAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.StellarAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.TonAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.TronAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.ZcashAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.MoneroAddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

object AddressValidatorFactory {

    fun get(token: Token): EnterAddressValidator {
        return when (token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                BitcoinAddressValidator(token, App.adapterManager)
            }

            BlockchainType.Zcash -> {
                ZcashAddressValidator(token, App.adapterManager)
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
                TronAddressValidator(token, App.adapterManager)
            }

            BlockchainType.Ton -> {
                TonAddressValidator()
            }

            is BlockchainType.Stellar -> {
                StellarAddressValidator(token)
            }

            is BlockchainType.Monero -> {
                MoneroAddressValidator()
            }

            is BlockchainType.Unsupported -> throw IllegalStateException("Unsupported blockchain type: ${token.blockchainType}")
        }
    }

}
