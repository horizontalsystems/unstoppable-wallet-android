package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.walletkit.entities.Address

/** Recognizes classic `r...` addresses and X-addresses (`X...`), which pack a destination tag. */
class AddressHandlerXrp : IAddressHandler {
    override val blockchainType = BlockchainType.Xrp

    override fun isSupported(value: String) = XrpKit.isValidAddress(value)

    override fun parseAddress(value: String): Address {
        return Address(value, blockchainType = blockchainType)
    }
}
