package io.horizontalsystems.bankwallet.modules.market.earn

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.earn.vault.VaultPage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.diffColor
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSDropdownButton
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun MarketEarnScreen(
    navigation: HSNavigation
) {
    val viewModel = viewModel<MarketEarnViewModel>(factory = EarnModule.Factory())
    val uiState = viewModel.uiState
    var openFilterSelector by rememberSaveable { mutableStateOf(false) }
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }

    HSSwipeRefresh(
        refreshing = uiState.isRefreshing,
        topPadding = 44,
        onRefresh = {
            viewModel.refresh()

            stat(
                page = StatPage.Markets,
                event = StatEvent.Refresh,
                section = StatSection.Watchlist
            )
        }
    ) {
        Crossfade(
            targetState = uiState.viewState,
            label = ""
        ) { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }

                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }

                ViewState.Success -> {
                    if (uiState.items.isEmpty()) {
                        ListEmptyView(
                            text = stringResource(R.string.Error),
                            icon = R.drawable.ic_sync_error
                        )
                    } else {
                        VaultList(
                            items = uiState.items,
                            scrollToTop = scrollToTopAfterUpdate,
                            onCoinClick = { viewItem ->
                                val input = VaultPage.Input(
                                    rank = viewItem.rank,
                                    address = viewItem.address,
                                    name = viewItem.name,
                                    tvl = viewItem.tvl,
                                    chain = viewItem.blockchainName,
                                    url = viewItem.url,
                                    holders = viewItem.holders,
                                    assetSymbol = viewItem.assetSymbol,
                                    protocolName = viewItem.protocolName,
                                    assetLogo = viewItem.assetLogo
                                )
                                navigation.slideFromRight(VaultPage(input))
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
                                            title = stringResource(uiState.filterBy.titleResId),
                                            onClick = {
                                                openFilterSelector = true
                                            }
                                        )
                                        HSpacer(width = 12.dp)
                                        HSDropdownButton(
                                            variant = ButtonVariant.Secondary,
                                            title = uiState.sortingByTitle,
                                            onClick = {
                                                openSortingSelector = true
                                            },
                                        )
                                        HSpacer(width = 12.dp)
                                        HSDropdownButton(
                                            variant = ButtonVariant.Secondary,
                                            title = stringResource(uiState.apyPeriod.titleResId),
                                            onClick = {
                                                openPeriodSelector = true
                                            },
                                        )
                                        HSpacer(width = 12.dp)
                                        val forResult = navigation.slideFromRightForResult<VaultBlockchainsSelectorPage.Result>(
                                            {
                                                VaultBlockchainsSelectorPage(
                                                    VaultBlockchainsSelectorPage.Input(
                                                        uiState.selectedBlockchains,
                                                        uiState.blockchains
                                                    )
                                                )
                                            }
                                        ) {
                                            viewModel.onBlockchainsSelected(it.selected)
                                        }

                                        HSDropdownButton(
                                            variant = ButtonVariant.Secondary,
                                            title = uiState.chainSelectorMenuTitle,
                                            onClick = forResult,
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

    if (openFilterSelector) {
        AlertGroup(
            title = stringResource(R.string.Market_Filter_PopupTitle),
            select = Select(uiState.filterBy, viewModel.filterOptions),
            onSelect = { selected ->
                openFilterSelector = false
                scrollToTopAfterUpdate = true
                viewModel.onFilterBySelected(selected)
            },
            onDismiss = {
                openFilterSelector = false
            }
        )
    }
    if (openPeriodSelector) {
        AlertGroup(
            title = stringResource(R.string.CoinPage_Period),
            select = Select(uiState.apyPeriod, viewModel.apyPeriods),
            onSelect = { selected ->
                openPeriodSelector = false
                scrollToTopAfterUpdate = true
                viewModel.onApyPeriodSelected(selected)
            },
            onDismiss = {
                openPeriodSelector = false
            }
        )
    }
    if (openSortingSelector) {
        AlertGroup(
            title = stringResource(R.string.Market_Sort_PopupTitle),
            select = Select(uiState.sortingBy, viewModel.sortingOptions),
            onSelect = { selected ->
                openSortingSelector = false
                scrollToTopAfterUpdate = true
                viewModel.onSortingSelected(selected)
            },
            onDismiss = {
                openSortingSelector = false
            }
        )
    }
}

@Composable
fun VaultList(
    items: List<EarnModule.VaultViewItem>,
    scrollToTop: Boolean,
    onCoinClick: (EarnModule.VaultViewItem) -> Unit,
    preItems: LazyListScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        preItems.invoke(this)
        items(
            items = items,
            key = { item -> item.address + item.protocolName }
        ) { item ->
            VaultItem(
                title = item.assetSymbol,
                subtitle = item.name,
                coinIconUrl = item.assetLogo,
                coinIconPlaceholder = R.drawable.coin_placeholder,
                value = item.apy,
                subvalue = item.tvl,
                label = item.blockchainName,
                onClick = { onCoinClick.invoke(item) },
            )

            HsDivider()
        }
        item {
            VSpacer(140.dp)
        }
        if (scrollToTop) {
            coroutineScope.launch {
                listState.scrollToItem(0)
            }
        }
    }
}

@Composable
private fun VaultItem(
    title: String,
    subtitle: String,
    coinIconUrl: String?,
    alternativeCoinIconUrl: String? = null,
    coinIconPlaceholder: Int,
    value: BigDecimal,
    subvalue: String,
    label: String? = null,
    onClick: (() -> Unit)? = null,
) {
    CellPrimary(
        left = {
            CellLeftImage(
                type = ImageType.Ellipse,
                size = 32,
                painter = rememberAsyncImagePainter(
                    model = coinIconUrl,
                    error = alternativeCoinIconUrl?.let { alternativeUrl ->
                        rememberAsyncImagePainter(
                            model = alternativeUrl,
                            error = painterResource(coinIconPlaceholder)
                        )
                    } ?: painterResource(coinIconPlaceholder)
                ),
            )
        },
        middle = {
            CellMiddleInfo(
                title = title.hs,
                badge = label?.hs,
                subtitle = subtitle.hs,
            )
        },
        right = {
            CellRightInfo(
                title = "APY ${vaultDiffText(value)}".hs(diffColor(value)),
                subtitle = subvalue.hs
            )
        },
        onClick = onClick
    )
}

private fun vaultDiffText(diff: BigDecimal): String {
    return App.numberFormatter.format(diff.abs(), 0, 2, "", "%")
}