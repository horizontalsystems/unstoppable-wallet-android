package cash.p.terminal.modules.send.evm.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.evmfee.Eip1559FeeSettings
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.evmfee.EvmFeeModule
import cash.p.terminal.modules.evmfee.LegacyFeeSettings
import cash.p.terminal.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import cash.p.terminal.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HsIconButton
import cash.p.terminal.ui.compose.components.MenuItem

class SendEvmSettingsFragment : BaseFragment() {

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
                    val feeSettingsViewModel = viewModel<ViewModel>(
                        factory = EvmFeeModule.Factory(
                            feeViewModel.feeService,
                            feeViewModel.gasPriceService,
                            feeViewModel.coinService
                        )
                    )
                    val sendSettingsViewModel = viewModel<SendEvmSettingsViewModel>(
                        factory = SendEvmSettingsModule.Factory(feeViewModel.gasPriceService)
                    )
                    SendEvmFeeSettingsScreen(
                        viewModel = sendSettingsViewModel,
                        feeSettingsViewModel = feeSettingsViewModel,
                        navController = findNavController()
                    )
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


@Composable
fun SendEvmFeeSettingsScreen(
    viewModel: SendEvmSettingsViewModel,
    feeSettingsViewModel: ViewModel,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .background(color = ComposeAppTheme.colors.tyler)
    ) {
        AppBar(
            title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
            navigationIcon = {
                HsIconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "back button",
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Reset),
                    enabled = !viewModel.isRecommendedSettingsSelected,
                    onClick = { viewModel.onClickReset() }
                )
            )
        )

        when (feeSettingsViewModel) {
            is LegacyFeeSettingsViewModel -> {
                LegacyFeeSettings(feeSettingsViewModel, navController)
            }
            is Eip1559FeeSettingsViewModel -> {
                Eip1559FeeSettings(feeSettingsViewModel, navController)
            }
        }
    }
}
