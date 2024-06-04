package io.horizontalsystems.bankwallet.modules.market.topplatforms

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPeriod
import io.horizontalsystems.bankwallet.core.stats.statSortType
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.topcoins.OptionController
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeWithDiff
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinFirstRow
import io.horizontalsystems.bankwallet.ui.compose.components.MarketDataValueComponent
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import java.math.BigDecimal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopPlatforms(
    navController: NavController,
    viewModel: TopPlatformsViewModel = viewModel(
        factory = TopPlatformsModule.Factory(null)
    ),
) {
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }
    val uiState = viewModel.uiState

    Column {
        HSSwipeRefresh(
            refreshing = uiState.isRefreshing,
            onRefresh = {
                viewModel.refresh()

                stat(page = StatPage.Markets, section = StatSection.Platforms, event = StatEvent.Refresh)
            }
        ) {
            Crossfade(uiState.viewState, label = "") { state ->
                when (state) {
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
                        if (uiState.viewItems.isNotEmpty()) {
                            TopPlatformsList(
                                viewItems = uiState.viewItems,
                                sortingField = uiState.sortingField,
                                timeDuration = uiState.timePeriod,
                                onItemClick = {
                                    navController.slideFromRight(
                                        R.id.marketPlatformFragment,
                                        it
                                    )

                                    stat(
                                        page = StatPage.Markets,
                                        section = StatSection.Platforms,
                                        event = StatEvent.OpenPlatform(it.uid)
                                    )
                                },
                                preItems = {
                                    stickyHeader {
                                        HeaderSorting(borderBottom = true) {
                                            HSpacer(width = 16.dp)
                                            OptionController(
                                                uiState.sortingField.titleResId,
                                                onOptionClick = {
                                                    openSortingSelector = true
                                                }
                                            )
                                            HSpacer(width = 12.dp)
                                            OptionController(
                                                uiState.timePeriod.titleResId,
                                                onOptionClick = {
                                                    openPeriodSelector = true
                                                }
                                            )
                                            HSpacer(width = 16.dp)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    //Dialogs
    if (openPeriodSelector) {
        AlertGroup(
            R.string.CoinPage_Period,
            Select(uiState.timePeriod, viewModel.periods),
            { selected ->
                viewModel.onTimePeriodSelect(selected)
                openPeriodSelector = false
                stat(
                    page = StatPage.Markets,
                    section = StatSection.Platforms,
                    event = StatEvent.SwitchPeriod(selected.statPeriod)
                )
            },
            { openPeriodSelector = false }
        )
    }
    if (openSortingSelector) {
        AlertGroup(
            R.string.Market_Sort_PopupTitle,
            Select(uiState.sortingField, viewModel.sortingOptions),
            { selected ->
                viewModel.onSelectSortingField(selected)
                openSortingSelector = false
                stat(
                    page = StatPage.Markets,
                    section = StatSection.Platforms,
                    event = StatEvent.SwitchSortType(selected.statSortType)
                )
            },
            { openSortingSelector = false }
        )
    }
}

@Composable
private fun TopPlatformsList(
    viewItems: List<TopPlatformViewItem>,
    sortingField: SortingField,
    timeDuration: TimeDuration,
    onItemClick: (Platform) -> Unit,
    preItems: LazyListScope.() -> Unit
) {
    val state = rememberSaveable(sortingField, timeDuration, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize()
    ) {
        preItems.invoke(this)
        items(viewItems) { item ->
            TopPlatformItem(item, onItemClick)
        }
    }
}

@Composable
private fun TopPlatformSecondRow(
    subtitle: String,
    marketDataValue: MarketDataValue?,
    rank: String,
    rankDiff: BigDecimal?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgeWithDiff(
            modifier = Modifier.padding(end = 8.dp),
            text = rank,
            diff = rankDiff
        )
        subhead2_grey(
            text = subtitle,
            maxLines = 1,
        )
        marketDataValue?.let {
            Spacer(modifier = Modifier.weight(1f))
            MarketDataValueComponent(marketDataValue)
        }
    }
}

@Composable
fun TopPlatformItem(item: TopPlatformViewItem, onItemClick: (Platform) -> Unit) {
    SectionItemBorderedRowUniversalClear(
        borderBottom = true,
        onClick = { onItemClick(item.platform) }
    ) {
        HsImage(
            url = item.iconUrl,
            placeholder = item.iconPlaceHolder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            MarketCoinFirstRow(item.platform.name, item.marketCap)
            Spacer(modifier = Modifier.height(3.dp))
            TopPlatformSecondRow(
                subtitle = item.subtitle,
                marketDataValue = MarketDataValue.Diff(item.marketCapDiff),
                rank = item.rank.toString(),
                rankDiff = item.rankDiff?.toBigDecimal()
            )
        }
    }
}