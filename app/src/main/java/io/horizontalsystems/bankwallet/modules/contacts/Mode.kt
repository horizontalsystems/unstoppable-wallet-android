package io.horizontalsystems.bankwallet.modules.contacts

import android.os.Parcelable
import io.horizontalsystems.bankwallet.serializers.BlockchainTypeSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed class Mode : Parcelable {
    @Parcelize
    @Serializable
    object Full : Mode()

    @Parcelize
    @Serializable
    class AddAddressToExistingContact(
        @Serializable(with = BlockchainTypeSerializer::class)
        val blockchainType: BlockchainType,
        val address: String
    ) : Mode()

    @Parcelize
    @Serializable
    class AddAddressToNewContact(
        @Serializable(with = BlockchainTypeSerializer::class)
        val blockchainType: BlockchainType,
        val address: String
    ) : Mode()
}
