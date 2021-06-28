package io.horizontalsystems.bankwallet.modules.swap.info

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.core.findNavController

object SwapInfoModule {

    private const val dexKey = "dexKey"

    class Factory(arguments: Bundle) : ViewModelProvider.Factory {
        private val dex: SwapMainModule.Dex = arguments.getParcelable(dexKey)!!

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapInfoViewModel(dex) as T
        }
    }

    fun start(fragment: Fragment, navOptions: NavOptions, dex: SwapMainModule.Dex) {
        fragment.findNavController().navigate(R.id.swapSettingsMainFragment_to_swapInfoFragment, bundleOf(dexKey to dex), navOptions)
    }

}
