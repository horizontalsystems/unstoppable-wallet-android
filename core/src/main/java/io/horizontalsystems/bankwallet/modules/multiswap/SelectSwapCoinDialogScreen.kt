package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottom.BottomSearchBar
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeader
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeaderColored
import io.horizontalsystems.marketkit.models.Token

@Composable
fun SelectSwapCoinDialogScreen(
    title: String,
    uiState: SwapSelectCoinUiState,
    onSearchTextChanged: (String) -> Unit,
    onClose: () -> Unit,
    onRecordRecent: (CoinBalanceItem) -> Unit,
    onClickItem: (CoinBalanceItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    HSScaffold(
        title = title,
        onBack = onClose,
    ) {
        val isSearching = searchQuery.isNotBlank()

        // Tokens picked while the search field is active are remembered as recent.
        val onClick: (CoinBalanceItem) -> Unit = { coinItem ->
            if (isSearchActive) onRecordRecent(coinItem)
            onClickItem(coinItem)
        }

        if (isSearching && uiState.searchResults.isEmpty()) {
            ListEmptyView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeAppTheme.colors.lawrence),
                text = stringResource(R.string.EmptyResults),
                icon = R.drawable.ic_not_available
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                if (isSearching) {
                    itemsIndexed(uiState.searchResults) { index, coinItem ->
                        CoinCell(coinItem, onClick, top = index == 0)
                    }
                } else if (isSearchActive) {
                    // Search active with empty input — show recently picked tokens.
                    if (uiState.recent.isNotEmpty()) {
                        item {
                            SectionHeaderColored(title = stringResource(R.string.Swap_RecentSearch))
                        }
                        itemsIndexed(uiState.recent) { index, coinItem ->
                            CoinCell(coinItem, onClick, top = index == 0)
                        }
                    }
                } else {
                    if (uiState.popular.isNotEmpty()) {
                        item {
                            SectionHeaderColored(
                                title = stringResource(R.string.Swap_PopularTokens)
                            )
                        }
                        item {
                            PopularTokensRow(uiState.popular, onClick)
                        }
                    }
                    if (uiState.yourTokens.isNotEmpty()) {
                        item {
                            SectionHeaderColored(title = stringResource(R.string.Swap_YourTokens))
                        }
                        itemsIndexed(uiState.yourTokens) { index, coinItem ->
                            CoinCell(coinItem, onClick, top = index == 0)
                        }
                    }
                    if (uiState.topTokens.isNotEmpty()) {
                        item {
                            SectionHeaderColored(title = stringResource(R.string.Swap_TopTokens))
                        }
                        itemsIndexed(uiState.topTokens) { index, coinItem ->
                            CoinCell(coinItem, onClick, top = index == 0)
                        }
                    }
                }
                item {
                    VSpacer(height = 88.dp)
                }
            }
        }

        BottomSearchBar(
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            onActiveChange = { isSearchActive = it },
            onSearchQueryChange = { q ->
                searchQuery = q
                onSearchTextChanged(q)
            },
        )
    }
}

@Composable
private fun PopularTokensRow(
    items: List<CoinBalanceItem>,
    onClickItem: (CoinBalanceItem) -> Unit,
) {
    HsDivider()
    LazyRow(
        modifier = Modifier.height(62.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items) { coinItem ->
            PopularTokenChip(coinItem) { onClickItem.invoke(coinItem) }
        }
    }
    HsDivider()
}

@Composable
private fun PopularTokenChip(
    coinItem: CoinBalanceItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ComposeAppTheme.colors.blade)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenIconWithBadge(coinItem.token)
        HSpacer(8.dp)
        subhead2_leah(text = coinItem.token.coin.code)
    }
}

@Composable
private fun TokenIconWithBadge(token: Token) {
    Box(
        modifier = Modifier.size(24.dp),
    ) {
        if (token.type.isNative) {
            CoinImage(
                coin = token.coin,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(ComposeAppTheme.colors.white)
                    .size(20.dp)
            )
        } else {
            CoinImage(
                coin = token.coin,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(ComposeAppTheme.colors.white)
                    .size(20.dp)
            )
            val badgeShape = RoundedCornerShape(2.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 0.5.dp, end = 0.5.dp)
                    .size(11.dp)
                    .clip(badgeShape)
                    .background(ComposeAppTheme.colors.blade)
            )
            HsImage(
                url = token.blockchainType.imageUrl,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(badgeShape),
            )
        }
    }
}

@Composable
private fun CoinCell(
    coinItem: CoinBalanceItem,
    onClickItem: (CoinBalanceItem) -> Unit,
    top: Boolean = false,
) {
    BoxBordered(top = top, bottom = true) {
        CellPrimary(
            left = {
                CoinImage(
                    coin = coinItem.token.coin,
                    modifier = Modifier.size(32.dp)
                )
            },
            middle = {
                CellMiddleInfo(
                    title = coinItem.token.coin.code.hs,
                    badge = coinItem.token.badge?.hs,
                    subtitle = coinItem.token.coin.name.hs,
                )
            },
            right = {
                CellRightInfo(
                    title = coinItem.balance?.let {
                        App.numberFormatter.formatCoinShort(
                            it,
                            coinItem.token.coin.code,
                            8
                        ).hs
                    } ?: "".hs,
                    subtitle = coinItem.fiatBalanceValue?.let { fiatBalanceValue ->
                        App.numberFormatter.formatFiatShort(
                            fiatBalanceValue.value,
                            fiatBalanceValue.currency.symbol,
                            2
                        ).hs
                    } ?: "".hs
                )
            },
            onClick = { onClickItem.invoke(coinItem) },
        )
    }
}
