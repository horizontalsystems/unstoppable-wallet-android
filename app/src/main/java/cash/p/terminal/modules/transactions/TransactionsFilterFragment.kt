package cash.p.terminal.modules.transactions

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.premiumAction
import cash.p.terminal.core.restartMain
import cash.p.terminal.navigation.popBackStackOrExecute
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.navigation.slideFromRightForResult
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellSingleLineLawrenceSection
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.badge

data class FilterScreenUiState(
    val resetEnabled: Boolean = false,
    val selectedCoinTitle: String? = null,
    val selectedBlockchainName: String? = null,
    val selectedContactName: String? = null,
    val hideSuspiciousTx: Boolean = true,
    val amlCheckEnabled: Boolean = false
)

class TransactionsFilterFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val viewModel: TransactionsViewModel? = try {
            navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }.value
        } catch (e: IllegalStateException) {
            null
        }

        if (viewModel == null) {
            navController.popBackStackOrExecute(R.id.filterCoinFragment, true) { activity?.restartMain() }
            return
        }

        FilterScreen(
            navController,
            viewModel
        )
    }

}


@Composable
private fun FilterScreen(
    navController: NavController,
    viewModel: TransactionsViewModel,
) {
    val filterResetEnabled by viewModel.filterResetEnabled.observeAsState(false)
    val filterCoins by viewModel.filterTokensLiveData.observeAsState()
    val filterBlockchains by viewModel.filterBlockchainsLiveData.observeAsState()
    val filterHideUnknownTokens = viewModel.filterHideSuspiciousTx.observeAsState(true)
    val filterContact by viewModel.filterContactLiveData.observeAsState()
    val uiState = viewModel.uiState
    var showAmlInfoSheet by remember { mutableStateOf(false) }

    val filterCoin = filterCoins?.find { it.selected }?.item
    val coinCode = filterCoin?.token?.coin?.code
    val badge = filterCoin?.token?.badge
    val selectedCoinFilterTitle = when {
        badge != null -> "$coinCode ($badge)"
        else -> coinCode
    }

    val filterBlockchain = filterBlockchains?.firstOrNull { it.selected }?.item

    FilterScreen(
        uiState = FilterScreenUiState(
            resetEnabled = filterResetEnabled,
            selectedCoinTitle = selectedCoinFilterTitle,
            selectedBlockchainName = filterBlockchain?.name,
            selectedContactName = filterContact?.name,
            hideSuspiciousTx = filterHideUnknownTokens.value,
            amlCheckEnabled = uiState.amlCheckEnabled
        ),
        onReset = viewModel::resetFilters,
        onBlockchainClick = { navController.slideFromRight(R.id.filterBlockchainFragment) },
        onCoinClick = { navController.slideFromRight(R.id.filterCoinFragment) },
        onContactClick = {
            navController.slideFromRightForResult<SelectContactFragment.Result>(
                R.id.selectContact,
                SelectContactFragment.Input(filterContact, filterBlockchain?.type)
            ) {
                viewModel.onEnterContact(it.contact)
            }
        },
        onHideSuspiciousTxChange = viewModel::updateFilterHideSuspiciousTx,
        onAmlCheckChange = { enabled ->
            if (enabled) {
                navController.premiumAction {
                    viewModel.setAmlCheckEnabled(true)
                }
            } else {
                viewModel.setAmlCheckEnabled(false)
            }
        },
        onAmlInfoClick = { showAmlInfoSheet = true },
        onApply = navController::navigateUp,
        onBack = navController::navigateUp
    )

    if (showAmlInfoSheet) {
        AmlCheckInfoBottomSheet(
            onPremiumSettingsClick = {
                showAmlInfoSheet = false
                navController.slideFromRight(R.id.premiumSettingsFragment)
            },
            onLaterClick = { showAmlInfoSheet = false },
            onDismiss = { showAmlInfoSheet = false }
        )
    }
}

@Composable
private fun FilterScreen(
    uiState: FilterScreenUiState,
    onReset: () -> Unit,
    onBlockchainClick: () -> Unit,
    onCoinClick: () -> Unit,
    onContactClick: () -> Unit,
    onHideSuspiciousTxChange: (Boolean) -> Unit,
    onAmlCheckChange: (Boolean) -> Unit,
    onAmlInfoClick: () -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Transactions_Filter),
                navigationIcon = {
                    HsBackButton(onClick = onBack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Reset),
                        enabled = uiState.resetEnabled,
                        onClick = onReset
                    )
                )
            )
        }
    ) {
        Column(Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterDropdownCell(
                            title = stringResource(R.string.Transactions_Filter_Blockchain),
                            value = uiState.selectedBlockchainName
                                ?: stringResource(R.string.Transactions_Filter_AllBlockchains),
                            valueColor = if (uiState.selectedBlockchainName != null)
                                ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                            onClick = onBlockchainClick
                        )
                    }
                )
                VSpacer(32.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterDropdownCell(
                            title = stringResource(R.string.Transactions_Filter_Coin),
                            value = uiState.selectedCoinTitle
                                ?: stringResource(R.string.Transactions_Filter_AllCoins),
                            valueColor = if (uiState.selectedCoinTitle != null)
                                ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                            onClick = onCoinClick
                        )
                    }
                )
                VSpacer(32.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterDropdownCell(
                            title = stringResource(R.string.Transactions_Filter_Contacts),
                            value = uiState.selectedContactName
                                ?: stringResource(R.string.Transactions_Filter_AllContacts),
                            valueColor = if (uiState.selectedContactName != null)
                                ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                            onClick = onContactClick
                        )
                    }
                )
                VSpacer(32.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterSwitch(
                            title = stringResource(R.string.Transactions_Filter_HideSuspiciousTx),
                            enabled = uiState.hideSuspiciousTx,
                            onChecked = onHideSuspiciousTxChange
                        )
                    }
                )
                InfoText(
                    text = stringResource(R.string.Transactions_Filter_StablecoinDustAmount_Description),
                )
                VSpacer(56.dp)
                AmlCheckRow(
                    enabled = uiState.amlCheckEnabled,
                    onToggleChange = onAmlCheckChange,
                    onInfoClick = onAmlInfoClick
                )
                Spacer(Modifier.weight(1f))
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.Button_Apply),
                    onClick = onApply,
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun FilterScreenPreview() {
    ComposeAppTheme {
        FilterScreen(
            uiState = FilterScreenUiState(
                resetEnabled = true,
                selectedCoinTitle = "BTC",
                selectedBlockchainName = "Ethereum",
                selectedContactName = null,
                hideSuspiciousTx = true,
                amlCheckEnabled = false
            ),
            onReset = {},
            onBlockchainClick = {},
            onCoinClick = {},
            onContactClick = {},
            onHideSuspiciousTxChange = {},
            onAmlCheckChange = {},
            onAmlInfoClick = {},
            onApply = {},
            onBack = {}
        )
    }
}

@Composable
private fun FilterDropdownCell(
    title: String,
    value: String,
    valueColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable {
                onClick.invoke()
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        body_leah(
            text = title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                maxLines = 1,
                style = ComposeAppTheme.typography.body,
                color = valueColor
            )
            Icon(
                modifier = Modifier.padding(start = 4.dp),
                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
    }
}

@Composable
private fun FilterSwitch(
    title: String,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable { onChecked(!enabled) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        body_leah(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        HsSwitch(
            checked = enabled,
            onCheckedChange = onChecked,
        )
    }
}

