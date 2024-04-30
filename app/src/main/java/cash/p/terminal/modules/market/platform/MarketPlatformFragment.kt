package cash.p.terminal.modules.market.platform

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.getInput
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.coin.overview.ui.Chart
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.modules.market.topcoins.SelectorDialogState
import cash.p.terminal.modules.market.topplatforms.Platform
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui.compose.components.ButtonSecondaryToggle
import cash.p.terminal.ui.compose.components.CoinList
import cash.p.terminal.ui.compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.SortMenu
import cash.p.terminal.ui.compose.components.TopCloseButton
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.title3_leah

class MarketPlatformFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {

        val platform = navController.getInput<Platform>()

        if (platform == null) {
            navController.popBackStack()
            return
        }

        val factory = MarketPlatformModule.Factory(platform)

        PlatformScreen(
            factory = factory,
            onCloseButtonClick = { navController.popBackStack() },
            onCoinClick = { coinUid ->
                val arguments = CoinFragment.Input(coinUid, "market_platform")
                navController.slideFromRight(R.id.coinFragment, arguments)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlatformScreen(
    factory: ViewModelProvider.Factory,
    onCloseButtonClick: () -> Unit,
    onCoinClick: (String) -> Unit,
    viewModel: MarketPlatformViewModel = viewModel(factory = factory),
    chartViewModel: ChartViewModel = viewModel(factory = factory),
) {

    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(onCloseButtonClick)

            HSSwipeRefresh(
                refreshing = viewModel.isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                Crossfade(viewModel.viewState) { state ->
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
                            viewModel.viewItems.let { viewItems ->
                                CoinList(
                                    items = viewItems,
                                    scrollToTop = scrollToTopAfterUpdate,
                                    onAddFavorite = { uid ->
                                        viewModel.onAddFavorite(uid)
                                    },
                                    onRemoveFavorite = { uid ->
                                        viewModel.onRemoveFavorite(uid)
                                    },
                                    onCoinClick = onCoinClick,
                                    preItems = {
                                        viewModel.header.let {
                                            item {
                                                HeaderContent(it.title, it.description, it.icon)
                                            }
                                        }
                                        item {
                                            Chart(chartViewModel = chartViewModel)
                                        }
                                        stickyHeader {
                                            HeaderSorting(borderTop = true, borderBottom = true) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    SortMenu(
                                                        viewModel.menu.sortingFieldSelect.selected.titleResId,
                                                        viewModel::showSelectorMenu
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier.padding(
                                                        start = 8.dp,
                                                        end = 16.dp
                                                    )
                                                ) {
                                                    ButtonSecondaryToggle(
                                                        select = viewModel.menu.marketFieldSelect,
                                                        onSelect = viewModel::onSelectMarketField
                                                    )
                                                }
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
        //Dialog
        when (val option = viewModel.selectorDialogState) {
            is SelectorDialogState.Opened -> {
                AlertGroup(
                    R.string.Market_Sort_PopupTitle,
                    option.select,
                    { selected ->
                        viewModel.onSelectSortingField(selected)
                        scrollToTopAfterUpdate = true
                    },
                    { viewModel.onSelectorDialogDismiss() }
                )
            }

            else -> {}
        }
    }
}

@Composable
private fun HeaderContent(title: String, description: String, image: ImageSource) {
    Column {
        Row(
            modifier = Modifier
                .height(100.dp)
                .padding(horizontal = 16.dp)
                .background(ComposeAppTheme.colors.tyler)
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
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
