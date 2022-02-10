package io.horizontalsystems.bankwallet.modules.evmfee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

class SendEvmFeeSettingsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(requireArguments().getInt(NAV_GRAPH_ID))

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    val viewModel = viewModel<ViewModel>(
                        factory = EvmFeeModule.Factory(
                            feeViewModel.feeService,
                            feeViewModel.gasPriceService,
                            feeViewModel.coinService
                        )
                    )
                    when (viewModel) {
                        is LegacyFeeSettingsViewModel -> {
                            LegacyFeeSettings(viewModel, findNavController())
                        }
                        is Eip1559FeeSettingsViewModel -> {
                            Eip1559FeeSettings(viewModel, findNavController())
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val NAV_GRAPH_ID = "nav_graph_id"

        fun prepareParams(@IdRes navGraphId: Int) =
            bundleOf(NAV_GRAPH_ID to navGraphId)
    }
}
