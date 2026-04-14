package com.quantum.wallet.bankwallet.modules.market.topsectors

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatSection
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.core.stats.statPeriod
import com.quantum.wallet.bankwallet.core.stats.statSortType
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.HSSwipeRefresh
import com.quantum.wallet.bankwallet.ui.compose.Select
import com.quantum.wallet.bankwallet.ui.compose.components.AlertGroup
import com.quantum.wallet.bankwallet.ui.compose.components.CoinImage
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderSorting
import com.quantum.wallet.bankwallet.ui.compose.components.ListErrorView
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.marketDataValueComponent
import com.quantum.wallet.bankwallet.uiv3.components.BoxBordered
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellPrimary
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSDropdownButton
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
                            itemsIndexed(uiState.items) { _, item ->
                                BoxBordered(bottom = true) {
                                    TopSectorItem(item) { coinCategory ->
                                        navController.slideFromRight(
                                            R.id.marketSectorFragment,
                                            coinCategory
                                        )
                                    }
                                }
                            }
                            item {
                                VSpacer(height = 140.dp)
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
fun TopSectorItem(
    viewItem: TopSectorViewItem,
    onItemClick: (CoinCategory) -> Unit,
) {
    CellPrimary(
        left = {
            Box(
                modifier = Modifier.width(76.dp)
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
        },
        middle = {
            CellMiddleInfo(
                title = viewItem.coinCategory.name.hs,
            )
        },
        right = {
            CellRightInfo(
                title = viewItem.marketCapValue?.hs ?: "n/a".hs,
                subtitle = marketDataValueComponent(viewItem.changeValue)
            )
        },
        onClick = { onItemClick(viewItem.coinCategory) }
    )
}
