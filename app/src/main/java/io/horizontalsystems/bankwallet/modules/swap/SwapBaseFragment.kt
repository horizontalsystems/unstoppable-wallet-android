package io.horizontalsystems.bankwallet.modules.swap

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.selector.SelectorBottomSheetDialog
import io.horizontalsystems.bankwallet.ui.selector.SelectorItemViewHolderFactory
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper

abstract class SwapBaseFragment : BaseFragment() {

    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)
    private val selectSwapProviderViewModel by viewModels<SelectSwapProviderViewModel> {
        SwapMainModule.SelectSwapProviderViewModelFactory(mainViewModel.service)
    }

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

    protected fun showSwapProviderSelectorDialog() {
        val dialog = SelectorBottomSheetDialog<ViewItemWrapper<SwapMainModule.ISwapProvider>>()
        dialog.titleText = getString(R.string.Swap_SelectSwapProvider_Title)
        dialog.subtitleText = getString(R.string.Swap_SelectSwapProvider_Subtitle)
        dialog.headerIconResourceId = R.drawable.ic_swap
        dialog.items = selectSwapProviderViewModel.viewItems
        dialog.selectedItem = selectSwapProviderViewModel.selectedItem
        dialog.onSelectListener = { providerWrapper -> selectSwapProviderViewModel.setProvider(providerWrapper.item) }
        dialog.itemViewHolderFactory = SelectorItemViewHolderFactory()

        dialog.show(childFragmentManager, "selector_dialog")
    }

}
