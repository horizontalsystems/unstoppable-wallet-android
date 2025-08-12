package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.TitleValue
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ValueType
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.caption_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

@Composable
fun WcRequestStellarScreenPre(navController: NavController) {
    val viewModelPre = viewModel<WCRequestStellarPreViewModel>(
        factory = WCRequestStellarPreViewModel.Factory()
    )

    val uiState = viewModelPre.uiState

    if (uiState is DataState.Success) {
        WcRequestStellarScreen(navController, uiState.data.sessionRequest, uiState.data.wcAction)
    } else if (uiState is DataState.Error) {
        ListErrorView(uiState.error.message ?: "Error") { }
    }
}

@Composable
fun WcRequestStellarScreen(
    navController: NavController,
    sessionRequest: Wallet.Model.SessionRequest,
    wcAction: AbstractWCAction
) {
    val viewModel = viewModel<WCRequestStellarViewModel>(
        factory = WCRequestStellarViewModel.Factory(sessionRequest, wcAction)
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
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )
        },
        bottomBar = {
            ButtonsGroupWithShade {
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

            uiState.contentItems.forEach { item ->
                SectionUniversalLawrence {
                    ContentItem(item, navController)
                }
                VSpacer(height = 16.dp)
            }
        }
    }
}

@Composable
private fun ContentItem(item: WCActionContentItem, navController: NavController) {
    when (item) {
        is WCActionContentItem.Fee -> {
            val networkFee = item.networkFee
            DataFieldFee(
                navController,
                networkFee?.primary?.getFormattedPlain() ?: "---",
                networkFee?.secondary?.getFormattedPlain() ?: "---"
            )
        }

        is WCActionContentItem.Paragraph -> {
            caption_leah(
                modifier = Modifier.padding(16.dp),
                text = item.value.getString()
            )
        }
        is WCActionContentItem.Multiline -> {

        }

        is WCActionContentItem.Section -> {
            item.items.forEach {
                ContentItem(it, navController)
            }
        }

        is WCActionContentItem.SingleLine -> {
            TitleValue(
                ViewItem.Value(
                    title = item.title.getString(),
                    value = item.value?.getString() ?: "",
                    type = ValueType.Regular
                )
            )
        }
    }
}
