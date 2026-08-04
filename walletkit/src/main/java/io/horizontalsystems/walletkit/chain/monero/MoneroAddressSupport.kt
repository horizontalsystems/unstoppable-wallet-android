package io.horizontalsystems.walletkit.chain.monero

import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.MoneroWatchAddress
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.watchaddress.MoneroUriParser
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.monerokit.MoneroKit

class AddressHandlerMonero : IAddressHandler {
    override val blockchainType = BlockchainType.Monero

    override fun isSupported(value: String) = try {
        val uriInfo = MoneroUriParser.parse(value)
        val address = uriInfo?.address ?: value
        MoneroKit.validateAddress(address)
        true
    } catch (_: Exception) {
        false
    }

    override fun parseAddress(value: String): Address {
        val uriInfo = MoneroUriParser.parse(value)
        return if (uriInfo?.viewKey != null) {
            val address = uriInfo.address
            val viewKey = uriInfo.viewKey
            val height = uriInfo.height

            MoneroWatchAddress(address, viewKey!!, height)
        } else {
            Address(hex = value, blockchainType = blockchainType)
        }
    }
}

class MoneroAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        MoneroKit.validateAddress(address.hex)
    }
}
