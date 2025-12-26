package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.multiswap.SwapViewModel
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class SwapSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapProviderSettingsScreen(navController)
    }
}

@Composable
private fun SwapProviderSettingsScreen(navController: NavController) {
    val previousBackStackEntry = remember { navController.previousBackStackEntry }
    val swapViewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = previousBackStackEntry!!,
    )

    val viewModel =
        viewModel<SwapSettingsViewModel>(factory = SwapSettingsViewModel.Factory(swapViewModel.getSettings()))

    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.SwapSettings_Title),
        onBack = navController::popBackStack,
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(id = R.string.SwapSettings_Apply),
                    enabled = uiState.applyEnabled,
                    onClick = {
                        swapViewModel.onUpdateSettings(viewModel.getSettings())
                        navController.popBackStack()
                    }
                )
            }
        },
    ) {
        LazyColumn {
            item {
                VSpacer(height = 12.dp)
            }

            swapViewModel.uiState.quote?.swapQuote?.let { swapQuote ->
                items(swapQuote.settings) { setting ->
                    val settingId = setting.id

                    setting.GetContent(
                        navController = navController,
                        onError = {
                            viewModel.onSettingError(settingId, it)
                        },
                        onValueChange = {
                            viewModel.onSettingEnter(settingId, it)
                        }
                    )
                }
            }

            item {
            }
        }
    }
}
