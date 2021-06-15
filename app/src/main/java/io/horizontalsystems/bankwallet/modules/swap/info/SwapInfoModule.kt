package io.horizontalsystems.bankwallet.modules.swap.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule

object SwapInfoModule {

    class Factory(private val dex: SwapMainModule.Dex) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapInfoViewModel(dex) as T
        }
    }

}
