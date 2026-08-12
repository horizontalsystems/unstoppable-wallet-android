package io.horizontalsystems.walletkit.modules.walletconnect.request

import android.view.View
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
import androidx.compose.material3.SheetState
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
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.walletconnect.WCDelegate
import io.horizontalsystems.walletkit.modules.walletconnect.request.sendtransaction.WCEthereumTransaction
import io.horizontalsystems.walletkit.modules.walletconnect.request.sendtransaction.WCSendEthRequestScreen
import io.horizontalsystems.walletkit.modules.walletconnect.request.signtransaction.WCSignEthereumTransactionRequestScreen
import io.horizontalsystems.walletkit.modules.walletconnect.session.TitleValueCell
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.MessageToSign
import io.horizontalsystems.walletkit.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline1_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead_grey
import io.horizontalsystems.walletkit.modules.walletconnect.VerificationAlert
import io.horizontalsystems.walletkit.helpers.DateHelper
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logger = AppLogger("wallet-connect request")

@Composable
fun WcRequestEvm(navigation: HSNavigation) {
    val wcRequestEvmViewModel =
        viewModel<WCRequestEvmViewModel>(factory = WCRequestEvmViewModel.Factory())

    when (val sessionRequestUI = wcRequestEvmViewModel.sessionRequestUi) {
        is SessionRequestUI.Content -> {
            if (sessionRequestUI.method == "eth_sendTransaction") {
                val blockchainType = wcRequestEvmViewModel.blockchainType ?: return
                val transaction =
                    try {
                        val ethTransaction = Gson().fromJson(
                            sessionRequestUI.param,
                            WCEthereumTransaction::class.java
                        )
                        ethTransaction.getWCTransaction()
                    } catch (e: Throwable) {
                        return
                    }

                WCSendEthRequestScreen(
                    navigation,
                    logger,
                    blockchainType,
                    transaction,
                    sessionRequestUI
                )
            } else if (sessionRequestUI.method == "eth_signTransaction") {
                val blockchainType = wcRequestEvmViewModel.blockchainType ?: return

                val transaction = try {
                    val ethTransaction = Gson().fromJson(
                        sessionRequestUI.param,
                        WCEthereumTransaction::class.java
                    )
                    ethTransaction.getWCTransaction()
                } catch (e: Throwable) {
                    return
                }

                WCSignEthereumTransactionRequestScreen(
                    navigation,
                    logger,
                    blockchainType,
                    transaction,
                    sessionRequestUI
                )
            } else {
                WCNewSignRequestScreen(
                    sessionRequestUI,
                    navigation,
                    onAllow = { wcRequestEvmViewModel.allow() },
                    onDecline = { wcRequestEvmViewModel.reject() }
                )
            }
        }

        is SessionRequestUI.Initial -> {
            WcRequestError { navigation.removeLastOrNull() }
        }
    }
}

