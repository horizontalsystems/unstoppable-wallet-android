package io.horizontalsystems.bankwallet.modules.market.sector

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPeriod
import io.horizontalsystems.bankwallet.core.stats.statSortType
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Chart
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.platform.InfoBottomSheet
import io.horizontalsystems.bankwallet.modules.market.topcoins.OptionController
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.CoinListSlidable
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.CoinCategory
import kotlinx.coroutines.launch

class MarketSectorFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<CoinCategory>(navController) { input ->
            val factory = MarketSectorModule.Factory(input)
            val chartViewModel = viewModel<ChartViewModel>(factory = factory)
            val viewModel = viewModel<MarketSectorViewModel>(factory = factory)

            SectorScreen(
                viewModel = viewModel,
                chartViewModel = chartViewModel,
                onCloseButtonClick = { navController.popBackStack() },
                onCoinClick = { coinUid -> onCoinClick(coinUid, navController) }
            )
        }
    }

    private fun onCoinClick(coinUid: String, navController: NavController) {
        val arguments = CoinFragment.Input(coinUid)

        navController.slideFromRight(R.id.coinFragment, arguments)

        stat(page = StatPage.CoinCategory, event = StatEvent.OpenCoin(coinUid))
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SectorScreen(
    viewModel: MarketSectorViewModel,
    chartViewModel: ChartViewModel,
    onCloseButtonClick: () -> Unit,
    onCoinClick: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    val infoModalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isInfoBottomSheetVisible by remember { mutableStateOf(false) }

    val uiState = viewModel.uiState

    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }

    HSScaffold(
        title = viewModel.categoryName,
        onBack = onCloseButtonClick,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Info_Title),
                icon = R.drawable.ic_info_24,
                onClick = {
                    coroutineScope.launch {
                        infoModalBottomSheetState.show()
                    }
                    isInfoBottomSheetVisible = true
                },
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
                Crossfade(uiState.viewState) { state ->
                    when (state) {
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
                            CoinListSlidable(
                                items = uiState.viewItems,
                                scrollToTop = scrollToTopAfterUpdate,
                                onAddFavorite = { uid -> viewModel.onAddFavorite(uid) },
                                onRemoveFavorite = { uid -> viewModel.onRemoveFavorite(uid) },
                                onCoinClick = onCoinClick,
                                preItems = {
                                    item {
                                        Chart(chartViewModel = chartViewModel)
                                    }
                                    stickyHeader {
                                        HeaderSorting(
                                            borderTop = true,
                                            borderBottom = true
                                        ) {
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
                            if (scrollToTopAfterUpdate) {
                                scrollToTopAfterUpdate = false
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
                    scrollToTopAfterUpdate = true
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
    if (isInfoBottomSheetVisible) {
        InfoBottomSheet(
            icon = R.drawable.ic_info_24,
            title = viewModel.categoryName,
            description = viewModel.categoryDescription,
            bottomSheetState = infoModalBottomSheetState,
            hideBottomSheet = {
                coroutineScope.launch {
                    infoModalBottomSheetState.hide()
                }
                isInfoBottomSheetVisible = false
            }
        )
    }
}

