package io.horizontalsystems.bankwallet.modules.swap.info

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule

object SwapInfoModule {

    private const val dexKey = "dexKey"

    class Factory(arguments: Bundle) : ViewModelProvider.Factory {
        private val dex: SwapMainModule.Dex = arguments.getParcelable(dexKey)!!

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapInfoViewModel(dex) as T
        }
    }

    fun prepareParams(dex: SwapMainModule.Dex) = bundleOf(dexKey to dex)

}
