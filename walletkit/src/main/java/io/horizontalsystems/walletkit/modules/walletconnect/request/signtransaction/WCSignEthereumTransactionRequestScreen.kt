package io.horizontalsystems.walletkit.modules.walletconnect.request.signtransaction

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
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.walletkit.modules.evmfee.FeeSettingsInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SendEvmTransactionView
import io.horizontalsystems.walletkit.modules.walletconnect.WCDelegate
import io.horizontalsystems.walletkit.modules.walletconnect.request.SessionRequestUI
import io.horizontalsystems.walletkit.modules.walletconnect.request.sendtransaction.DataBlock
import io.horizontalsystems.walletkit.modules.walletconnect.request.sendtransaction.FeeCell
import io.horizontalsystems.walletkit.modules.walletconnect.request.sendtransaction.WalletConnectTransaction
import io.horizontalsystems.walletkit.modules.walletconnect.session.TitleValueCell
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline1_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead_grey
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.info.TextBlock
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCSignEthereumTransactionRequestScreen(
    navigation: HSNavigation,
    logger: AppLogger,
    blockchainType: BlockchainType,
    transaction: WalletConnectTransaction,
    sessionRequestUI: SessionRequestUI.Content,
) {
    val viewModel = viewModel<WCSignEthereumTransactionRequestViewModel>(
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
                        navigation.slideFromBottom(
                            FeeSettingsInfoSheet(FeeSettingsInfoSheet.Input(feeText, feeInfoText))
                        )
                    },
                    onCopy = {
                        snackbarActions.showSuccessMessage(it)
                    }
                )

                // A signed transaction is bound to a chain, so name it here rather than leaving the
                // user to infer it from the dApp.
                sessionRequestUI.chainName?.let { chainName ->
                    TitleValueCell(stringResource(R.string.Balance_Network), chainName)
                }

                TitleValueCell(
                    stringResource(R.string.Wallet_Title),
                    sessionRequestUI.walletName
                )
                uiState.networkFee?.let { fee ->
                    FeeCell(
                        primaryValue = fee.primary.getFormatted(),
                        secondaryValue = fee.secondary?.getFormatted(),
                        onInfoClick = {
                            navigation.slideFromBottom(
                                FeeSettingsInfoSheet(FeeSettingsInfoSheet.Input(feeText, feeInfoText))
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
                        viewModel.reject(sessionRequestUI.topic, sessionRequestUI.requestId)
                        navigation.removeLastOrNull()
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

                            navigation.removeLastOrNull()
                        }
                    }
                )
            }
        }
    }

    ConfirmTransactionScreen(
        title = stringResource(id = R.string.WalletConnect_SignMessageRequest_Title),
        onClickBack = navigation::removeLastOrNull,
        onClickFeeSettings = null,
        buttonsSlot = {
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Sign),
                onClick = navigation.authorizedAction {
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

                        navigation.removeLastOrNull()
                    }
                }
            )
            VSpacer(16.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Reject),
                onClick = {
                    viewModel.reject(sessionRequestUI.topic, sessionRequestUI.requestId)
                    navigation.removeLastOrNull()
                }
            )
        }
    ) {
        SendEvmTransactionView(
            navigation,
            uiState.sectionViewItems,
            uiState.cautions,
            uiState.transactionFields,
            uiState.networkFee,
            StatPage.WalletConnect
        )
    }
}
