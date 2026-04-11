package cash.p.terminal.modules.walletconnect.request

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reown.walletkit.client.Wallet
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.sendevmtransaction.SectionView
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun WcRequestScreen(
    navController: NavController,
    sessionRequest: Wallet.Model.SessionRequest,
    wcAction: AbstractWCAction
) {
    val viewModel = viewModel<WCRequestViewModel>(
        factory = WCRequestViewModel.Factory(sessionRequest, wcAction)
    )

    val uiState = viewModel.uiState

    LaunchedEffect(uiState.finish) {
        if (uiState.finish) {
            navController.popBackStack()
        }
    }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = uiState.title.getString(),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close_24,
                        onClick = { navController.popBackStack() }
                    )
                )
            )
        },
        bottomBar = {
            ButtonsGroupWithShade(modifier = Modifier.navigationBarsPadding()) {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ButtonPrimaryYellow(
                        modifier = Modifier.fillMaxWidth(),
                        title = uiState.approveButtonTitle.getString(),
                        onClick = viewModel::approve,
                        enabled = uiState.runnable
                    )
                    VSpacer(16.dp)
                    ButtonPrimaryDefault(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Reject),
                        onClick = viewModel::reject
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .fillMaxWidth()
        ) {
            VSpacer(12.dp)

            uiState.contentItems.forEach { sectionViewItem ->
                SectionView(
                    sectionViewItem.viewItems,
                    navController
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
