package cash.p.terminal.modules.swap.coinselect

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.badge
import cash.p.terminal.core.getInput
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.SwapMainModule.CoinBalanceItem
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.B2
import cash.p.terminal.ui.compose.components.Badge
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.D1
import cash.p.terminal.ui.compose.components.MultitextM1
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.SearchBar
import cash.p.terminal.ui.compose.components.SectionUniversalItem
import cash.p.terminal.ui.compose.components.VSpacer

class SelectSwapCoinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val dex = navController.getInput<SwapMainModule.Dex>()
        if (dex == null) {
            navController.popBackStack()
        } else {
            val viewModel = viewModel<SelectSwapCoinViewModel>(
                factory = SelectSwapCoinModule.Factory(
                    dex
                )
            )
            SelectSwapCoinDialogScreen(
                coinBalanceItems = viewModel.coinItems,
                onSearchTextChanged = viewModel::onEnterQuery,
                onClose = navController::popBackStack
            ) {
                navController.setNavigationResultX(it)
                Handler(Looper.getMainLooper()).postDelayed({
                    navController.popBackStack()
                }, 100)
            }
        }
    }

    private fun closeWithResult(coinBalanceItem: CoinBalanceItem, requestId: Long, navController: NavController) {
        setNavigationResult(
            resultBundleKey, bundleOf(
                requestIdKey to requestId,
                coinBalanceItemResultKey to coinBalanceItem
            )
        )
        Handler(Looper.getMainLooper()).postDelayed({
            navController.popBackStack()
        }, 100)
    }

    companion object {
        const val resultBundleKey = "selectSwapCoinResultKey"
        const val dexKey = "dexKey"
        const val requestIdKey = "requestIdKey"
        const val coinBalanceItemResultKey = "coinBalanceItemResultKey"

        fun prepareParams(requestId: Long, dex: SwapMainModule.Dex) = bundleOf(requestIdKey to requestId, dexKey to dex)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SelectSwapCoinDialogScreen(
    coinBalanceItems: List<CoinBalanceItem>,
    onSearchTextChanged: (String) -> Unit,
    onClose: () -> Unit,
    onClickItem: (CoinBalanceItem) -> Unit
) {
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        SearchBar(
            title = stringResource(R.string.Select_Coins),
            searchHintText = stringResource(R.string.ManageCoins_Search),
            onClose = onClose,
            onSearchTextChanged = onSearchTextChanged
        )

        LazyColumn {
            items(coinBalanceItems) { coinItem ->
                SectionUniversalItem(borderTop = true) {
                    RowUniversal(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = {
                            onClickItem.invoke(coinItem)
                        }
                    ) {
                        CoinImage(
                            iconUrl = coinItem.token.coin.imageUrl,
                            placeholder = coinItem.token.iconPlaceholder,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        MultitextM1(
                            title = {
                                Row {
                                    B2(text = coinItem.token.coin.name)
                                    coinItem.token.badge?.let {
                                        Badge(text = it)
                                    }
                                }
                            },
                            subtitle = { D1(text = coinItem.token.coin.code) }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        MultitextM1(
                            title = {
                                coinItem.balance?.let {
                                    App.numberFormatter.formatCoinFull(
                                        it,
                                        coinItem.token.coin.code,
                                        8
                                    )
                                }?.let {
                                    B2(text = it)
                                }
                            },
                            subtitle = {
                                coinItem.fiatBalanceValue?.let { fiatBalanceValue ->
                                    App.numberFormatter.formatFiatFull(
                                        fiatBalanceValue.value,
                                        fiatBalanceValue.currency.symbol
                                    )
                                }?.let {
                                    D1(
                                        modifier = Modifier.align(Alignment.End),
                                        text = it
                                    )
                                }
                            }
                        )
                    }
                }
            }
            item {
                VSpacer(height = 32.dp)
            }
        }
    }
}
