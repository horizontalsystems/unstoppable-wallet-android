package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WC2RequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.StatusCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.TitleValueCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionModule.SESSION_TOPIC_KEY
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryRed
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WC2SessionFragment : BaseComposeFragment() {

    private val viewModel by viewModels<WC2SessionViewModel> {
        WC2SessionModule.Factory(arguments?.getString(SESSION_TOPIC_KEY))
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

        viewModel.showErrorLiveEvent.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireView(), getString(R.string.Hud_Text_NoInternet))
        }

    }

}

@Composable
fun WCSessionPage(
    navController: NavController,
    viewModel: WC2SessionViewModel,
) {
    val uiState = viewModel.uiState

    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = stringResource(R.string.WalletConnect_Title),
                showSpinner = uiState.connecting,
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() },
                        enabled = uiState.closeEnabled,
                        tint = if (uiState.closeEnabled) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey50
                    )
                )
            )
            WCSessionListContent(navController, viewModel)
        }
    }
}

@Composable
private fun ColumnScope.WCSessionListContent(
    navController: NavController,
    viewModel: WC2SessionViewModel
) {
    val uiState = viewModel.uiState

    val view = LocalView.current
    uiState.showError?.let { HudHelper.showErrorMessage(view, it) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .weight(1f)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                top = 12.dp,
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
                    model = uiState.peerMeta?.icon,
                    error = painterResource(R.drawable.ic_platform_placeholder_24)
                ),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = uiState.peerMeta?.name ?: "",
                style = ComposeAppTheme.typography.headline1,
                color = ComposeAppTheme.colors.leah
            )
        }
        val composableItems = mutableListOf<@Composable () -> Unit>().apply {
            add { StatusCell(uiState.status) }
            add {
                val url = uiState.peerMeta?.url?.let { TextHelper.getCleanedUrl(it) } ?: ""
                TitleValueCell(stringResource(R.string.WalletConnect_Url), url)
            }
            add {
                TitleValueCell(
                    stringResource(R.string.WalletConnect_ActiveWallet),
                    uiState.peerMeta?.accountName ?: ""
                )
            }
        }

        val pendingRequests = uiState.pendingRequests
        if (pendingRequests.isNotEmpty()) {
            HeaderText(text = stringResource(R.string.WalletConnect_PendingRequests))
            CellUniversalLawrenceSection(pendingRequests) { request ->
                RequestCell(viewItem = request) {
                    navController.slideFromBottom(
                        R.id.wc2RequestFragment,
                        WC2RequestFragment.prepareParams(it.requestId)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        CellUniversalLawrenceSection(
            composableItems
        )
        uiState.hint?.let {
            Spacer(Modifier.height(12.dp))
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(it)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
    uiState.buttonStates?.let { ActionButtons(viewModel, it) }
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
        if (buttonsStates.remove.visible) {
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryRed(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Remove),
                onClick = { viewModel.disconnect() }
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
