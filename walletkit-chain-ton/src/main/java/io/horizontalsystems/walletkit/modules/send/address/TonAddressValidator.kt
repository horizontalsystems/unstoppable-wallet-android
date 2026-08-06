package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.tonkit.FriendlyAddress

class TonAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        FriendlyAddress.parse(address.hex)
    }
}
