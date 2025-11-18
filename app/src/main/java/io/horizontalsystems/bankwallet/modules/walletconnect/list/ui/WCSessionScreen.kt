package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListUiState
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel.ConnectionResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCSessionsScreen(
    navController: NavController,
    deepLinkUri: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showInvalidUrlBottomSheet by remember { mutableStateOf(false) }
    var removeSessionBottomSheet by remember {
        mutableStateOf<WalletConnectListModule.SessionViewItem?>(
            null
        )
    }

    val viewModel = viewModel<WalletConnectListViewModel>(
        factory = WalletConnectListModule.Factory()
    )
    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.setConnectionUri(
                    result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                )
            }
        }

    val uiState by viewModel.uiState.collectAsState(initial = WalletConnectListUiState())

    when (viewModel.connectionResult) {
        ConnectionResult.Error -> {
            LaunchedEffect(viewModel.connectionResult) {
                scope.launch {
                    delay(300)
                    showInvalidUrlBottomSheet = true
                }
            }
            viewModel.onRouteHandled()
        }

        else -> Unit
    }

    LaunchedEffect(Unit) {
        if (deepLinkUri != null) {
            viewModel.setConnectionUri(deepLinkUri)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.refreshList()
    }

    HSScaffold(
        title = stringResource(R.string.DAppConnection_Title),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Info_Title),
                icon = R.drawable.ic_info_24,
                tint = ComposeAppTheme.colors.grey,
                onClick = {
                    FaqManager.showFaqPage(navController, FaqManager.faqPathDefiRisks)
                }
            )
        )
    ) {
        Column {
            Column(modifier = Modifier.weight(1f)) {
                if (uiState.sessionViewItems.isEmpty() && uiState.pairingsNumber == 0) {
                    ListEmptyView(
                        text = stringResource(R.string.WalletConnect_NoConnection),
                        icon = R.drawable.ic_wallet_connet_48
                    )
                } else {
                    WCSessionList(
                        viewModel = viewModel,
                        onSessionDeleteClick = { session ->
                            removeSessionBottomSheet = session
                        },
                        onRequestClick = { requestViewItem ->
                            viewModel.setRequestToOpen(requestViewItem.request)
                            navController.slideFromBottom(R.id.wcRequestFragment)
                        }
                    )
                }
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.WalletConnect_NewConnect),
                    onClick = {
                        qrScannerLauncher.launch(
                            QRScannerActivity.getScanQrIntent(
                                context,
                                true
                            )
                        )
                    }
                )
            }
        }
        if (showInvalidUrlBottomSheet) {
            WCInvalidUrlBottomSheet(
                sheetState = sheetState,
                onConfirm = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showInvalidUrlBottomSheet = false
                        }
                    }

                    qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true))
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showInvalidUrlBottomSheet = false
                        }
                    }
                }
            )
        }
        removeSessionBottomSheet?.let { session ->
            RemoveSessionBottomSheet(
                icon = session.imageUrl,
                name = session.title,
                url = session.url,
                onDismissRequest = {
                    removeSessionBottomSheet = null
                },
                onDisconnect = {
                    viewModel.onDelete(session.sessionTopic)
                    removeSessionBottomSheet = null
                },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WCInvalidUrlBottomSheet(
    sheetState: SheetState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheetContent(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column() {
            BottomSheetHeaderV3(
                image72 = painterResource(R.drawable.warning_filled_24),
                imageTint = ComposeAppTheme.colors.lucian,
                title = stringResource(R.string.WalletConnect_Error_InvalidUrl)
            )
            TextBlock(
                text = stringResource(R.string.WalletConnect_Reconnect_Hint),
                textAlign = TextAlign.Center
            )
            ButtonsGroupHorizontal {
                HSButton(
                    title = stringResource(R.string.Button_Cancel),
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.Secondary,
                    onClick = onDismiss
                )
                HSButton(
                    title = stringResource(R.string.Button_TryAgain),
                    modifier = Modifier.weight(1f),
                    onClick = onConfirm
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveSessionBottomSheet(
    icon: String?,
    name: String,
    url: String?,
    onDismissRequest: () -> Unit,
    onDisconnect: () -> Unit,
) {
    BottomSheetContent(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Disconnect),
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                onClick = onDisconnect
            )
        },
        content = {
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
                            model = icon,
                            error = painterResource(R.drawable.ic_platform_placeholder_24)
                        ),
                        contentDescription = null,
                    )
                }
                VSpacer(16.dp)
                headline1_leah(
                    text = stringResource(R.string.WalletConnect_Disconnect, name),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                VSpacer(8.dp)
                subhead_grey(
                    text = url?.let { TextHelper.getCleanedUrl(it) } ?: "",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                VSpacer(16.dp)
                TextBlock(
                    text = stringResource(R.string.WalletConnect_DisconnectWarning),
                    textAlign = TextAlign.Center,
                )
            }
        }
    )
}
