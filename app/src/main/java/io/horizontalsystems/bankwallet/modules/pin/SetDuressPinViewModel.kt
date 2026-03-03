package io.horizontalsystems.bankwallet.modules.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.UserManager

class SetDuressPinViewModel(
    private val userManager: UserManager,
    private val accountIds: List<String>?,
) : ViewModel() {

    fun onDuressPinSet() {
        if (!accountIds.isNullOrEmpty()) {
            userManager.allowAccountsForDuress(accountIds)
        }
    }

    class Factory(val accountIds: List<String>?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SetDuressPinViewModel(App.userManager, accountIds) as T
        }
    }

}
