package io.horizontalsystems.walletkit.chain.zcash

import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAddressValidator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.marketkit.models.BlockchainType

class AddressHandlerZcash : IAddressHandler {
    override val blockchainType = BlockchainType.Zcash

    override fun isSupported(value: String): Boolean {
        return ZcashAddressValidator.validate(value)
    }

    override fun parseAddress(value: String): Address {
        return Address(value, blockchainType = blockchainType)
    }

}
