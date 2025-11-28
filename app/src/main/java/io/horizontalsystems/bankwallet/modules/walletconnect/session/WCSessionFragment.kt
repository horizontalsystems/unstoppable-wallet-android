package io.horizontalsystems.bankwallet.modules.walletconnect.session

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInputX
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.NetworksCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.ScamProtectionCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.WalletName
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.AlertCard
import io.horizontalsystems.bankwallet.uiv3.components.AlertFormat
import io.horizontalsystems.bankwallet.uiv3.components.AlertType
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeaderAndy
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionIsolated
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WCSessionFragment : BaseComposeFragment() {

    private val viewModel by viewModels<WCSessionViewModel> {
        val input = arguments?.getInputX<WCSessionModule.Input>()
        WCSessionModule.Factory(input?.sessionTopic)
    }

    @Composable
    override fun GetContent(navController: NavController) {
        WCSessionPage(
            navController,
            viewModel,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.closeLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }

        viewModel.showErrorLiveEvent.observe(viewLifecycleOwner) { error ->
            HudHelper.showErrorMessage(requireView(), error ?: getString(R.string.Error))
        }

        viewModel.showNoInternetErrorLiveEvent.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireView(), getString(R.string.Hud_Text_NoInternet))
        }

    }

}

@Composable
fun WCSessionPage(
    navController: NavController,
    viewModel: WCSessionViewModel,
) {
    val view = LocalView.current
    val uiState = viewModel.uiState
    val buttonsStates = uiState.buttonStates

    uiState.showError?.let { HudHelper.showErrorMessage(view, it) }
    val connectionTitleRes =
        if (uiState.connected) R.string.WalletConnect_ConnectedTo else R.string.WalletConnect_ConnectTo
    val connectedDAppName = stringResource(connectionTitleRes, uiState.peerMeta?.name ?: "")

    HSScaffold(
        title = stringResource(R.string.WalletConnect_Title),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = navController::popBackStack
            )
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        painter = rememberAsyncImagePainter(
                            model = uiState.peerMeta?.icon,
                            error = painterResource(R.drawable.ic_platform_placeholder_24)
                        ),
                        contentDescription = null,
                    )
                }
                headline1_leah(
                    text = connectedDAppName,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                subhead_grey(
                    text = uiState.peerMeta?.url?.let { TextHelper.getCleanedUrl(it) } ?: "",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                VSpacer(24.dp)

                val composableItems = mutableListOf<@Composable () -> Unit>().apply {
                    add {
                        ScamProtectionCell(
                            activated = uiState.hasSubscription,
                            whiteListState = uiState.whiteListState,
                            navController = navController
                        )
                    }
                    add {
                        WalletName(uiState.peerMeta?.accountName ?: "")
                    }
                    add {
                        NetworksCell(
                            blockchainTypes = uiState.blockchainTypes,
                            onClick = {
                                navController.slideFromBottom(
                                    R.id.wcNetworksFragment,
                                    WCNetworksFragment.Input(uiState.blockchainTypes ?: emptyList())
                                )
                            }
                        )
                    }
                }

                val pendingRequests = uiState.pendingRequests
                if (pendingRequests.isNotEmpty()) {
                    SectionHeaderAndy(stringResource(R.string.WalletConnect_PendingRequests))
                    SectionIsolated(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        pendingRequests.forEachIndexed { index, item ->
                            CellPrimary(
                                middle = {
                                    CellMiddleInfo(
                                        title = item.title.hs,
                                        subtitle = item.subtitle.hs,
                                    )
                                },
                                right = {
                                    CellRightNavigation()
                                },
                                onClick = {
                                    viewModel.setRequestToOpen(item.request)
                                    navController.slideFromBottom(R.id.wcRequestFragment)
                                }
                            )
                            if (index < pendingRequests.size - 1) {
                                HsDivider()
                            }
                        }
                    }
                    VSpacer(16.dp)
                }

                AlertMessage(uiState)

                SectionIsolated(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    composableItems.forEachIndexed { index, item ->
                        item.invoke()
                        if (index < composableItems.size - 1) {
                            HsDivider()
                        }
                    }
                }

                if (!uiState.connected) {
                    TextBlock(
                        stringResource(R.string.WalletConnect_ConnectWarning)
                    )
                    VSpacer(12.dp)
                }

                uiState.hint?.let {
                    VSpacer(12.dp)
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = it
                    )
                }
                VSpacer(24.dp)
            }
            buttonsStates?.let { buttons ->
                ActionButtons(
                    buttons = buttons,
                    onConnectClick = { viewModel.connect() },
                    onDisconnectClick = { viewModel.disconnect() },
                    onCancelClick = { viewModel.rejectProposal() }
                )
            }
        }
    }
}

@Composable
private fun AlertMessage(uiState: WCSessionUiState) {
    if (!uiState.hasSubscription) {
        AlertCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            format = AlertFormat.Structured,
            type = AlertType.Caution,
            text = stringResource(R.string.WalletConnect_WhiteListDeactivatedWarning),
        )
    } else if (uiState.whiteListState == WCWhiteListState.NotInWhiteList) {
        AlertCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            format = AlertFormat.Structured,
            type = AlertType.Critical,
            text = stringResource(R.string.WalletConnect_NotInWhiteListAlert),
        )
    }
}

@Composable
private fun ActionButtons(
    buttons: WCSessionButtonStates,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    ButtonsGroupWithShade {
        Column(Modifier.padding(horizontal = 24.dp)) {
            if (buttons.connect.visible) {
                HSButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    title = stringResource(R.string.Button_Connect),
                    enabled = buttons.connect.enabled,
                    onClick = onConnectClick
                )
            }
            if (buttons.disconnect.visible) {
                Spacer(Modifier.height(16.dp))
                HSButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    title = stringResource(R.string.Button_Disconnect),
                    enabled = buttons.disconnect.enabled,
                    onClick = onDisconnectClick
                )
            }
            if (buttons.cancel.visible) {
                Spacer(Modifier.height(16.dp))
                HSButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    style = ButtonStyle.Transparent,
                    size = ButtonSize.Medium,
                    title = stringResource(R.string.Button_Cancel),
                    onClick = onCancelClick
                )
            }
            if (buttons.remove.visible) {
                Spacer(Modifier.height(16.dp))
                HSButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    title = stringResource(R.string.Button_Disconnect),
                    enabled = buttons.disconnect.enabled,
                    onClick = onDisconnectClick
                )
            }
            VSpacer(16.dp)
        }
    }
}
