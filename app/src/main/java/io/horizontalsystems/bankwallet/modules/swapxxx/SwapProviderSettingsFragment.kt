package io.horizontalsystems.bankwallet.modules.swapxxx

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer

class SwapProviderSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapProviderSettingsScreen(navController)
    }

}

private @Composable
fun SwapProviderSettingsScreen(navController: NavController) {
    val viewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = navController.previousBackStackEntry!!,
        factory = SwapViewModel.Factory()
    )

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.SwapSettings_Title),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Reset),
                        onClick = {

                        }
                    )
                ),
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        LazyColumn(
            modifier = Modifier.padding(it),
        ) {
            item {
                VSpacer(height = 12.dp)
            }

            viewModel.uiState.quote?.swapQuote?.let { swapQuote ->
                items(swapQuote.getSettingFields()) { settingField ->
                    val settingId = settingField.id

                    settingField.GetContent(
                        navController = navController,
                        initial = viewModel.getSettingValue(settingId),
                        onError = {
                            viewModel.onSettingError(settingId, it)
                            Log.e("AAA", "address error: $it")
                        },
                        onValueChange = {
                            viewModel.onSettingEnter(settingId, it)
                            Log.e("AAA", "Address: $it")
                        }
                    )
                }
            }

            item {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(id = R.string.SwapSettings_Apply),
                    enabled = viewModel.saveSettingsEnabled,
                    onClick = {
                        viewModel.saveSettings()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
