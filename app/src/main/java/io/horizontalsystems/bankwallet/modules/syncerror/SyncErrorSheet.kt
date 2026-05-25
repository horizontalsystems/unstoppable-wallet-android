package io.horizontalsystems.bankwallet.modules.syncerror

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BtcBlockchainSettingsPage
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkPage
import io.horizontalsystems.bankwallet.modules.moneronetwork.MoneroNetworkPage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.zanonetwork.ZanoNetworkPage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.HSBottomSheet
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SyncErrorSheet(val input: Input) : HSBottomSheet() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        SyncErrorScreen(navController, input.wallet)
    }

    @Serializable
    @Parcelize
    data class Input(val wallet: Wallet, val errorMessage: String?) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncErrorScreen(navController: HSNavigation, wallet: Wallet) {
    val viewModel = viewModel<SyncErrorViewModel>(factory = SyncErrorModule.Factory(wallet))
    val text = if (viewModel.sourceChangeable) {
        stringResource(R.string.BalanceSyncError_ChangableSourceErrorText)
    } else {
        stringResource(R.string.BalanceSyncError_ErrorText)
    }

    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = {
                navController.removeLastOrNull()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.BalanceSyncError_ButtonRetry),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
                        viewModel.retry()
                        navController.removeLastOrNull()
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
                            navController.removeLastOrNull()

                            val blockchainWrapper = viewModel.blockchainWrapper
                            when (blockchainWrapper) {
                                is SyncErrorModule.BlockchainWrapper.Bitcoin -> {
                                    navController.slideFromBottom(
                                        BtcBlockchainSettingsPage(blockchainWrapper.blockchain),
                                    )
                                }

                                is SyncErrorModule.BlockchainWrapper.Evm -> {
                                    navController.slideFromBottom(
                                        EvmNetworkPage(blockchainWrapper.blockchain)
                                    )
                                }

                                SyncErrorModule.BlockchainWrapper.Monero -> {
                                    navController.slideFromBottom(MoneroNetworkPage)
                                }

                                SyncErrorModule.BlockchainWrapper.Zano -> {
                                    navController.slideFromBottom(ZanoNetworkPage)
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

