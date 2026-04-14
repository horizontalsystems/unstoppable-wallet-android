package com.quantum.wallet.bankwallet.core.factories

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.modules.send.address.BitcoinAddressValidator
import com.quantum.wallet.bankwallet.modules.send.address.EnterAddressValidator
import com.quantum.wallet.bankwallet.modules.send.address.EvmAddressValidator
import com.quantum.wallet.bankwallet.modules.send.address.SolanaAddressValidator
import com.quantum.wallet.bankwallet.modules.send.address.StellarAddressValidator
import com.quantum.wallet.bankwallet.modules.send.address.TonAddressValidator
import com.quantum.wallet.bankwallet.modules.send.address.TronAddressValidator
import com.quantum.wallet.bankwallet.modules.send.address.ZcashAddressValidator
import com.quantum.wallet.bankwallet.modules.send.address.MoneroAddressValidator
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
