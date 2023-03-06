package io.horizontalsystems.bankwallet.modules.swap.settings.uniswap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapDeadlineViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSlippageViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.RecipientAddress
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.SlippageAmount
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.TransactionDeadlineInput
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapModule
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class UniswapSettingsFragment : SwapSettingsBaseFragment() {
    private val uniswapViewModel by navGraphViewModels<UniswapViewModel>(R.id.swapFragment) {
        UniswapModule.Factory(dex)
    }

    private val vmFactory by lazy {
        UniswapSettingsModule.Factory(uniswapViewModel.tradeService)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
                    UniswapSettingsScreen(
                        onCloseClick = {
                            findNavController().popBackStack()
                        },
                        dex = dex,
                        factory = vmFactory,
                        navController = findNavController()
                    )
                }
            }
        }
    }

}

@Composable
private fun UniswapSettingsScreen(
    onCloseClick: () -> Unit,
    factory: UniswapSettingsModule.Factory,
    dex: SwapMainModule.Dex,
    uniswapSettingsViewModel: UniswapSettingsViewModel = viewModel(factory = factory),
    deadlineViewModel: SwapDeadlineViewModel = viewModel(factory = factory),
    recipientAddressViewModel: RecipientAddressViewModel = viewModel(factory = factory),
    slippageViewModel: SwapSlippageViewModel = viewModel(factory = factory),
    navController: NavController,
) {
    val (buttonTitle, buttonEnabled) = uniswapSettingsViewModel.buttonState
    val view = LocalView.current

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = TranslatableString.ResString(R.string.SwapSettings_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onCloseClick
                    )
                )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    RecipientAddress(dex.blockchainType, recipientAddressViewModel, navController)

                    Spacer(modifier = Modifier.height(24.dp))
                    SlippageAmount(slippageViewModel)

                    Spacer(modifier = Modifier.height(24.dp))
                    TransactionDeadlineInput(deadlineViewModel)

                    Spacer(modifier = Modifier.height(24.dp))
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.SwapSettings_FeeSettingsAlert)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = buttonTitle,
                    onClick = {
                        if (uniswapSettingsViewModel.onDoneClick()) {
                            onCloseClick()
                        } else {
                            HudHelper.showErrorMessage(view, R.string.default_error_msg)
                        }
                    },
                    enabled = buttonEnabled
                )
            }
        }
    }
}
