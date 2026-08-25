package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.tronkit.account.AddressHandler
import io.horizontalsystems.walletkit.entities.Address

class AddressHandlerTron : IAddressHandler {
    override val blockchainType = BlockchainType.Tron

    override fun isSupported(value: String) = try {
        io.horizontalsystems.tronkit.models.Address.fromBase58(value)
        true
    } catch (e: AddressHandler.AddressValidationException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    override fun parseAddress(value: String): Address {
        val tronAddress = io.horizontalsystems.tronkit.models.Address.fromBase58(value)
        return Address(tronAddress.base58, blockchainType = blockchainType)
    }
}
