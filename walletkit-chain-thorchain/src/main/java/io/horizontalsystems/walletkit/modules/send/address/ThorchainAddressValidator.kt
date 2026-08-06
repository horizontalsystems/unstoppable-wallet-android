package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendThorchainAdapter
import io.horizontalsystems.walletkit.core.managers.thorchainNetwork
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.marketkit.models.Token

class ThorchainAddressValidator(private val token: Token) : EnterAddressValidator {
    private val sendAdapter by lazy { App.adapterManager.getAdapterForToken<ISendThorchainAdapter>(token) }
    override suspend fun validate(address: Address) {
        val adapter = sendAdapter
        if (adapter != null) {
            adapter.validate(address.hex)
        } else {
            // no enabled wallet (external swap recipient) — static parse against the chain's network
            io.horizontalsystems.thorchainkit.models.Address.fromString(address.hex, token.blockchainType.thorchainNetwork())
        }
    }
}
