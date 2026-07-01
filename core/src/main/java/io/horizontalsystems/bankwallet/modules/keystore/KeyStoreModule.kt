package io.horizontalsystems.bankwallet.modules.keystore

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import kotlinx.parcelize.Parcelize

object KeyStoreModule {
    class Factory(private val mode: ModeType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KeyStoreViewModel(App.keyStoreManager, mode) as T
        }
    }

    @Parcelize
    enum class ModeType : Parcelable {
        NoSystemLock,
        InvalidKey,
        UserAuthentication
    }
}