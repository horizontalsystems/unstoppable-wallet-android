package io.horizontalsystems.bankwallet.modules.publickeys

import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.entities.Account
import kotlinx.parcelize.Parcelize

object PublicKeysModule {
    const val ACCOUNT = "account"

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PublicKeysViewModel(account) as T
        }
    }

    fun prepareParams(account: Account) = bundleOf(ACCOUNT to account)

    @Parcelize
    data class PrivateKey(val blockchain: String, val value: String) : Parcelable

}
