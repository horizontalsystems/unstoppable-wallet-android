package io.horizontalsystems.bankwallet.modules.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.selector.*
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_swap.*

class SwapMainFragment : BaseFragment() {

    private val vmFactory by lazy { SwapMainModule.Factory(requireArguments()) }
    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment) { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        setProviderView(mainViewModel.provider)

        mainViewModel.providerLiveData.observe(viewLifecycleOwner, { provider ->
            setProviderView(provider)
        })

        topMenuCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    private fun setProviderView(provider: ISwapProvider) {
        setTopMenu(provider)

        childFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_placeholder, provider.fragment)
            .commitNow()
    }

    private fun setTopMenu(provider: ISwapProvider) {
        topMenuCompose.setContent {
            ComposeAppTheme {
                Row(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .height(40.dp)
                        .padding(end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ButtonSecondaryTransparent(
                        title = provider.title,
                        iconRight = R.drawable.ic_down_arrow_20,
                        onClick = {
                            showSwapProviderSelectorDialog()
                        }
                    )
                    ButtonSecondaryCircle(
                        icon = R.drawable.ic_manage_2,
                        onClick = {
                            findNavController().navigate(R.id.swapFragment_to_swapSettingsMainFragment)
                        }
                    )
                }
            }
        }
    }

    private fun showSwapProviderSelectorDialog() {
        val dialog = SelectorBottomSheetDialog<ViewItemWithIconWrapper<ISwapProvider>>()
        dialog.titleText = getString(R.string.Swap_SelectSwapProvider_Title)
        dialog.subtitleText = getString(R.string.Swap_SelectSwapProvider_Subtitle)
        dialog.headerIconResourceId = R.drawable.ic_swap_24
        dialog.items = mainViewModel.providerItems
        dialog.selectedItem = mainViewModel.selectedProviderItem
        dialog.onSelectListener = { providerWrapper -> mainViewModel.setProvider(providerWrapper.item) }
        dialog.itemViewHolderFactory = SelectorItemWithIconViewHolderFactory()

        dialog.show(childFragmentManager, "selector_dialog")
    }

}