private fun showError(view: View, e: Throwable) {
    HudHelper.showErrorMessage(view, e.message ?: e::class.java.simpleName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCNewSignRequestScreen(
    sessionRequestUI: SessionRequestUI.Content,
    navigation: HSNavigation,
    onAllow: suspend () -> Unit,
    onDecline: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val messageBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var messageBottomSheet by remember { mutableStateOf<String?>(null) }
    val confirmAction = rememberAsyncAction()

    // Animate the bottom sheet out before popping. Removing the entry directly disposes the
    // ModalBottomSheet's window abruptly, which intermittently leaves the sheet content on screen
    // (the dim is removed but the sheet card stays).
    val hideAndPop = {
        scope.launch {
            sheetState.hide()
            navigation.removeLastOrNull()
        }
        Unit
    }

    BottomSheetContent(
        onDismissRequest = {
            // Discard synchronously (avoids the reEmit race), then animate out before popping —
            // same reason as hideAndPop above; popping outright can leave the sheet card on screen.
            // A confirmed sign is already responding (NonCancellable); discarding now would race
            // the pending response, so ignore dismissal until it completes.
            if (!confirmAction.inProgress) {
                WCDelegate.discardActiveSessionRequest()
                hideAndPop()
            }
        },
        sheetState = sheetState
    ) { snackbarActions ->
        Column(
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
                text = stringResource(R.string.WalletConnect_SignMessageRequest_Title),
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
                sessionRequestUI.chainAddress?.let {
                    DomainCell(it, { snackbarActions.showSuccessMessage(it)})
                }

                // For typed data the domain decides what the signature is worth: which contract may
                // use it and on which chain. Surface it instead of leaving it buried in the JSON.
                sessionRequestUI.typedData?.let { typedData ->
                    typedData.primaryType?.let {
                        TitleValueCell(stringResource(R.string.WalletConnect_TypedData_Type), it)
                    }
                    typedData.domainName?.let {
                        TitleValueCell(stringResource(R.string.WalletConnect_Domain), it)
                    }
                    typedData.verifyingContract?.let {
                        TitleValueCell(stringResource(R.string.WalletConnect_TypedData_Contract), it)
                    }

                    // A permit grants an allowance by signature. Nothing goes on chain now, so the
                    // ordinary approval confirmation never runs and these are the only numbers the
                    // user will ever be shown.
                    typedData.permit?.let { permit ->
                        permit.token?.let {
                            TitleValueCell(stringResource(R.string.WalletConnect_Permit_Token), it)
                        }
                        permit.spender?.let {
                            TitleValueCell(stringResource(R.string.Approve_Spender), it)
                        }
                        permit.amount?.let { amount ->
                            TitleValueCell(
                                stringResource(R.string.Approve_Allowance),
                                if (permit.unlimited) {
                                    stringResource(R.string.Swap_Approve_Unlimited)
                                } else {
                                    amount.toString()
                                }
                            )
                        }
                        // Two different things, so they get two different labels: an EIP-2612
                        // allowance outlives the deadline that gated its signature, and saying
                        // otherwise would understate how long the spender keeps access.
                        permit.allowanceExpires?.asTimestampOrNull()?.let {
                            TitleValueCell(
                                stringResource(R.string.WalletConnect_Permit_Expires),
                                DateHelper.getFullDate(it)
                            )
                        }
                        permit.signatureDeadline?.asTimestampOrNull()?.let {
                            TitleValueCell(
                                stringResource(R.string.WalletConnect_Permit_SignatureValid),
                                DateHelper.getFullDate(it)
                            )
                        }
                    }
                }

                MessageCell(
                    message = sessionRequestUI.param,
                    onMessageClick = {
                        messageBottomSheet = it
                    }
                )
                // The screen already names the signing address; the chain it belongs to matters
                // just as much, since the same signature carries different weight per network.
                sessionRequestUI.chainName?.let { chainName ->
                    TitleValueCell(stringResource(R.string.Balance_Network), chainName)
                }

                TitleValueCell(
                    stringResource(R.string.Wallet_Title),
                    sessionRequestUI.walletName
                )
            }

            if (sessionRequestUI.chainIdMismatch) {
                VSpacer(16.dp)
                AlertCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    format = AlertFormat.Structured,
                    type = AlertType.Critical,
                    titleCustom = stringResource(R.string.WalletConnect_TypedData_ChainMismatch_Title),
                    text = stringResource(R.string.WalletConnect_TypedData_ChainMismatch),
                )
            }

            // An unlimited allowance is the shape most drains take, and a permit grants it without
            // a transaction the user could later spot in their history.
            if (sessionRequestUI.typedData?.permit?.unlimited == true) {
                VSpacer(16.dp)
                AlertCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    format = AlertFormat.Structured,
                    type = AlertType.Critical,
                    titleCustom = stringResource(R.string.WalletConnect_Permit_Unlimited_Title),
                    text = stringResource(
                        R.string.WalletConnect_Permit_Unlimited,
                        sessionRequestUI.typedData?.permit?.spender
                            ?: sessionRequestUI.peerUI.peerName
                    ),
                )
            }

            ButtonsGroupHorizontal {
                HSButton(
                    title = stringResource(R.string.Button_Reject),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    modifier = Modifier.weight(1f),
                    enabled = !confirmAction.inProgress,
                    onClick = {
                        logger.info("decline request")
                        onDecline()
                        hideAndPop()
                    }
                )
                HSButton(
                    title = stringResource(R.string.Button_Confirm),
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                    enabled = !confirmAction.inProgress,
                    onClick = {
                        logger.info("allow request")
                        confirmAction.run {
                            try {
                                // Offload the signing (CPU-bound crypto) off the Main thread, but
                                // keep hide()/removeLastOrNull() on Main — they mutate UI state.
                                // NonCancellable: this scope dies with the composition (locking the
                                // app tears the sheet down), and a confirmed sign-and-respond must
                                // not be dropped halfway.
                                withContext(Dispatchers.Default + NonCancellable) { onAllow() }
                                sheetState.hide()
                                navigation.removeLastOrNull()
                            } catch (e: Throwable) {
                                showError(view, e)
                            }
                        }
                    }
                )
            }
        }
    }
    messageBottomSheet?.let { message ->
        MessageBottomSheet(
            message = message,
            sheetState = messageBottomSheetState,
            onDismiss = {
                scope.launch {
                    messageBottomSheetState.hide()
                    messageBottomSheet = null
                }
            }
        )
    }
}

@Composable
fun MessageCell(
    message: String,
    onMessageClick: (String) -> Unit
) {
    CellPrimary(
        middle = {
            CellMiddleInfo(
                subtitle = stringResource(R.string.WalletConnect_Message).hs,
            )
        },
        right = {
            // No summary value: the row opens the full payload, and labelling it "Unknown" claimed
            // the message could not be read while the request type sits right above it.
            CellRightNavigation()
        },
        onClick = {
            onMessageClick.invoke(message)
        }
    )
}

@Composable
fun DomainCell(
    address: String,
    onCopy: (String) -> Unit
) {
    val copyMessage = stringResource(R.string.Hud_Text_Copied)

    CellPrimary(
        middle = {
            CellMiddleInfo(
                title = stringResource(R.string.WalletConnect_Domain).hs,
            )
        },
        right = {
            CellRightControlsButtonText(
                subtitle = address.hs,
                icon = painterResource(id = R.drawable.copy_filled_24),
                iconTint = ComposeAppTheme.colors.leah
            ) {
                TextHelper.copyText(address)
                onCopy.invoke(copyMessage)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBottomSheet(
    message: String,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    BottomSheetContent(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Back),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Medium,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            )
        },
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
            VSpacer(16.dp)
            headline1_leah(
                text = stringResource(R.string.WalletConnect_Message),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            VSpacer(8.dp)
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                MessageToSign(
                    message,
                    { snackbarActions.showSuccessMessage(it) }
                )
            }
            VSpacer(16.dp)
        }
    }
}
