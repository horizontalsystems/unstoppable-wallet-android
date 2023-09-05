package io.horizontalsystems.bankwallet.modules.walletconnect.session.v1

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.settings.appearance.RowSelect
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectModule
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.DropDownCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.StatusCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.TitleValueCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.WCSessionError
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule.CONNECTION_LINK_KEY
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule.REMOTE_PEER_ID_KEY
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SignMessageRequest
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

class WCSessionFragment : BaseComposeFragment() {

    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.wcSessionFragment) {
        WalletConnectModule.Factory(
            arguments?.getString(REMOTE_PEER_ID_KEY),
            arguments?.getString(CONNECTION_LINK_KEY)
        )
    }

    private val viewModel by viewModels<WCSessionViewModel> {
        WCSessionModule.Factory(baseViewModel.service)
    }

    @Composable
    override fun GetContent() {
        WCSessionPage(
            findNavController(),
            viewModel,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.closeLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }

        viewModel.openRequestLiveEvent.observe(viewLifecycleOwner) { requestWrapper ->
            when (requestWrapper.wC1Request) {
                is WC1SendEthereumTransactionRequest -> {
                    baseViewModel.sharedSendEthereumTransactionRequest = requestWrapper.wC1Request
                    baseViewModel.dAppName = requestWrapper.dAppName

                    findNavController().slideFromBottom(
                        R.id.wcSendEthereumTransactionRequestFragment
                    )
                }
                is WC1SignMessageRequest -> {
                    baseViewModel.sharedSignMessageRequest = requestWrapper.wC1Request
                    baseViewModel.dAppName = requestWrapper.dAppName

                    findNavController().slideFromBottom(
                        R.id.wcSignMessageRequestFragment
                    )
                }
            }
        }

        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            error?.let { HudHelper.showErrorMessage(requireView(), it) }
        }
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WCSessionPage(
    navController: NavController,
    viewModel: WCSessionViewModel,
) {
    val closeEnabled by viewModel.closeEnabledLiveData.observeAsState(false)
    val connecting by viewModel.connectingLiveData.observeAsState(false)
    val invalidStateError = viewModel.invalidStateError
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val blockchainSelect = viewModel.blockchainSelect
    val coroutineScope = rememberCoroutineScope()

    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = modalBottomSheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                BottomSheetHeader(
                    iconPainter = painterResource(R.drawable.ic_blocks_24),
                    title = stringResource(R.string.WalletConnect_Network),
                    onCloseClick = {
                        coroutineScope.launch {
                            modalBottomSheetState.hide()
                        }
                    },
                    iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
                ) {
                    Spacer(Modifier.height(12.dp))
                    CellUniversalLawrenceSection(
                        items= blockchainSelect.options,
                        showFrame = true
                    ) { option ->
                        RowSelect(
                            imageContent = {
                                CoinImage(
                                    iconUrl = option.type.imageUrl,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            text = option.name,
                            selected = option == blockchainSelect.selected
                        ) {
                            coroutineScope.launch {
                                modalBottomSheetState.hide()
                            }
                            viewModel.onSelectBlockchain(option)
                        }
                    }
                    Spacer(Modifier.height(44.dp))
                }
            },
        ) {
            Column(
                modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
            ) {
                AppBar(
                    TranslatableString.ResString(R.string.WalletConnect_Title),
                    showSpinner = connecting,
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = { navController.popBackStack() },
                            enabled = closeEnabled,
                            tint = if (closeEnabled) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey50
                        )
                    )
                )
                if (invalidStateError != null) {
                    WCSessionError(error = stringResource(invalidStateError), navController = navController)
                } else {
                    WCSessionListContent(viewModel) {
                        coroutineScope.launch {
                            modalBottomSheetState.show()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.WCSessionListContent(
    viewModel: WCSessionViewModel,
    onSelectBlockchain: () -> Unit
) {
    val status by viewModel.statusLiveData.observeAsState()
    val peerMeta by viewModel.peerMetaLiveData.observeAsState()
    val buttonsStates by viewModel.buttonStatesLiveData.observeAsState()
    val warningStringRes by viewModel.hintLiveData.observeAsState()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .weight(1f)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                top = 16.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(15.dp)),
                painter = rememberAsyncImagePainter(
                    model = peerMeta?.icon,
                    error = painterResource(R.drawable.coin_placeholder)
                ),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = peerMeta?.name ?: "",
                style = ComposeAppTheme.typography.headline1,
                color = ComposeAppTheme.colors.leah
            )
        }
        CellUniversalLawrenceSection(
            listOf(
                {
                    StatusCell(status)
                },
                {
                    val url = peerMeta?.url?.let { TextHelper.getCleanedUrl(it) } ?: ""
                    TitleValueCell(stringResource(R.string.WalletConnect_Url), url)
                },
                {
                    TitleValueCell(
                        stringResource(R.string.WalletConnect_ActiveWallet),
                        peerMeta?.activeWallet ?: ""
                    )
                },
                {
                    TitleValueCell(
                        stringResource(R.string.WalletConnect_Address),
                        peerMeta?.address ?: ""
                    )
                },
                {
                    DropDownCell(
                        title = stringResource(R.string.WalletConnect_Network),
                        value = viewModel.blockchainSelect.selected.name,
                        enabled = viewModel.blockchainSelectEnabled
                    ) {
                        onSelectBlockchain.invoke()
                    }
                }
            )
        )
        warningStringRes?.let {
            Spacer(Modifier.height(12.dp))
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(it)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
    buttonsStates?.let { ActionButtons(viewModel, it) }
}

@Composable
private fun ActionButtons(
    viewModel: WCSessionViewModel,
    buttonsStates: WCSessionViewModel.ButtonStates
) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        if (buttonsStates.connect.visible) {
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Connect),
                enabled = buttonsStates.connect.enabled,
                onClick = { viewModel.connect() }
            )
        }
        if (buttonsStates.reconnect.visible) {
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Reconnect),
                enabled = buttonsStates.reconnect.enabled,
                onClick = { viewModel.reconnect() }
            )
        }
        if (buttonsStates.disconnect.visible) {
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryRed(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Disconnect),
                enabled = buttonsStates.disconnect.enabled,
                onClick = { viewModel.disconnect() }
            )

        }
        if (buttonsStates.cancel.visible) {
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Cancel),
                onClick = { viewModel.cancel() }
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
