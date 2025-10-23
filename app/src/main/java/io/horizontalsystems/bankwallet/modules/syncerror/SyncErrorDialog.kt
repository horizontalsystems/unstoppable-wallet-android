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
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Wallet
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
                    SyncErrorScreen(navController, input.wallet)
                }
            }
        }
    }

    @Parcelize
    data class Input(val wallet: Wallet, val errorMessage: String?) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncErrorScreen(navController: NavController, wallet: Wallet) {
    val viewModel = viewModel<SyncErrorViewModel>(factory = SyncErrorModule.Factory(wallet))
    val text = if (viewModel.sourceChangeable) {
        stringResource(R.string.BalanceSyncError_ChangableSourceErrorText)
    } else {
        stringResource(R.string.BalanceSyncError_ErrorText)
    }

    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = {
                navController.popBackStack()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.BalanceSyncError_ButtonRetry),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
                        viewModel.retry()
                        navController.popBackStack()
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
                            navController.popBackStack()

                            val blockchainWrapper = viewModel.blockchainWrapper
                            when (blockchainWrapper) {
                                is SyncErrorModule.BlockchainWrapper.Bitcoin -> {
                                    navController.slideFromBottom(
                                        R.id.btcBlockchainSettingsFragment,
                                        blockchainWrapper.blockchain
                                    )
                                }

                                is SyncErrorModule.BlockchainWrapper.Evm -> {
                                    navController.slideFromBottom(
                                        R.id.evmNetworkFragment,
                                        blockchainWrapper.blockchain
                                    )
                                }

                                SyncErrorModule.BlockchainWrapper.Monero -> {
                                    navController.slideFromBottom(R.id.moneroNetworkFragment)
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

