package cash.p.terminal.modules.pin.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App

object PinEditModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PinEditViewModel(App.pinComponent) as T
        }
    }

    enum class EditStage(val title: Int) {
        Enter(R.string.EditPin_NewPinInfo),
        Confirm(R.string.PinSet_ConfirmInfo)
    }

    data class PinEditViewState(
        val stage: EditStage,
        val enteredCount: Int,
        val finished: Boolean,
        val reverseSlideAnimation: Boolean,
        val error: String?,
    )

}