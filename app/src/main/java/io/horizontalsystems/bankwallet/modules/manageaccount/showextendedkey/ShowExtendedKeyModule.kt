package io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

object ShowExtendedKeyModule {

    @Serializable
    @Parcelize
    sealed class DisplayKeyType : Parcelable {
        @Serializable
        @Parcelize
        object Bip32RootKey : DisplayKeyType()

        @Serializable
        @Parcelize
        class AccountPrivateKey(val derivable: Boolean) : DisplayKeyType()

        @Serializable
        @Parcelize
        class AccountPublicKey(val derivable: Boolean) : DisplayKeyType()

        val isDerivable: Boolean
            get() = when (this) {
                is AccountPrivateKey -> derivable
                is AccountPublicKey -> derivable
                Bip32RootKey -> false
            }

        val isPrivate: Boolean
            get() = when (this) {
                is AccountPrivateKey -> true
                is AccountPublicKey -> false
                Bip32RootKey -> true
            }
    }
}
