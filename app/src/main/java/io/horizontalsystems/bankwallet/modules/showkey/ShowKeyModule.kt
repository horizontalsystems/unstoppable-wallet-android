package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import kotlinx.parcelize.Parcelize

object ShowKeyModule {
    const val ACCOUNT = "account"

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = ShowKeyService(account, App.pinComponent, App.accountSettingManager)
            return ShowKeyViewModel(service) as T
        }
    }

    fun prepareParams(account: Account) = bundleOf(ACCOUNT to account)

    enum class ShowKeyTab(@StringRes val title: Int) {
        MnemonicPhrase(R.string.ShowKey_TabMnemonicPhrase),
        PrivateKey(R.string.ShowKey_TabPrivateKey)
    }

    @Parcelize
    data class PrivateKey(val blockchain: String, val value: String) : Parcelable

}
