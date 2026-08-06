package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.stellarkit.StellarKit

class AddressHandlerStellar : IAddressHandler {
    override val blockchainType = BlockchainType.Stellar

    override fun isSupported(value: String) = try {
        StellarKit.validateAddress(value)
        true
    } catch (e: Exception) {
        false
    }

    override fun parseAddress(value: String): Address {
        return Address(value, blockchainType = blockchainType)
    }
}
