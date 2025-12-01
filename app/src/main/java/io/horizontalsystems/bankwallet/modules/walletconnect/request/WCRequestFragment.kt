package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.isEvm
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCEthereumTransaction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCSendEthRequestScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signtransaction.WCSignEthereumTransactionRequestScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.session.TitleValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.MessageToSign
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch

private val logger = AppLogger("wallet-connect request")

class WCRequestFragment : BaseComposableBottomSheetFragment() {

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

                ComposeAppTheme {
                    val wcRequestRouterViewModel =
                        viewModel<WCRequestRouterViewModel>(factory = WCRequestRouterViewModel.Factory())

                    val uiState = wcRequestRouterViewModel.uiState

                    val blockchainType = uiState.blockchainType

                    if (blockchainType == null) {
                        WcRequestError { navController.popBackStack() }
                    } else if (blockchainType.isEvm) {
                        WcRequestEvm(navController)
                    } else if (blockchainType is BlockchainType.Stellar) {
                        WcRequestPreScreen(navController)
                    } else {
                        WcRequestError { navController.popBackStack() }
                    }
                }
            }
        }
    }
}

@Composable
fun WcRequestEvm(navController: NavController) {
    val wcRequestEvmViewModel =
        viewModel<WCRequestEvmViewModel>(factory = WCRequestEvmViewModel.Factory())
    val composableScope = rememberCoroutineScope()
    val view = LocalView.current

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
                    navController,
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
                    navController,
                    logger,
                    blockchainType,
                    transaction,
                    sessionRequestUI
                )
            } else {
                WCNewSignRequestScreen(
                    sessionRequestUI,
                    navController,
                    onAllow = {
                        composableScope.launch {
                            try {
                                wcRequestEvmViewModel.allow()
                                navController.popBackStack()
                            } catch (e: Throwable) {
                                showError(view, e)
                            }
                        }
                        logger.info("allow request")
                    },
                    onDecline = {
                        composableScope.launch {
                            try {
                                wcRequestEvmViewModel.reject()
                                navController.popBackStack()
                            } catch (e: Throwable) {
                                showError(view, e)
                            }
                        }
                        logger.info("decline request")
                    }
                )
            }
        }

        is SessionRequestUI.Initial -> {
            WcRequestError { navController.popBackStack() }
        }
    }
}

private fun showError(view: View, e: Throwable) {
    HudHelper.showErrorMessage(view, e.message ?: e::class.java.simpleName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WcRequestError(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BottomSheetContent(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Close),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Medium,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            )
        },
    ) {
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
            Icon(
                modifier = Modifier.size(60.dp),
                painter = painterResource(R.drawable.ic_warning_filled_24),
                contentDescription = null,
                tint = ComposeAppTheme.colors.lucian
            )
            VSpacer(8.dp)
            VSpacer(16.dp)
            headline1_leah(
                text = stringResource(R.string.WalletConnect_RequestFailed),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            VSpacer(8.dp)
            TextBlock(
                text = stringResource(R.string.WalletConnect_RequestFailedDescription),
                textAlign = TextAlign.Center,
            )
            VSpacer(16.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCNewSignRequestScreen(
    sessionRequestUI: SessionRequestUI.Content,
    navController: NavController,
    onAllow: () -> Unit,
    onDecline: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val messageBottomSheetState = rememberModalBottomSheetState()
    var messageBottomSheet by remember { mutableStateOf<String?>(null) }

    BottomSheetContent(
        onDismissRequest = navController::popBackStack,
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
                MessageCell(
                    message = sessionRequestUI.param,
                    onMessageClick = {
                        messageBottomSheet = it
                    }
                )
                TitleValueCell(
                    stringResource(R.string.Wallet_Title),
                    sessionRequestUI.walletName
                )
            }

            ButtonsGroupHorizontal {
                HSButton(
                    title = stringResource(R.string.Button_Reject),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    modifier = Modifier.weight(1f),
                    onClick = onDecline
                )
                HSButton(
                    title = stringResource(R.string.Button_Confirm),
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onAllow
                )
            }
        }
    }
    messageBottomSheet?.let { message ->
        MessageBottomSheet(
            message = message,
            sheetState = messageBottomSheetState,
            onDismiss = {
                scope.launch { messageBottomSheetState.hide() }.invokeOnCompletion {
                    if (!messageBottomSheetState.isVisible) {
                        messageBottomSheet = null
                    }
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
            CellRightNavigation(
                subtitle = "Unknown".hs,
            )
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
                text = address.hs,
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
