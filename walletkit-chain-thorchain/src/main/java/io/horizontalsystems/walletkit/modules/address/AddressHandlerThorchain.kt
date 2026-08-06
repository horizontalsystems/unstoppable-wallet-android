package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.thorchainkit.models.Address as ThorchainAddress
import io.horizontalsystems.thorchainkit.network.Network as ThorchainNetwork

class AddressHandlerThorchain(
    private val network: ThorchainNetwork,
    override val blockchainType: BlockchainType,
) : IAddressHandler {

    override fun isSupported(value: String) = try {
        ThorchainAddress.fromString(value, network)
        true
    } catch (e: Exception) {
        false
    }

    override fun parseAddress(value: String): Address {
        return Address(value, blockchainType = blockchainType)
    }
}
