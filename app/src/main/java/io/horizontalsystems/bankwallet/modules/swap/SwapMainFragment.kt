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
import io.horizontalsystems.bankwallet.databinding.FragmentSwapBinding
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSwapProviderSelectDialog
import io.horizontalsystems.core.findNavController

class SwapMainFragment : BaseFragment() {

    private val vmFactory by lazy { SwapMainModule.Factory(requireArguments()) }
    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment) { vmFactory }

    private var _binding: FragmentSwapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwapBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setOnMenuItemClickListener { item ->
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

        binding.topMenuCompose.setViewCompositionStrategy(
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
        binding.topMenuCompose.setContent {
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
        val dialog = BottomSheetSwapProviderSelectDialog()
        dialog.items = mainViewModel.providerItems
        dialog.selectedItem = mainViewModel.selectedProviderItem
        dialog.onSelectListener = { mainViewModel.setProvider(it) }

        dialog.show(childFragmentManager, "selector_dialog")
    }

}
