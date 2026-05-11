package io.horizontalsystems.bankwallet.modules.contacts

import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable

@Serializable
sealed class Mode {
    @Serializable
    object Full : Mode()

    @Serializable
    class AddAddressToExistingContact(
        val blockchainType: BlockchainType,
        val address: String
    ) : Mode()

    @Serializable
    class AddAddressToNewContact(
        val blockchainType: BlockchainType,
        val address: String
    ) : Mode()
}
