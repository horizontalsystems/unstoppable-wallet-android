package io.horizontalsystems.walletkit.modules.walletconnect.request.sendtransaction

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
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.modules.evmfee.FeeSettingsInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.walletkit.modules.walletconnect.WCDelegate
import io.horizontalsystems.walletkit.modules.walletconnect.request.SessionRequestUI
import io.horizontalsystems.walletkit.modules.walletconnect.session.TitleValueCell
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline1_leah
import io.horizontalsystems.walletkit.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.walletkit.ui.compose.components.subhead_grey
import io.horizontalsystems.walletkit.modules.walletconnect.VerificationAlert
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellSecondary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCSendEthRequestScreen(
    navigation: HSNavigation,
    logger: AppLogger,
    blockchainType: BlockchainType,
    transaction: WalletConnectTransaction,
    sessionRequestUI: SessionRequestUI.Content,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel = viewModel<WCSendEthereumTransactionRequestViewModel>(
        factory = WCSendEthereumTransactionRequestViewModel.Factory(
            blockchainType = blockchainType,
            transaction = transaction,
            peerName = sessionRequestUI.peerUI.peerName
        )
    )
    val uiState = viewModel.uiState
    val view = LocalView.current
    val confirmAction = rememberAsyncAction()
    val doneMessage = stringResource(R.string.Hud_Text_Done)
    val sendingMessage = stringResource(R.string.Send_Sending)
    val feeText = stringResource(id = R.string.Send_Fee)
    val feeInfoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)

    BottomSheetContent(
        onDismissRequest = {
            WCDelegate.discardActiveSessionRequest(sessionRequestUI.requestId)
            navigation.removeLastOrNull()
        },
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
                text = stringResource(
                    R.string.WalletConnect_ConfirmTransaction,
                ),
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

            // The url above is what the dApp says it is. This is the attested origin, and it is the
            // only thing on the screen a phishing site cannot set. Unverified origins are not
            // reported here: that was already accepted when the session was approved.
            VerificationAlert(sessionRequestUI.verification, reportUnverified = false)

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
                        navigation.slideFromBottom(
                            FeeSettingsInfoSheet(FeeSettingsInfoSheet.Input(feeText, feeInfoText))
                        )
                    },
                    onCopy = { snackbarActions.showSuccessMessage(it) }
                )

                // Which chain the transaction lands on decides what the signature is worth, so it
                // is named on the approval rather than left implicit.
                sessionRequestUI.chainName?.let { chainName ->
                    TitleValueCell(stringResource(R.string.Balance_Network), chainName)
                }

                TitleValueCell(
                    stringResource(R.string.Wallet_Title),
                    sessionRequestUI.walletName
                )

                FeeCell(
                    primaryValue = uiState.networkFee?.primary?.getFormatted(),
                    secondaryValue = uiState.networkFee?.secondary?.getFormatted(),
                    onInfoClick = {
                        navigation.slideFromBottom(
                            FeeSettingsInfoSheet(FeeSettingsInfoSheet.Input(feeText, feeInfoText))
                        )
                    }
                )
            }
            uiState.cautions.forEach { caution ->
                CautionCell(caution)
            }
            ButtonsGroupHorizontal {
                HSButton(
                    title = stringResource(R.string.Button_Reject),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.reject(sessionRequestUI.topic, sessionRequestUI.requestId)
                        navigation.removeLastOrNull()
                    }
                )
                HSButton(
                    title = stringResource(R.string.Button_Confirm),
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                    enabled = !confirmAction.inProgress && uiState.sendEnabled,
                    onClick = {
                        confirmAction.run {
                            Toast.makeText(view.context, sendingMessage, Toast.LENGTH_SHORT).show()
                            try {
                                logger.info("click confirm button")
                                viewModel.confirm()
                                logger.info("success")

                                snackbarActions.showSuccessMessage(doneMessage)

                                delay(1200)
                            } catch (t: Throwable) {
                                logger.warning("failed", t)
                                snackbarActions.showErrorMessage(t.message ?: "Error")
                            }

                            navigation.removeLastOrNull()
                        }
                    }
                )
            }
        }
    }
}
