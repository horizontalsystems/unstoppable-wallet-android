package io.horizontalsystems.bankwallet.modules.pin

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.managers.UserManager

@HiltViewModel(assistedFactory = SetDuressPinViewModel.Factory::class)
class SetDuressPinViewModel @AssistedInject constructor(
    @Assisted private val input: SetDuressPinPage.Input?,
    private val userManager: UserManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(input: SetDuressPinPage.Input?): SetDuressPinViewModel
    }

    fun onDuressPinSet() {
        val accountIds = input?.accountIds
        if (!accountIds.isNullOrEmpty()) {
            userManager.allowAccountsForDuress(accountIds)
        }
    }

}
