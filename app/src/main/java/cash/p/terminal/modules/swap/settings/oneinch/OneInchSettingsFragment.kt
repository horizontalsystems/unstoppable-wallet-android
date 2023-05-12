package cash.p.terminal.modules.swap.settings.oneinch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.settings.RecipientAddressViewModel
import cash.p.terminal.modules.swap.settings.SwapSlippageViewModel
import cash.p.terminal.modules.swap.settings.ui.RecipientAddress
import cash.p.terminal.modules.swap.settings.ui.SlippageAmount
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.ScreenMessageWithAction
import cash.p.terminal.ui.compose.components.TextImportantWarning
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.setNavigationResult
import java.math.BigDecimal

class OneInchSettingsFragment : BaseFragment() {

    companion object {
        private const val dexKey = "dexKey"
        private const val addressKey = "addressKey"
        private const val slippageKey = "slippageKey"

        fun prepareParams(
            dex: SwapMainModule.Dex,
            address: Address?,
            slippage: BigDecimal
        ) = bundleOf(
            dexKey to dex,
            addressKey to address,
            slippageKey to slippage.toPlainString()
        )
    }

    private val dex by lazy {
        requireArguments().getParcelable<SwapMainModule.Dex>(dexKey)
    }

    private val address by lazy {
        requireArguments().getParcelable<Address>(addressKey)
    }

    private val slippage by lazy {
        requireArguments().getString(slippageKey)?.toBigDecimal()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val dexValue = dex
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    if (dexValue != null) {
                        OneInchSettingsScreen(
                            onCloseClick = {
                                findNavController().popBackStack()
                            },
                            dex = dexValue,
                            factory = OneInchSwapSettingsModule.Factory(address, slippage),
                            navController = findNavController()
                        )
                    } else {
                        ScreenMessageWithAction(
                            text = stringResource(R.string.Error),
                            icon = R.drawable.ic_error_48
                        ) {
                            ButtonPrimaryYellow(
                                modifier = Modifier
                                    .padding(horizontal = 48.dp)
                                    .fillMaxWidth(),
                                title = stringResource(R.string.Button_Close),
                                onClick = { findNavController().popBackStack() }
                            )
                        }
                    }
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
    navController: NavController,
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
                    RecipientAddress(dex.blockchainType, recipientAddressViewModel, navController)

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
                        val swapSettings = oneInchSettingsViewModel.swapSettings

                        if (swapSettings != null) {
                            navController.setNavigationResult(
                                SwapMainModule.resultKey,
                                bundleOf(
                                    SwapMainModule.swapSettingsRecipientKey to swapSettings.recipient,
                                    SwapMainModule.swapSettingsSlippageKey to swapSettings.slippage.toString(),
                                )
                            )
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
