package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.request.SessionRequestUI
import io.horizontalsystems.bankwallet.modules.walletconnect.session.TitleValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCSendEthRequestScreen(
    navController: NavController,
    logger: AppLogger,
    blockchainType: BlockchainType,
    transaction: WalletConnectTransaction,
    sessionRequestUI: SessionRequestUI.Content,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.wcRequestFragment)
    }
    val viewModel = viewModel<WCSendEthereumTransactionRequestViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = WCSendEthereumTransactionRequestViewModel.Factory(
            blockchainType = blockchainType,
            transaction = transaction,
            peerName = sessionRequestUI.peerUI.peerName
        )
    )
    val uiState = viewModel.uiState
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var buttonEnabled by remember { mutableStateOf(true) }
    val doneMessage = stringResource(R.string.Hud_Text_Done)
    val sendingMessage = stringResource(R.string.Send_Sending)
    val feeText = stringResource(id = R.string.Send_Fee)
    val feeInfoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)

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
                    onCopy = { snackbarActions.showSuccessMessage(it) }
                )

                TitleValueCell(
                    stringResource(R.string.Wallet_Title),
                    sessionRequestUI.walletName
                )

                FeeCell(
                    primaryValue = uiState.networkFee?.primary?.getFormatted(),
                    secondaryValue = uiState.networkFee?.secondary?.getFormatted(),
                    onInfoClick = {
                        navController.slideFromBottom(
                            R.id.feeSettingsInfoDialog,
                            FeeSettingsInfoDialog.Input(feeText, feeInfoText)
                        )
                    }
                )
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
                    title = stringResource(R.string.Button_Confirm),
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                    enabled = buttonEnabled && uiState.sendEnabled,
                    onClick = {
                        coroutineScope.launch {
                            buttonEnabled = false
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

                            buttonEnabled = true
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DataBlock(
    sections: List<SectionViewItem>,
    onInfoClick: () -> Unit,
    onCopy: (String) -> Unit
) {
    sections.forEach { section ->
        section.viewItems.forEach { item ->
            when (item) {
                is ViewItem.AmountMulti -> {
                    item.amounts.forEach { amount ->
                        TitleValueCell(
                            stringResource(R.string.WalletConnect_Value),
                            amount.coinAmount
                        )
                    }
                }

                is ViewItem.Value -> TitleValueCell(item.title, item.value)

                is ViewItem.ValueMulti -> {
                    TitleValueCell(item.title, item.primaryValue, item.secondaryValue)
                }

                is ViewItem.Amount -> TitleValueCell(
                    stringResource(R.string.WalletConnect_Value),
                    item.coinAmount
                )

                is ViewItem.AmountWithTitle -> TitleValueCell(
                    item.title,
                    item.coinAmount
                )

                is ViewItem.Address -> CopiableValueCell(item.title, item.value, onCopy)
                is ViewItem.Input -> CopiableValueCell(item.title, item.value, onCopy)
                is ViewItem.Fee -> FeeCell(
                    primaryValue = item.networkFee.primary.getFormatted(),
                    secondaryValue = item.networkFee.secondary?.getFormatted(),
                    onInfoClick = onInfoClick
                )
                else -> {
                    // do nothing
                }
            }
        }
    }
}

@Composable
fun CopiableValueCell(
    title: String,
    value: String,
    onCopy: ((String) -> Unit)? = null
) {
    val shortedValue = value.shorten()
    val copiedMessage = stringResource(R.string.Hud_Text_Copied)

    CellSecondary(
        middle = {
            CellMiddleInfo(
                subtitle = title.hs,
            )
        },
        right = {
            CellRightControlsButtonText(
                text = shortedValue.hs,
                icon = painterResource(id = R.drawable.copy_filled_24),
                iconTint = ComposeAppTheme.colors.leah
            ) {
                TextHelper.copyText(value)
                onCopy?.invoke(copiedMessage)
            }
        },
    )
}

@Composable
fun FeeCell(
    primaryValue: String?,
    secondaryValue: String?,
    onInfoClick: () -> Unit
) {
    CellSecondary(
        middle = {
            CellMiddleInfoTextIcon(
                text = stringResource(R.string.Send_Fee).hs,
                icon = painterResource(R.drawable.info_filled_24),
                iconTint = ComposeAppTheme.colors.grey,
                onIconClick = onInfoClick,
            )
        },
        right = {
            CellRightInfo(
                title = primaryValue?.hs ?: "".hs,
                subtitle = secondaryValue?.hs ?: "".hs
            )
        },
    )
}