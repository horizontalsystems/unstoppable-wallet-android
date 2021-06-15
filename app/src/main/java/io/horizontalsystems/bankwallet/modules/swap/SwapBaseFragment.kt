package io.horizontalsystems.bankwallet.modules.swap

import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment

abstract class SwapBaseFragment : BaseFragment() {

    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)

    protected val dex: SwapMainModule.Dex
        get() = mainViewModel.dex

    protected abstract fun restoreProviderState(providerState: SwapMainModule.SwapProviderState)
    protected abstract fun getProviderState(): SwapMainModule.SwapProviderState

    override fun onStart() {
        super.onStart()

        restoreProviderState(mainViewModel.providerState)
    }

    override fun onStop() {
        super.onStop()

        mainViewModel.providerState = getProviderState()
    }

}
