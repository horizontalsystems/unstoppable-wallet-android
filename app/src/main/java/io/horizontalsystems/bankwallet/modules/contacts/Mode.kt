package io.horizontalsystems.bankwallet.modules.contacts

import android.os.Parcelable
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

sealed class Mode : Parcelable {
    @Parcelize
    object Full : Mode()

    @Parcelize
    class AddAddressToExistingContact(val blockchainType: BlockchainType, val address: String) : Mode()

    @Parcelize
    class AddAddressToNewContact(val blockchainType: BlockchainType, val address: String) : Mode()
}
