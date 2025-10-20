package io.horizontalsystems.bankwallet.modules.market.platform

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
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
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.market.topcoins.OptionController
import io.horizontalsystems.bankwallet.modules.market.topplatforms.Platform
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinListSlidable
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.coroutines.launch

class MarketPlatformFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {

        withInput<Platform>(navController) { platform ->
            val factory = MarketPlatformModule.Factory(platform)

            PlatformScreen(
                factory = factory,
                platform = platform,
                onCloseButtonClick = { navController.popBackStack() },
                onCoinClick = { coinUid ->
                    val arguments = CoinFragment.Input(coinUid)
                    navController.slideFromRight(R.id.coinFragment, arguments)

                    stat(page = StatPage.TopPlatform, event = StatEvent.OpenCoin(coinUid))
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlatformScreen(
    factory: ViewModelProvider.Factory,
    platform: Platform,
    onCloseButtonClick: () -> Unit,
    onCoinClick: (String) -> Unit,
    viewModel: MarketPlatformViewModel = viewModel(factory = factory),
    chartViewModel: ChartViewModel = viewModel(factory = factory),
) {

    val uiState = viewModel.uiState
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val infoModalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isInfoBottomSheetVisible by remember { mutableStateOf(false) }
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }

    HSScaffold(
        title = platform.name,
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

                    stat(page = StatPage.TopPlatform, event = StatEvent.Refresh)
                }
            ) {
                Crossfade(uiState.viewState, label = "") { state ->
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
                            uiState.viewItems.let { viewItems ->
                                CoinListSlidable(
                                    items = viewItems,
                                    scrollToTop = scrollToTopAfterUpdate,
                                    onAddFavorite = { uid ->
                                        viewModel.onAddFavorite(uid)

                                        stat(
                                            page = StatPage.TopPlatform,
                                            event = StatEvent.AddToWatchlist(uid)
                                        )
                                    },
                                    onRemoveFavorite = { uid ->
                                        viewModel.onRemoveFavorite(uid)

                                        stat(
                                            page = StatPage.TopPlatform,
                                            event = StatEvent.RemoveFromWatchlist(uid)
                                        )
                                    },
                                    onCoinClick = onCoinClick,
                                    preItems = {
                                        item {
                                            Chart(chartViewModel = chartViewModel)
                                        }
                                        stickyHeader {
                                            HeaderSorting(borderTop = true, borderBottom = true) {
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
        }
    }
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
            Select(uiState.sortingField, viewModel.sortingFields),
            { selected ->
                scrollToTopAfterUpdate = true
                viewModel.onSelectSortingField(selected)
                openSortingSelector = false
                stat(
                    page = StatPage.TopPlatform,
                    event = StatEvent.SwitchSortType(selected.statSortType)
                )
            },
            { openSortingSelector = false }
        )
    }
    if (isInfoBottomSheetVisible) {
        InfoBottomSheet(
            icon = R.drawable.ic_info_24,
            title = platform.name,
            description = stringResource(
                R.string.MarketPlatformCoins_PlatformEcosystemDescription,
                platform.name
            ),
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

@Composable
private fun HeaderContent(title: String, description: String, image: ImageSource) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(ComposeAppTheme.colors.tyler)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 16.dp)
                .weight(1f)
        ) {
            title3_leah(
                text = title,
            )
            subhead2_grey(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Image(
            painter = image.painter(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = 24.dp)
                .size(32.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    icon: Int,
    title: String,
    description: String,
    hideBottomSheet: () -> Unit,
    bottomSheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        sheetState = bottomSheetState,
        containerColor = ComposeAppTheme.colors.transparent
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(icon),
            title = title,
            titleColor = ComposeAppTheme.colors.leah,
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
            onCloseClick = hideBottomSheet
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                body_bran(
                    text = description,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )
                VSpacer(56.dp)
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Close),
                    onClick = hideBottomSheet
                )
                VSpacer(32.dp)
            }
        }
    }
}

@Preview
@Composable
fun HeaderContentPreview() {
    ComposeAppTheme {
        HeaderContent(
            "Solana Ecosystem",
            "Market cap of all protocols on the Solana chain",
            ImageSource.Local(R.drawable.logo_ethereum_24)
        )
    }
}
