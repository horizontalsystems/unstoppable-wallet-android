package cash.p.terminal.modules.swap.settings.oneinch

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
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.oneinch.OneInchModule
import cash.p.terminal.modules.swap.oneinch.OneInchSwapViewModel
import cash.p.terminal.modules.swap.settings.RecipientAddressViewModel
import cash.p.terminal.modules.swap.settings.SwapSettingsBaseFragment
import cash.p.terminal.modules.swap.settings.SwapSlippageViewModel
import cash.p.terminal.modules.swap.settings.ui.RecipientAddress
import cash.p.terminal.modules.swap.settings.ui.SlippageAmount
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.TextImportantWarning
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class OneInchSettingsFragment : SwapSettingsBaseFragment() {

    private val oneInchViewModel by navGraphViewModels<OneInchSwapViewModel>(R.id.swapFragment) {
        OneInchModule.Factory(dex)
    }

    private val vmFactory by lazy {
        OneInchSwapSettingsModule.Factory(oneInchViewModel.tradeService)
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
                    OneInchSettingsScreen(
                        onCloseClick = {
                            findNavController().popBackStack()
                        },
                        dex = dex,
                        factory = vmFactory,
                    )
                }
            }
        }
    }

}

@Composable
private fun OneInchSettingsScreen(
    onCloseClick: () -> Unit,
    factory: OneInchSwapSettingsModule.Factory,
    dex: SwapMainModule.Dex,
    oneInchSettingsViewModel: OneInchSettingsViewModel = viewModel(factory = factory),
    recipientAddressViewModel: RecipientAddressViewModel = viewModel(factory = factory),
    slippageViewModel: SwapSlippageViewModel = viewModel(factory = factory),
) {
    val (buttonTitle, buttonEnabled) = oneInchSettingsViewModel.buttonState
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
                    RecipientAddress(dex.blockchainType, recipientAddressViewModel)

                    Spacer(modifier = Modifier.height(24.dp))
                    SlippageAmount(slippageViewModel)

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
                        if (oneInchSettingsViewModel.onDoneClick()) {
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
