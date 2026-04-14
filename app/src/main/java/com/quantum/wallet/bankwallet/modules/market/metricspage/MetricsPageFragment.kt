package com.quantum.wallet.bankwallet.modules.market.metricspage

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.alternativeImageUrl
import com.quantum.wallet.bankwallet.core.iconPlaceholder
import com.quantum.wallet.bankwallet.core.imageUrl
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.core.stats.statPage
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.chart.ChartViewModel
import com.quantum.wallet.bankwallet.modules.coin.CoinFragment
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Chart
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.modules.metricchart.MetricsType
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.HSSwipeRefresh
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.DescriptionCard
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderSorting
import com.quantum.wallet.bankwallet.ui.compose.components.ListErrorView
import com.quantum.wallet.bankwallet.ui.compose.components.MarketCoin
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.hsRememberLazyListState
import com.quantum.wallet.bankwallet.uiv3.components.BoxBordered
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonSize
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSButton

class MetricsPageFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<MetricsType>(navController) { metricsType ->
            val factory = MetricsPageModule.Factory(metricsType)
            val chartViewModel by viewModels<ChartViewModel> { factory }
            val viewModel by viewModels<MetricsPageViewModel> { factory }
            MetricsPage(viewModel, chartViewModel, navController) {
                onCoinClick(it, navController)

                stat(page = metricsType.statPage, event = StatEvent.OpenCoin(it))
            }
        }
    }

    private fun onCoinClick(coinUid: String, navController: NavController) {
        val arguments = CoinFragment.Input(coinUid)

        navController.slideFromRight(R.id.coinFragment, arguments)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MetricsPage(
        viewModel: MetricsPageViewModel,
        chartViewModel: ChartViewModel,
        navController: NavController,
        onCoinClick: (String) -> Unit,
    ) {
        val uiState = viewModel.uiState

        HSScaffold(
            title = "",
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = {
                        navController.popBackStack()
                    }
                )
            )
        ) {
            Column(Modifier.navigationBarsPadding()) {
                HSSwipeRefresh(
                    refreshing = uiState.isRefreshing,
                    onRefresh = {
                        viewModel.refresh()
                        chartViewModel.refresh()
                    }
                ) {
                    Crossfade(uiState.viewState, label = "") { viewState ->
                        when (viewState) {
                            ViewState.Loading -> {
                                Loading()
                            }

                            is ViewState.Error -> {
                                ListErrorView(
                                    errorText = stringResource(R.string.SyncError),
                                    onClick = {
                                        viewModel.onErrorClick()
                                        chartViewModel.refresh()
                                    }
                                )
                            }

                            ViewState.Success -> {
                                val listState = hsRememberLazyListState(2, uiState.sortDescending)
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(ComposeAppTheme.colors.lawrence),
                                    state = listState,
                                    contentPadding = PaddingValues(bottom = 32.dp),
                                ) {
                                    item {
                                        uiState.header.let { header ->
                                            DescriptionCard(
                                                header.title,
                                                header.description,
                                                header.icon
                                            )
                                        }
                                    }
                                    item {
                                        Chart(
                                            chartViewModel = chartViewModel,
                                            modifier = Modifier.background(ComposeAppTheme.colors.tyler)
                                        )
                                    }
                                    stickyHeader {
                                        HeaderSorting(
                                            borderBottom = true,
                                            borderTop = true,
                                            backgroundColor = ComposeAppTheme.colors.lawrence
                                        ) {
                                            HSpacer(width = 16.dp)
                                            HSButton(
                                                variant = ButtonVariant.Secondary,
                                                size = ButtonSize.Small,
                                                title = uiState.toggleButtonTitle,
                                                icon = painterResource(if (uiState.sortDescending) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20),
                                                onClick = { viewModel.toggleSorting() }
                                            )
                                            HSpacer(width = 16.dp)
                                        }
                                    }
                                    items(uiState.viewItems) { viewItem ->
                                        BoxBordered(bottom = true) {
                                            MarketCoin(
                                                title = viewItem.fullCoin.coin.code,
                                                subtitle = viewItem.subtitle,
                                                coinIconUrl = viewItem.fullCoin.coin.imageUrl,
                                                alternativeCoinIconUrl = viewItem.fullCoin.coin.alternativeImageUrl,
                                                coinIconPlaceholder = viewItem.fullCoin.iconPlaceholder,
                                                value = viewItem.coinRate,
                                                marketDataValue = viewItem.marketDataValue,
                                                label = viewItem.rank,
                                                onClick = { onCoinClick(viewItem.fullCoin.coin.uid) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

