package com.quantum.wallet.bankwallet.modules.market.earn

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.paidAction
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.slideFromRightForResult
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatSection
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.modules.market.earn.vault.VaultFragment
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.HSSwipeRefresh
import com.quantum.wallet.bankwallet.ui.compose.Select
import com.quantum.wallet.bankwallet.ui.compose.components.AlertGroup
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderSorting
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.compose.components.ListEmptyView
import com.quantum.wallet.bankwallet.ui.compose.components.ListErrorView
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.diffColor
import com.quantum.wallet.bankwallet.ui.compose.components.headline2_leah
import com.quantum.wallet.bankwallet.uiv3.components.BoxBordered
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellLeftImage
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellPrimary
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.ImageType
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSDropdownButton
import com.quantum.wallet.subscriptions.core.TokenInsights
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun MarketEarnScreen(
    navController: NavController
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
                            noPremium = uiState.noPremium,
                            items = uiState.items,
                            blurredItems = uiState.blurredItems,
                            scrollToTop = scrollToTopAfterUpdate,
                            onCoinClick = { viewItem ->
                                val input = VaultFragment.Input(
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
                                navController.paidAction(TokenInsights) {
                                    navController.slideFromRight(R.id.vaultFragment, input)
                                }
                            },
                            onGetPremiumClick = {
                                navController.paidAction(TokenInsights) {
                                    //refresh page
                                }
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
                                        HSDropdownButton(
                                            variant = ButtonVariant.Secondary,
                                            title = uiState.chainSelectorMenuTitle,
                                            onClick = {
                                                navController.slideFromRightForResult<VaultBlockchainsSelectorFragment.Result>(
                                                    R.id.vaultsBlockchainsSelectorFragment,
                                                    VaultBlockchainsSelectorFragment.Input(
                                                        uiState.selectedBlockchains,
                                                        uiState.blockchains
                                                    )
                                                ) {
                                                    viewModel.onBlockchainsSelected(it.selected)
                                                }
                                            },
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
                navController.paidAction(TokenInsights) {
                    scrollToTopAfterUpdate = true
                    viewModel.onFilterBySelected(selected)
                }
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
                navController.paidAction(TokenInsights) {
                    scrollToTopAfterUpdate = true
                    viewModel.onApyPeriodSelected(selected)
                }
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
                navController.paidAction(TokenInsights) {
                    scrollToTopAfterUpdate = true
                    viewModel.onSortingSelected(selected)
                }
            },
            onDismiss = {
                openSortingSelector = false
            }
        )
    }
}

@Composable
fun VaultList(
    noPremium: Boolean,
    items: List<EarnModule.VaultViewItem>,
    blurredItems: List<EarnModule.VaultViewItem>,
    scrollToTop: Boolean,
    onCoinClick: (EarnModule.VaultViewItem) -> Unit,
    onGetPremiumClick: () -> Unit = {},
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
            if (noPremium) {
                PremiumContentMessage(blurredItems) {
                    onGetPremiumClick.invoke()
                }
                VSpacer(70.dp)
            }
        }
        item {
            //Add bottom space only when all items are visible
            //and don't show bottom space when Premium banner is shown
            if (!noPremium) {
                VSpacer(140.dp)
            }
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

@Composable
private fun PremiumContentMessage(
    blurredItems: List<EarnModule.VaultViewItem>,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.blur(
                radiusX = 16.dp,
                radiusY = 16.dp
            )
        ) {
            blurredItems.forEach { item ->
                BoxBordered(bottom = true) {
                    VaultItem(
                        title = item.assetSymbol,
                        subtitle = item.name,
                        coinIconUrl = item.assetLogo,
                        alternativeCoinIconUrl = null,
                        coinIconPlaceholder = R.drawable.coin_placeholder,
                        value = item.apy,
                        subvalue = item.tvl,
                        label = item.blockchainName,
                    )
                }
                HsDivider()
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(R.drawable.icon_lock_48),
                contentDescription = "lock icon",
                tint = ComposeAppTheme.colors.grey
            )

            VSpacer(24.dp)
            headline2_leah(
                modifier = Modifier.padding(horizontal = 48.dp),
                text = stringResource(R.string.Market_Vaults_WantToUnlockPremium),
                textAlign = TextAlign.Center,
            )
            VSpacer(24.dp)
            ButtonPrimaryYellow(
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .fillMaxWidth(),
                title = stringResource(R.string.Market_Vaults_UnlockPremium),
                onClick = onClick
            )
        }
    }
}

private fun vaultDiffText(diff: BigDecimal): String {
    return App.numberFormatter.format(diff.abs(), 0, 2, "", "%")
}