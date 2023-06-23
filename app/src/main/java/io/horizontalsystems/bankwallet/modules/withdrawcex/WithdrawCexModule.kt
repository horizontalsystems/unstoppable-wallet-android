package cash.p.terminal.modules.withdrawcex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.modules.market.ImageSource

object WithdrawCexModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WithdrawCexViewModel() as T
        }
    }

    data class NetworkViewItem(
        val title: String,
        val imageSource: ImageSource,
        val selected: Boolean,
    )

    sealed class CodeGetButtonState{
        object Active : CodeGetButtonState()
        class Pending(val secondsLeft: Int) : CodeGetButtonState()
    }

}
