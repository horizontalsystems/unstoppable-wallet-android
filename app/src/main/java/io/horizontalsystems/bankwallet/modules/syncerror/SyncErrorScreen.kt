package io.horizontalsystems.bankwallet.modules.syncerror

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BtcBlockchainSettingsScreen
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkScreen
import io.horizontalsystems.bankwallet.modules.moneronetwork.MoneroNetworkScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SyncErrorScreen(
    val wallet: Wallet, val errorMessage: String?
) : HSScreen(bottomSheet = true) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        SyncErrorScreen(backStack, wallet)
    }
}

class SyncErrorDialog : BaseComposableBottomSheetFragment() {

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
                val navController = findNavController()
                navController.getInput<Input>()?.let { input ->
//                    SyncErrorScreen(navController, input.wallet)
                }
            }
        }
    }

    @Parcelize
    data class Input(val wallet: Wallet, val errorMessage: String?) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncErrorScreen(backStack: NavBackStack<HSScreen>, wallet: Wallet) {
    val viewModel = viewModel<SyncErrorViewModel>(factory = SyncErrorModule.Factory(wallet))
    val text = if (viewModel.sourceChangeable) {
        stringResource(R.string.BalanceSyncError_ChangableSourceErrorText)
    } else {
        stringResource(R.string.BalanceSyncError_ErrorText)
    }

    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = {
                backStack.removeLastOrNull()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.BalanceSyncError_ButtonRetry),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
                        viewModel.retry()
                        backStack.removeLastOrNull()
                    }
                )
                if (viewModel.sourceChangeable) {
                    HSButton(
                        title = stringResource(R.string.BalanceSyncError_ButtonChangeSource),
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Transparent,
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Medium,
                        onClick = {
                            backStack.removeLastOrNull()

                            val blockchainWrapper = viewModel.blockchainWrapper
                            when (blockchainWrapper) {
                                is SyncErrorModule.BlockchainWrapper.Bitcoin -> {
                                    backStack.add(BtcBlockchainSettingsScreen)
                                }

                                is SyncErrorModule.BlockchainWrapper.Evm -> {
                                    backStack.add(EvmNetworkScreen(blockchainWrapper.blockchain))
                                }

                                SyncErrorModule.BlockchainWrapper.Monero -> {
                                    backStack.add(MoneroNetworkScreen)
                                }

                                else -> {}
                            }
                        }
                    )
                }
            },
            content = {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.warning_filled_24),
                    imageTint = ComposeAppTheme.colors.lucian,
                    title = stringResource(R.string.BalanceSyncError_Title)
                )
                TextBlock(
                    text = text,
                    textAlign = TextAlign.Center
                )
            }
        )
    }
}

