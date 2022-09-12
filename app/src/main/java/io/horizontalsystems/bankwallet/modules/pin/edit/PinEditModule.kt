package io.horizontalsystems.bankwallet.modules.pin.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object PinEditModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PinEditViewModel(App.pinComponent) as T
        }
    }

    enum class EditStage(val title: Int) {
        Unlock(R.string.EditPin_UnlockInfo),
        Enter(R.string.EditPin_NewPinInfo),
        Confirm(R.string.PinSet_ConfirmInfo)
    }

    data class PinEditViewState(
        val stage: EditStage,
        val enteredCount: Int,
        val finished: Boolean,
        val reverseSlideAnimation: Boolean,
        val showShakeAnimation: Boolean,
        val error: String?,
    )

}