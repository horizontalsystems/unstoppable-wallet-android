package io.horizontalsystems.bankwallet.modules.market.metricspage

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPage
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinScreen
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Chart
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.DescriptionCard
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoin
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.hsRememberLazyListState
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import kotlinx.serialization.Serializable

@Serializable
data class MetricsPageScreen(val metricsType: MetricsType) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val factory = remember { MetricsPageModule.Factory(metricsType) }
        val chartViewModel = viewModel<ChartViewModel>(factory = factory)
        val viewModel = viewModel<MetricsPageViewModel>(factory = factory)
        MetricsPage(viewModel, chartViewModel, backStack) {
            backStack.add(CoinScreen(it))

            stat(page = metricsType.statPage, event = StatEvent.OpenCoin(it))
        }
    }
}


@Composable
fun MetricsPage(
    viewModel: MetricsPageViewModel,
    chartViewModel: ChartViewModel,
    backStack: NavBackStack<HSScreen>,
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
                    backStack.removeLastOrNull()
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

