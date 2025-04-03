package cash.p.terminal.modules.subscription

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui.compose.components.MessageToSign
import cash.p.terminal.ui.compose.components.ScreenMessageWithAction
import cash.p.terminal.ui_compose.components.TitleAndValueCell
import cash.p.terminal.ui.compose.components.TransactionInfoAddressCell
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.entities.BlockchainType

class ActivateSubscriptionFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        ActivateSubscriptionScreen(navController)
    }

}

@Composable
fun ActivateSubscriptionScreen(navController: NavController) {
    val viewModel = viewModel<ActivateSubscriptionViewModel>(factory = ActivateSubscriptionViewModel.Factory())
    val uiState = viewModel.uiState
    val view = LocalView.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uiState.fetchingTokenSuccess) {
        if (uiState.fetchingTokenSuccess) {
            navController.popBackStack()
        }
    }
    LaunchedEffect(uiState.fetchingTokenError) {
        uiState.fetchingTokenError?.let { error ->
            HudHelper.showErrorMessage(view, error.message ?: error.javaClass.simpleName)
        }
    }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.ActivateSubscription_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(it)
        ) {
            if (uiState.fetchingMessage) {
                Loading()
            }
            uiState.fetchingMessageError?.let { error ->
                if (error is NoSubscription) {
                    ScreenMessageWithAction(
                        text = stringResource(R.string.ActivateSubscription_NoSubscriptionError),
                        icon = R.drawable.ic_sync_error,
                    ) {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(horizontal = 48.dp)
                                .fillMaxWidth(),
                            title = stringResource(R.string.SubscriptionInfo_GetPremium),
                            onClick = {
                                uriHandler.openUri(App.appConfigProvider.analyticsLink)
                            }
                        )
                    }
                } else {
                    ListErrorView(
                        errorText = error.message ?: error.javaClass.simpleName,
                        icon = R.drawable.ic_error_48
                    ) {
                        viewModel.retry()
                    }
                }
            }
            uiState.subscriptionInfo?.let { subscriptionInfo ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    MessageToSignSection(
                        subscriptionInfo.walletName,
                        subscriptionInfo.walletAddress,
                        subscriptionInfo.messageToSign
                    )
                }
            }
            ButtonsGroupWithShade {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    if (uiState.signButtonState.visible) {
                        ButtonPrimaryYellow(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.Button_Sign),
                            enabled = uiState.signButtonState.enabled,
                            onClick = {
                                viewModel.sign()
                            },
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    ButtonPrimaryDefault(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Cancel),
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageToSignSection(
    walletName: String,
    walletAddress: String,
    messageToSign: String
) {
    VSpacer(height = 12.dp)
    CellUniversalLawrenceSection(buildList {
        add {
            TitleAndValueCell(
                stringResource(R.string.ActivateSubscription_Wallet),
                walletName
            )
        }

        add {
            TransactionInfoAddressCell(
                title = stringResource(id = R.string.ActivateSubscription_Address),
                value = walletAddress,
                showAdd = false,
                blockchainType = BlockchainType.Ethereum
            )
        }
    })
    MessageToSign(messageToSign)
}
