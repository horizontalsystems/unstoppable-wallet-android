package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.StatusCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.TitleValueCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.WCSessionError
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionModule.CONNECTION_LINK_KEY
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionModule.SESSION_TOPIC_KEY
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WC2SessionFragment : BaseFragment() {

    private val viewModel by viewModels<WC2SessionViewModel> {
        WC2SessionModule.Factory(
            arguments?.getString(SESSION_TOPIC_KEY),
            arguments?.getString(CONNECTION_LINK_KEY),
        )
    }

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
                WCSessionPage(
                    findNavController(),
                    viewModel,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.closeLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }

//        viewModel.openRequestLiveEvent.observe(viewLifecycleOwner) {
//            when (it) {
//                is WC1SendEthereumTransactionRequest -> {
//                    baseViewModel.sharedSendEthereumTransactionRequest = it
//
//                    findNavController().slideFromBottom(
//                        R.id.walletConnectMainFragment_to_walletConnectSendEthereumTransactionRequestFragment
//                    )
//                }
//                is WC1SignMessageRequest -> {
//                    baseViewModel.sharedSignMessageRequest = it
//
//                    findNavController().slideFromBottom(
//                        R.id.walletConnectMainFragment_to_walletConnectSignMessageRequestFragment
//                    )
//                }
//            }
//        }

        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            error?.let { HudHelper.showErrorMessage(requireView(), it) }
        }
    }

}

@Composable
fun WCSessionPage(
    navController: NavController,
    viewModel: WC2SessionViewModel,
) {
    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                TranslatableString.ResString(R.string.WalletConnect_Title),
                showSpinner = viewModel.connecting,
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() },
                        enabled = viewModel.closeEnabled
                    )
                )
            )
            if (viewModel.invalidUrlError) {
                WCSessionError(
                    stringResource(R.string.WalletConnect_Error_InvalidUrl),
                    navController
                )
            } else {
                WCSessionListContent(viewModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.WCSessionListContent(
    viewModel: WC2SessionViewModel
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
                painter = rememberImagePainter(
                    data = peerMeta?.icon,
                    builder = {
                        error(R.drawable.coin_placeholder)
                    }
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
        CellSingleLineLawrenceSection(
            listOf(
                { StatusCell(status) },
                {
                    val url = peerMeta?.url?.let { TextHelper.getCleanedUrl(it) } ?: ""
                    TitleValueCell(stringResource(R.string.WalletConnect_Url), url)
                },
                {
                    TitleValueCell(
                        stringResource(R.string.WalletConnect_ActiveWallet), "Wallet1"
                    )
                },
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
    viewModel: WC2SessionViewModel,
    buttonsStates: WCSessionButtonStates
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
