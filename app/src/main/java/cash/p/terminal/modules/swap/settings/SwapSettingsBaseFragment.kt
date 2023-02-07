package cash.p.terminal.modules.swap.settings

import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.SwapMainViewModel

abstract class SwapSettingsBaseFragment : BaseFragment() {
    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)

    val dex: SwapMainModule.Dex
        get() = mainViewModel.dex

}
