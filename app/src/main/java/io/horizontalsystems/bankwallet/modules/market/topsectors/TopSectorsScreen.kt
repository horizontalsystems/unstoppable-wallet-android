package io.horizontalsystems.bankwallet.modules.market.topsectors

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPeriod
import io.horizontalsystems.bankwallet.core.stats.statSortType
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.topcoins.OptionController
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketDataValueComponent
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.marketkit.models.CoinCategory

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopSectorsScreen(
    navController: NavController
) {
    val viewModel = viewModel<TopSectorsViewModel>(factory = TopSectorsViewModel.Factory())
    val uiState = viewModel.uiState
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }

    val state =
        rememberSaveable(uiState.sortingField, uiState.timePeriod, saver = LazyListState.Saver) {
            LazyListState(0, 0)
        }

    Column() {
        HSSwipeRefresh(
            topPadding = 44,
            refreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh
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
                        LazyColumn(
                            state = state,
                            modifier = Modifier.fillMaxSize()
                        ) {
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
                            itemsIndexed(uiState.items) { i, item ->
                                TopSectorItem(
                                    item,
                                    borderBottom = true
                                ) { coinCategory ->
                                    navController.slideFromBottom(
                                        R.id.marketCategoryFragment,
                                        coinCategory
                                    )
                                }
                            }
                            item {
                                VSpacer(height = 32.dp)
                            }
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
fun TopSectorItem(
    viewItem: TopSectorViewItem,
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    onItemClick: (CoinCategory) -> Unit,
) {
    SectionItemBorderedRowUniversalClear(
        borderTop = borderTop,
        borderBottom = borderBottom,
        onClick = { onItemClick(viewItem.coinCategory) }
    ) {
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .width(76.dp)
        ) {
            val iconModifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(ComposeAppTheme.colors.tyler)

            CoinImage(
                coin = viewItem.coin3.coin,
                modifier = iconModifier.align(Alignment.TopEnd)
            )
            CoinImage(
                coin = viewItem.coin2.coin,
                modifier = iconModifier.align(Alignment.TopCenter)
            )
            CoinImage(
                coin = viewItem.coin1.coin,
                modifier = iconModifier.align(Alignment.TopStart)
            )
        }
        body_leah(
            text = viewItem.coinCategory.name,
            modifier = Modifier.weight(1f)
        )
        Column(
            horizontalAlignment = Alignment.End
        ) {
            body_leah(
                text = viewItem.marketCapValue ?: "n/a",
                maxLines = 1,
            )
            VSpacer(3.dp)
            MarketDataValueComponent(viewItem.changeValue)
        }
    }
}
