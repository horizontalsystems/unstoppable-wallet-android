package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.marketkit.models.Token

object AddressValidatorFactory {

    // allowOwnAddress: swap recipients may name the user's own address (the default
    // delivery target anyway); the Send flow keeps the send-to-self protection.
    // zcashTransparentOnly: the selected swap route can deliver only to transparent
    // Zcash addresses (CEX providers) — shielded/unified recipients are rejected.
    fun get(
        token: Token,
        allowOwnAddress: Boolean = false,
        zcashTransparentOnly: Boolean = false,
    ): EnterAddressValidator =
        ChainRegistry[token.blockchainType]?.addressValidator(token, allowOwnAddress, zcashTransparentOnly)
            ?: throw IllegalStateException("Unsupported blockchain type: ${token.blockchainType}")

}
