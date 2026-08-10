package io.horizontalsystems.walletkit.entities

import io.horizontalsystems.walletkit.core.managers.EvmKitManagerRegistry
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType

fun AccountType.evmAddress(chain: Chain) = when (this) {
    is AccountType.Mnemonic -> Signer.address(seed, chain)
    is AccountType.EvmPrivateKey -> Signer.address(key)
    else -> null
}

fun AccountType.sign(message: ByteArray, isLegacy: Boolean = false): ByteArray? {
    val signer = when (this) {
        is AccountType.Mnemonic -> {
            Signer.getInstance(seed, EvmKitManagerRegistry.getChain(BlockchainType.Ethereum))
        }
        is AccountType.EvmPrivateKey -> {
            Signer.getInstance(key, EvmKitManagerRegistry.getChain(BlockchainType.Ethereum))
        }
        else -> null
    } ?: return null

    return if (isLegacy) {
        signer.signByteArrayLegacy(message)
    } else {
        signer.signByteArray(message)
    }
}
