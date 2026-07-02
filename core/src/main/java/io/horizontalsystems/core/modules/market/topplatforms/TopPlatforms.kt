package io.horizontalsystems.core.modules.market.topplatforms

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.stats.StatEvent
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.StatSection
import io.horizontalsystems.core.core.stats.stat
import io.horizontalsystems.core.core.stats.statPeriod
import io.horizontalsystems.core.core.stats.statSortType
import io.horizontalsystems.core.entities.ViewState
import io.horizontalsystems.core.modules.coin.overview.ui.Loading
import io.horizontalsystems.core.modules.market.MarketDataValue
import io.horizontalsystems.core.modules.market.SortingField
import io.horizontalsystems.core.modules.market.TimeDuration
import io.horizontalsystems.core.modules.market.Value
import io.horizontalsystems.core.modules.market.platform.MarketPlatformPage
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.ui.compose.HSSwipeRefresh
import io.horizontalsystems.core.ui.compose.Select
import io.horizontalsystems.core.ui.compose.components.AlertGroup
import io.horizontalsystems.core.ui.compose.components.HSpacer
import io.horizontalsystems.core.ui.compose.components.HeaderSorting
import io.horizontalsystems.core.ui.compose.components.ListErrorView
import io.horizontalsystems.core.ui.compose.components.VSpacer
import io.horizontalsystems.core.ui.compose.components.diffColor
import io.horizontalsystems.core.ui.compose.components.marketDataValueComponent
import io.horizontalsystems.core.uiv3.components.BoxBordered
import io.horizontalsystems.core.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.core.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.core.uiv3.components.cell.CellPrimary
import io.horizontalsystems.core.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.core.uiv3.components.cell.ImageType
import io.horizontalsystems.core.uiv3.components.cell.hs
import io.horizontalsystems.core.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.core.uiv3.components.controls.HSDropdownButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopPlatforms(
    navigation: HSNavigation,
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
            topPadding = 44,
            onRefresh = {
                viewModel.refresh()

                stat(
                    page = StatPage.Markets,
                    event = StatEvent.Refresh,
                    section = StatSection.Platforms
                )
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
                                    navigation.slideFromRight(
                                        MarketPlatformPage(it)
                                    )

                                    stat(
                                        page = StatPage.Markets,
                                        event = StatEvent.OpenPlatform(it.uid),
                                        section = StatSection.Platforms
                                    )
                                },
                                preItems = {
                                    stickyHeader {
                                        HeaderSorting(
                                            borderBottom = true,
                                            backgroundColor = ComposeAppTheme.colors.lawrence
                                        ) {
                                            HSpacer(width = 16.dp)
                                            HSDropdownButton(
                                                variant = ButtonVariant.Secondary,
                                                title = stringResource(uiState.sortingField.titleResId),
                                                onClick = {
                                                    openSortingSelector = true
                                                }
                                            )
                                            HSpacer(width = 12.dp)
                                            HSDropdownButton(
                                                variant = ButtonVariant.Secondary,
                                                title = stringResource(uiState.timePeriod.titleResId),
                                                onClick = {
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
            stringResource(R.string.CoinPage_Period),
            Select(uiState.timePeriod, viewModel.periods),
            { selected ->
                viewModel.onTimePeriodSelect(selected)
                openPeriodSelector = false
                stat(
                    page = StatPage.Markets,
                    event = StatEvent.SwitchPeriod(selected.statPeriod),
                    section = StatSection.Platforms
                )
            },
            { openPeriodSelector = false }
        )
    }
    if (openSortingSelector) {
        AlertGroup(
            stringResource(R.string.Market_Sort_PopupTitle),
            Select(uiState.sortingField, viewModel.sortingOptions),
            { selected ->
                viewModel.onSelectSortingField(selected)
                openSortingSelector = false
                stat(
                    page = StatPage.Markets,
                    event = StatEvent.SwitchSortType(selected.statSortType),
                    section = StatSection.Platforms
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
            BoxBordered(bottom = true) {
                TopPlatformItem(item, onItemClick)
            }
        }
        item {
            VSpacer(height = 140.dp)
        }
    }
}

@Composable
fun TopPlatformItem(item: TopPlatformViewItem, onItemClick: (Platform) -> Unit) {
    CellPrimary(
        left = {
            CellLeftImage(
                type = ImageType.Rectangle,
                size = 32,
                painter = rememberAsyncImagePainter(
                    model =  item.iconUrl,
                    error =  rememberAsyncImagePainter(
                            model =  item.iconUrl,
                            error = painterResource(item.iconPlaceHolder)
                        )
                ),
            )
        },
        middle = {
            CellMiddleInfo(
                title = item.platform.name.hs,
                subtitle = item.subtitle.hs,
                //TODO BadgeWithDiff was used before
                subtitleBadge = item.rank.toString().hs(diffColor(item.rankDiff?.toBigDecimal())),
            )
        },
        right = {
            CellRightInfo(
                title = item.marketCap.hs,
                subtitle = item.marketCapDiff?.let {
                    marketDataValueComponent(MarketDataValue.Diff(Value.Percent(it)))
                }
            )
        },
        onClick = { onItemClick.invoke(item.platform) }
    )
}