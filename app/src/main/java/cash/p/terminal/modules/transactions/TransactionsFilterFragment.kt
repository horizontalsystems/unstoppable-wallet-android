package cash.p.terminal.modules.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CellSingleLineLawrenceSection
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah

class TransactionsFilterFragment : BaseComposeFragment() {

    private val viewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment)

    @Composable
    override fun GetContent(navController: NavController) {
        FilterScreen(
            navController,
            viewModel
        )
    }

}


@Composable
fun FilterScreen(
    navController: NavController,
    viewModel: TransactionsViewModel,
) {
    val filterResetEnabled by viewModel.filterResetEnabled.collectAsState()
    val filterCoins by viewModel.filterCoinsLiveData.observeAsState()
    val filterBlockchains by viewModel.filterBlockchainsLiveData.observeAsState()
    val filterHideUnknownTokens = viewModel.filterHideUnknownTokens
    val filterStablecoinsDust = viewModel.filterHideStablecoinsDust

    val filterCoin = filterCoins?.find { it.selected }?.item
    val coinCode = filterCoin?.token?.coin?.code
    val badge = filterCoin?.badge
    val selectedCoinFilterTitle = when {
        badge != null -> "$coinCode ($badge)"
        else -> coinCode
    }

    val filterBlockchain = filterBlockchains?.firstOrNull { it.selected }?.item

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Transactions_Filter),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Reset),
                        enabled = filterResetEnabled,
                        onClick = {
                            viewModel.resetFilters()
                        }
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
                            title = stringResource(R.string.Market_Filter_Blockchains),
                            value = filterBlockchain?.name,
                            onClick = {
                                navController.slideFromRight(R.id.filterBlockchainFragment)
                            }
                        )
                    }
                )
                VSpacer(32.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterDropdownCell(
                            title = stringResource(R.string.Transactions_Coins),
                            value = selectedCoinFilterTitle,
                            onClick = {
                                navController.slideFromRight(R.id.filterCoinFragment)
                            }
                        )
                    }
                )
                VSpacer(32.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterSwitch(
                            title = stringResource(R.string.Transactions_Filter_HideUnknownTokens),
                            enabled = filterHideUnknownTokens,
                            onChecked = { checked ->
                                viewModel.updateFilterHideUnknownTokens(checked)
                            }
                        )
                    }
                )
                VSpacer(32.dp)
                CellSingleLineLawrenceSection(
                    listOf {
                        FilterSwitch(
                            title = stringResource(R.string.Transactions_Filter_StablecoinDustAmount),
                            enabled = filterStablecoinsDust,
                            onChecked = { checked ->
                                viewModel.updateFilterHideStablecoinsDust(checked)
                            }
                        )
                    }
                )
                InfoText(
                    text = stringResource(R.string.Transactions_Filter_StablecoinDustAmount_Description),
                )
                VSpacer(24.dp)
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.Button_Apply),
                    onClick = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterDropdownCell(
    title: String,
    value: String?,
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
                text = value ?: stringResource(R.string.Any),
                maxLines = 1,
                style = ComposeAppTheme.typography.body,
                color = if (value != null) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
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