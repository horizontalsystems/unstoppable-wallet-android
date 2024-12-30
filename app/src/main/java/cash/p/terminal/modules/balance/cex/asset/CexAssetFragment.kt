package cash.p.terminal.modules.balance.cex.asset

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.getInput
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.balance.cex.BalanceCexViewItem
import cash.p.terminal.modules.balance.cex.WalletIconCex
import cash.p.terminal.ui_compose.CoinFragmentInput
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryCircle
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import io.horizontalsystems.core.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper

class CexAssetFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val asset = navController.getInput<CexAsset>()
        if (asset == null) {
            Toast.makeText(App.instance, "Asset is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            return
        }

        val viewModel by viewModels<CexAssetViewModel> { CexAssetViewModel.Factory(asset) }

        CexAssetScreen(
            viewModel,
            navController
        )
    }
}

@Composable
fun CexAssetScreen(
    viewModel: CexAssetViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState

    Scaffold(
        backgroundColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = uiState.title,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
        }
    ) { paddingValues ->

        Column(Modifier.padding(paddingValues)) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = rememberSaveable(
                    uiState.balanceViewItem?.assetId,
                    saver = LazyListState.Saver
                ) {
                    LazyListState()
                }
            ) {

                item {
                    uiState.balanceViewItem?.let {
                        TokenBalanceHeader(
                            balanceViewItem = it,
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenBalanceHeader(
    balanceViewItem: BalanceCexViewItem,
    navController: NavController,
    viewModel: CexAssetViewModel,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(height = (24.dp))
        WalletIconCex(balanceViewItem)
        VSpacer(height = 12.dp)
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        viewModel.toggleBalanceVisibility()
                        HudHelper.vibrate(context)
                    }
                ),
            text = if (balanceViewItem.primaryValue.visible) balanceViewItem.primaryValue.value else "*****",
            color = if (balanceViewItem.primaryValue.dimmed) cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.grey else cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.title2R,
            textAlign = TextAlign.Center,
        )
        VSpacer(height = 6.dp)
        Text(
            text = if (balanceViewItem.secondaryValue.visible) balanceViewItem.secondaryValue.value else "*****",
            color = if (balanceViewItem.secondaryValue.dimmed) cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.grey50 else cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.body,
            maxLines = 1,
        )

        VSpacer(height = 24.dp)
        ButtonsRow(viewItem = balanceViewItem, navController = navController)
        LockedBalanceCell(balanceViewItem)
    }
}

@Composable
private fun LockedBalanceCell(balanceViewItem: BalanceCexViewItem) {
    if (balanceViewItem.coinValueLocked.value != null) {
        VSpacer(height = 8.dp)
        RowUniversal(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
        ) {
            subhead2_grey(
                text = stringResource(R.string.Balance_LockedAmount_Title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = if (balanceViewItem.coinValueLocked.visible) balanceViewItem.coinValueLocked.value!! else "*****",
                color = if (balanceViewItem.coinValueLocked.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
        }
        VSpacer(height = 16.dp)
    }
}


@Composable
private fun ButtonsRow(viewItem: BalanceCexViewItem, navController: NavController) {
    Row(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp)
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.Balance_Withdraw),
            enabled = viewItem.withdrawEnabled,
            onClick = {},
        )
        HSpacer(width = 8.dp)
        ButtonPrimaryDefault(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.Balance_Deposit),
            enabled = viewItem.depositEnabled,
            onClick = {
                navController.slideFromRight(R.id.depositCexFragment, viewItem.cexAsset)
            },
        )
        HSpacer(width = 8.dp)
        ButtonPrimaryCircle(
            icon = R.drawable.ic_chart_24,
            contentDescription = stringResource(R.string.Coin_Info),
            enabled = viewItem.coin != null,
            onClick = {
                viewItem.coin?.let { coin ->
                    navController.slideFromRight(
                        R.id.coinFragment,
                        CoinFragmentInput(coin.uid)
                    )
                }
            },
        )
    }
}
