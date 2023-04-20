package io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey

import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDKeychain
import kotlinx.parcelize.Parcelize

object ShowExtendedKeyModule {
    const val EXTENDED_ROOT_KEY = "extended_root_key"
    const val DISPLAY_KEY_TYPE = "display_key_type"

    class Factory(
        private val extendedRootKey: HDExtendedKey,
        private val displayKeyType: DisplayKeyType
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShowExtendedKeyViewModel(
                keyChain = HDKeychain(extendedRootKey.key),
                displayKeyType = displayKeyType,
                purpose = extendedRootKey.purposes.first(),
                extendedKeyCoinType = extendedRootKey.coinTypes.first()
            ) as T
        }
    }

    @Parcelize
    sealed class DisplayKeyType : Parcelable {
        @Parcelize
        object Bip32RootKey : DisplayKeyType()

        @Parcelize
        class AccountPrivateKey(val derivable: Boolean) : DisplayKeyType()

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

    fun prepareParams(extendedRootKey: HDExtendedKey, displayKeyType: DisplayKeyType) =
        bundleOf(EXTENDED_ROOT_KEY to extendedRootKey.serialize(), DISPLAY_KEY_TYPE to displayKeyType)
}
