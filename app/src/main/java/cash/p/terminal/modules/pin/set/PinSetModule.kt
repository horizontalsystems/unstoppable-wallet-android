package cash.p.terminal.modules.pin.set

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.modules.pin.PinType

object PinSetModule {

    class Factory(private val pinType: PinType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PinSetViewModel(
                pinComponent = App.pinComponent,
                pinType = pinType,
                transactionHiddenManager = getKoinInstance<TransactionHiddenManager>()
            ) as T
        }
    }

    enum class SetStage {
        Enter,
        Confirm
    }

    data class PinSetViewState(
        val stage: SetStage,
        val enteredCount: Int,
        val finished: Boolean,
        val reverseSlideAnimation: Boolean,
        val error: String?,
    )

}