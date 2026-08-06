package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.tonkit.core.TonKit

class AddressHandlerTon : IAddressHandler {
    override val blockchainType = BlockchainType.Ton

    override fun isSupported(value: String) = try {
        TonKit.validateAddress(value)
        true
    } catch (e: Exception) {
        false
    }

    override fun parseAddress(value: String): Address {
        return Address(value, blockchainType = blockchainType)
    }
}
