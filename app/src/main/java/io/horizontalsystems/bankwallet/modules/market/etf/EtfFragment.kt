package cash.p.terminal.modules.market.etf

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonSecondaryWithIcon
import cash.p.terminal.ui.compose.components.DescriptionCard
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.MarketCoinClear
import cash.p.terminal.ui.compose.components.MenuItem

class EtfFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val factory = EtfModule.Factory()
        val viewModel by viewModels<EtfViewModel> { factory }
        EtfPage(viewModel, navController)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EtfPage(
    viewModel: EtfViewModel,
    navController: NavController,
) {
    val uiState = viewModel.uiState
    val title = stringResource(id = R.string.MarketEtf_Title)
    val description = stringResource(id = R.string.MarketEtf_Description)
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = {
                        navController.popBackStack()
                    }
                )
            )
        )

        HSSwipeRefresh(
            refreshing = uiState.isRefreshing,
            onRefresh = {
                viewModel.refresh()
            }
        ) {
            Crossfade(uiState.viewState, label = "") { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        Loading()
                    }

                    is ViewState.Error -> {
                        ListErrorView(
                            stringResource(R.string.SyncError),
                            viewModel::onErrorClick
                        )
                    }

                    ViewState.Success -> {
                        val listState = rememberSaveable(
                            uiState.sortBy,
                            saver = LazyListState.Saver
                        ) {
                            LazyListState()
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(bottom = 32.dp),
                        ) {
                            item {
                                DescriptionCard(
                                    title,
                                    description,
                                    ImageSource.Remote("https://cdn.blocksdecoded.com/category-icons/lending@3x.png")
                                )
                            }
                            //todo Add chart
//                                item {
//                                    Chart(chartViewModel = chartViewModel)
//                                }
                            stickyHeader {
                                HeaderSorting(borderBottom = true, borderTop = true) {
                                    HSpacer(width = 16.dp)
                                    ButtonSecondaryWithIcon(
                                        modifier = Modifier.height(28.dp),
                                        onClick = {
                                            openSortingSelector = true
                                        },
                                        title = stringResource(uiState.sortBy.titleResId),
                                        iconRight = painterResource(R.drawable.ic_down_arrow_20),
                                    )
                                    HSpacer(width = 8.dp)
                                    ButtonSecondaryWithIcon(
                                        modifier = Modifier.height(28.dp),
                                        onClick = {
                                            openPeriodSelector = true
                                        },
                                        title = stringResource(uiState.timeDuration.titleResId),
                                        iconRight = painterResource(R.drawable.ic_down_arrow_20),
                                    )
                                    HSpacer(width = 16.dp)
                                }
                            }
                            items(uiState.viewItems) { viewItem ->
                                MarketCoinClear(
                                    subtitle = viewItem.subtitle,
                                    title = viewItem.title,
                                    coinIconUrl = viewItem.iconUrl,
                                    coinIconPlaceholder = R.drawable.ic_platform_placeholder_24,
                                    value = viewItem.value,
                                    marketDataValue = viewItem.subvalue,
                                    label = viewItem.rank,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (openPeriodSelector) {
        AlertGroup(
            title = R.string.CoinPage_Period,
            select = Select(uiState.timeDuration, viewModel.timeDurations),
            onSelect = { selected ->
                viewModel.onSelectTimeDuration(selected)
                openPeriodSelector = false
            },
            onDismiss = {
                openPeriodSelector = false
            }
        )
    }
    if (openSortingSelector) {
        AlertGroup(
            title = R.string.Market_Sort_PopupTitle,
            select = Select(uiState.sortBy, viewModel.sortByOptions),
            onSelect = { selected ->
                viewModel.onSelectSortBy(selected)
                openSortingSelector = false
            },
            onDismiss = {
                openSortingSelector = false
            }
        )
    }
}