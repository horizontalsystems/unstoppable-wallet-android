package io.horizontalsystems.bankwallet.modules.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.UserManager

class SetDuressPinViewModel(
    private val input: SetDuressPinFragment.Input?,
    private val userManager: UserManager,
) : ViewModel() {

    fun onDuressPinSet() {
        val accountIds = input?.accountIds
        if (!accountIds.isNullOrEmpty()) {
            userManager.allowAccountsForDuress(accountIds)
        }
    }

    class Factory(private val input: SetDuressPinFragment.Input?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SetDuressPinViewModel(input, App.userManager) as T
        }
    }

}
