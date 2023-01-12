package io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R

object RestoreMenuModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestoreMenuViewModel() as T
        }
    }

    enum class RestoreOption(@StringRes val titleRes: Int) {
        RecoveryPhrase(R.string.Restore_RecoveryPhrase),
        PrivateKey(R.string.Restore_PrivateKey)
    }
}
