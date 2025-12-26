package io.horizontalsystems.bankwallet.modules.walletconnect.request.signtransaction

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionView
import io.horizontalsystems.bankwallet.modules.walletconnect.request.SessionRequestUI
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.DataBlock
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.FeeCell
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WalletConnectTransaction
import io.horizontalsystems.bankwallet.modules.walletconnect.session.TitleValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCSignEthereumTransactionRequestScreen(
    navController: NavController,
    logger: AppLogger,
    blockchainType: BlockchainType,
    transaction: WalletConnectTransaction,
    sessionRequestUI: SessionRequestUI.Content,
) {
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.wcRequestFragment)
    }
    val viewModel = viewModel<WCSignEthereumTransactionRequestViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = WCSignEthereumTransactionRequestViewModel.Factory(
            blockchainType = blockchainType,
            transaction = transaction,
            peerName = sessionRequestUI.peerUI.peerName
        )
    )
    val uiState = viewModel.uiState
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val feeText = stringResource(id = R.string.Send_Fee)
    val feeInfoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)
    val doneMessage = stringResource(R.string.Hud_Text_Done)

    BottomSheetContent(
        onDismissRequest = navController::popBackStack,
        sheetState = sheetState,
    ) { snackbarActions ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .size(52.dp, 4.dp)
                    .background(ComposeAppTheme.colors.blade, RoundedCornerShape(50))
            ) { }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    painter = rememberAsyncImagePainter(
                        model = sessionRequestUI.peerUI.peerIcon,
                        error = painterResource(R.drawable.ic_platform_placeholder_24)
                    ),
                    contentDescription = null,
                )
            }
            VSpacer(16.dp)
            headline1_leah(
                text = stringResource(R.string.WalletConnect_TokenAllowance),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            VSpacer(8.dp)
            subhead_grey(
                text = TextHelper.getCleanedUrl(sessionRequestUI.peerUI.peerUri),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            VSpacer(16.dp)
            TextBlock(
                text = stringResource(R.string.WalletConnect_DappWillBeAbleToMoveToken, sessionRequestUI.peerUI.peerName),
            )
            VSpacer(16.dp)
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp)
            ) {
                DataBlock(
                    sections = uiState.sectionViewItems,
                    onInfoClick = {
                        navController.slideFromBottom(
                            R.id.feeSettingsInfoDialog,
                            FeeSettingsInfoDialog.Input(feeText, feeInfoText)
                        )
                    },
                    onCopy = {
                        snackbarActions.showSuccessMessage(it)
                    }
                )

                TitleValueCell(
                    stringResource(R.string.Wallet_Title),
                    sessionRequestUI.walletName
                )
                uiState.networkFee?.let { fee ->
                    FeeCell(
                        primaryValue = fee.primary.getFormatted(),
                        secondaryValue = fee.secondary?.getFormatted(),
                        onInfoClick = {
                            navController.slideFromBottom(
                                R.id.feeSettingsInfoDialog,
                                FeeSettingsInfoDialog.Input(feeText, feeInfoText)
                            )
                        }
                    )
                }
            }
            ButtonsGroupHorizontal {
                HSButton(
                    title = stringResource(R.string.Button_Reject),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.reject()
                        navController.popBackStack()
                    }
                )
                HSButton(
                    title = stringResource(R.string.Button_Approve),
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            try {
                                logger.info("click sign button")
                                viewModel.sign()
                                logger.info("success")

                                Toast.makeText(view.context, doneMessage, Toast.LENGTH_SHORT).show()
                                delay(1200)
                            } catch (t: Throwable) {
                                logger.warning("failed", t)
                                Toast.makeText(view.context, t.javaClass.simpleName, Toast.LENGTH_SHORT).show()
                            }

                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }

    ConfirmTransactionScreen(
        title = stringResource(id = R.string.WalletConnect_SignMessageRequest_Title),
        onClickBack = navController::popBackStack,
        onClickSettings = null,
        onClickClose = navController::popBackStack,
        buttonsSlot = {

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Sign),
                onClick = {
                    coroutineScope.launch {
                        try {
                            logger.info("click sign button")
                            viewModel.sign()
                            logger.info("success")

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                        } catch (t: Throwable) {
                            logger.warning("failed", t)
                            HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                        }

                        navController.popBackStack()
                    }
                }
            )
            VSpacer(16.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Reject),
                onClick = {
                    viewModel.reject()
                    navController.popBackStack()
                }
            )
        }
    ) {
        SendEvmTransactionView(
            navController,
            uiState.sectionViewItems,
            uiState.cautions,
            uiState.transactionFields,
            uiState.networkFee,
            StatPage.WalletConnect
        )
    }
}
