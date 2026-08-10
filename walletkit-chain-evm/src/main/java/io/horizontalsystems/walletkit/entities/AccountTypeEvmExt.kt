package io.horizontalsystems.walletkit.entities

import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Chain

fun AccountType.evmAddress(chain: Chain) = when (this) {
    is AccountType.Mnemonic -> Signer.address(seed, chain)
    is AccountType.EvmPrivateKey -> Signer.address(key)
    else -> null
}

