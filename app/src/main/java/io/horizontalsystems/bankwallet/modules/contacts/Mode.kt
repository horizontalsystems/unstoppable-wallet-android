package io.horizontalsystems.bankwallet.modules.contacts

import io.horizontalsystems.bankwallet.serializers.BlockchainTypeSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable

@Serializable
sealed class Mode {
    @Serializable
    object Full : Mode()

    @Serializable
    class AddAddressToExistingContact(
        @Serializable(with = BlockchainTypeSerializer::class) val blockchainType: BlockchainType,
        val address: String
    ) : Mode()

    @Serializable
    class AddAddressToNewContact(
        @Serializable(with = BlockchainTypeSerializer::class) val blockchainType: BlockchainType,
        val address: String
    ) : Mode()
}
