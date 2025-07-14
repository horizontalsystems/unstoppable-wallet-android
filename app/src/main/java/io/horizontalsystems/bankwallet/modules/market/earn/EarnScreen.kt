package io.horizontalsystems.bankwallet.modules.market.earn

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.earn.vault.VaultFragment
import io.horizontalsystems.bankwallet.modules.market.topcoins.OptionController
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryWithIcon
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.diffColor
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun MarketEarnScreen(
    navController: NavController
) {
    val viewModel = viewModel<MarketEarnViewModel>(factory = EarnModule.Factory())
    val uiState = viewModel.uiState
    var openFilterSelector by rememberSaveable { mutableStateOf(false) }
    var openApyPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var openChainSelector by rememberSaveable { mutableStateOf(false) }
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
                            text = stringResource(R.string.Market_Tab_Watchlist_EmptyList),
                            icon = R.drawable.ic_heart_24
                        )
                    } else {
                        VaultList(
                            items = uiState.items,
                            scrollToTop = scrollToTopAfterUpdate,
                            onCoinClick = { viewItem ->
                                val input = VaultFragment.Input(
                                    viewItem.address,
                                    viewItem.name,
                                    viewItem.tvl,
                                    viewItem.chain,
                                    viewItem.url,
                                    viewItem.holders,
                                    viewItem.assetSymbol,
                                    viewItem.protocolName,
                                    viewItem.assetLogo
                                )
                                navController.paidAction(AdvancedSearch) {
                                    navController.slideFromRight(R.id.vaultFragment, input)
                                }
                            },
                            preItems = {
                                stickyHeader {
                                    HeaderSorting(
                                        borderBottom = true,
                                    ) {
                                        HSpacer(width = 16.dp)
                                        OptionController(
                                            uiState.filterBy.titleResId,
                                            onOptionClick = {
                                                navController.paidAction(AdvancedSearch) {
                                                    openFilterSelector = true
                                                }
                                            }
                                        )
                                        HSpacer(width = 12.dp)
                                        ButtonSecondaryWithIcon(
                                            modifier = Modifier.height(28.dp),
                                            onClick = {
                                                navController.paidAction(AdvancedSearch) {
                                                    openApyPeriodSelector = true
                                                }
                                            },
                                            title = "APY (" + stringResource(uiState.apyPeriod.titleResId) + ")",
                                            iconRight = painterResource(R.drawable.ic_down_arrow_20),
                                        )
                                        HSpacer(width = 12.dp)
                                        ButtonSecondaryWithIcon(
                                            modifier = Modifier.height(28.dp),
                                            onClick = {
                                                navController.paidAction(AdvancedSearch) {
                                                    openChainSelector = true
                                                }
                                            },
                                            title = uiState.chainSelected.title.text,
                                            iconRight = painterResource(R.drawable.ic_down_arrow_20),
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
    if (openApyPeriodSelector) {
        AlertGroup(
            title = stringResource(R.string.CoinPage_Period),
            select = Select(uiState.apyPeriod, viewModel.apyPeriods),
            onSelect = { selected ->
                openApyPeriodSelector = false
                scrollToTopAfterUpdate = true
                viewModel.onApyPeriodSelected(selected)
            },
            onDismiss = {
                openApyPeriodSelector = false
            }
        )
    }
    if (openChainSelector) {
        AlertGroup(
            title = stringResource(R.string.Market_Filter_Blockchains),
            select = Select(uiState.chainSelected, viewModel.chainOptions),
            onSelect = { selected ->
                openChainSelector = false
                scrollToTopAfterUpdate = true
                viewModel.onChainSelected(selected)
            },
            onDismiss = {
                openChainSelector = false
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
        itemsIndexed(items, key = { _, item -> item.address + item.protocolName }) { index, item ->
            VaultItem(
                title = item.assetSymbol,
                subtitle = item.name,
                coinIconUrl = item.assetLogo,
                coinIconPlaceholder = R.drawable.coin_placeholder,
                value = item.apy,
                subvalue = "TVL:" + item.tvl,
                label = item.chain,
                onClick = { onCoinClick.invoke(item) },
            )

            HsDivider()
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 24.dp)
            .clickable { onClick?.invoke() }
            .background(ComposeAppTheme.colors.tyler)
            .padding(horizontal = 16.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HsImage(
            url = coinIconUrl,
            alternativeUrl = alternativeCoinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            VaultFirstRow(title, value, label)
            Spacer(modifier = Modifier.height(3.dp))
            VaultSecondRow(subtitle, subvalue)
        }
    }
}

@Composable
fun VaultFirstRow(
    title: String,
    value: BigDecimal,
    badge: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            body_leah(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badge != null) {
                Badge(
                    text = badge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Text(
            text = vaultDiffText(value),
            color = diffColor(value),
            style = ComposeAppTheme.typography.subheadR,
            maxLines = 1,
        )
    }
}

@Composable
private fun VaultSecondRow(
    subtitle: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead2_grey(
            text = subtitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        subhead2_grey(
            text = value,
            maxLines = 1,
        )
    }
}

private fun vaultDiffText(diff: BigDecimal): String {
    return App.numberFormatter.format(diff.abs(), 0, 2, "", "%")
}