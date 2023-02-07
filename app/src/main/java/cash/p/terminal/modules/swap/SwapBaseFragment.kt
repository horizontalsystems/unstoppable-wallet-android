package cash.p.terminal.modules.swap

import android.os.Bundle
import android.view.View
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment

abstract class SwapBaseFragment : BaseFragment() {

    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)
    protected val dex: SwapMainModule.Dex
        get() = mainViewModel.dex

    protected abstract fun restoreProviderState(providerState: SwapMainModule.SwapProviderState)
    protected abstract fun getProviderState(): SwapMainModule.SwapProviderState

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restoreProviderState(mainViewModel.providerState)
    }

    override fun onStop() {
        super.onStop()

        mainViewModel.providerState = getProviderState()
    }

    override fun onDestroy() {
        super.onDestroy()

        mainViewModel.providerState = getProviderState()
    }

}
