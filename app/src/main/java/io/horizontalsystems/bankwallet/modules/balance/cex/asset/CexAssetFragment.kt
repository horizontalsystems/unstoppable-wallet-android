package io.horizontalsystems.bankwallet.modules.balance.cex.asset

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
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.cex.BalanceCexViewItem
import io.horizontalsystems.bankwallet.modules.balance.cex.WalletIconCex
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.depositcex.DepositCexFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.parcelable

class CexAssetFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val asset = requireArguments().parcelable<CexAsset>(ASSET_KEY)
        if (asset == null) {
            Toast.makeText(App.instance, "Asset is Null", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val viewModel by viewModels<CexAssetViewModel> { CexAssetViewModel.Factory(asset) }

        ComposeAppTheme {
            CexAssetScreen(
                viewModel,
                findNavController()
            )
        }
    }

    companion object {
        private const val ASSET_KEY = "asset_key"

        fun prepareParams(asset: CexAsset) = bundleOf(
            ASSET_KEY to asset
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
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.PlainString(uiState.title),
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
            color = if (balanceViewItem.primaryValue.dimmed) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.title2R,
            textAlign = TextAlign.Center,
        )
        VSpacer(height = 6.dp)
        Text(
            text = if (balanceViewItem.secondaryValue.visible) balanceViewItem.secondaryValue.value else "*****",
            color = if (balanceViewItem.secondaryValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
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
                text = if (balanceViewItem.coinValueLocked.visible) balanceViewItem.coinValueLocked.value else "*****",
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
                navController.slideFromRight(R.id.depositCexFragment, DepositCexFragment.args(viewItem.cexAsset))
            },
        )
        HSpacer(width = 8.dp)
        ButtonPrimaryCircle(
            icon = R.drawable.ic_chart_24,
            contentDescription = stringResource(R.string.Coin_Info),
            enabled = viewItem.coinUid != null,
            onClick = {
                viewItem.coinUid?.let { coinUid ->
                    navController.slideFromRight(
                        R.id.coinFragment,
                        CoinFragment.prepareParams(coinUid)
                    )
                }
            },
        )
    }
}
