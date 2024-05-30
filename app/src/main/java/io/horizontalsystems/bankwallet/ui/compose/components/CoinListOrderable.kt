package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CoinListOrderable(
    items: List<MarketViewItem>,
    scrollToTop: Boolean,
    onAddFavorite: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onCoinClick: (String) -> Unit,
    userScrollEnabled: Boolean = true,
    canReorder: Boolean = false,
    showReorderArrows: Boolean = false,
    preItems: LazyListScope.() -> Unit,
    enableManualOrder: () -> Unit,
    onReorder: (from: Int, to: Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var revealedCardId by remember { mutableStateOf<String?>(null) }

    LazyColumn(state = listState, userScrollEnabled = userScrollEnabled) {
        preItems.invoke(this)
        itemsIndexed(items, key = { _, item -> item.coinUid }) { index, item ->
            if (showReorderArrows) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { onReorder.invoke(index, index - 1) }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_big_up_20),
                            tint = ComposeAppTheme.colors.grey,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = { onReorder.invoke(index, index + 1) }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_big_down_20),
                            tint = ComposeAppTheme.colors.grey,
                            contentDescription = null
                        )
                    }

                    MarketCoin(
                        title = item.fullCoin.coin.code,
                        subtitle = item.fullCoin.coin.name,
                        coinIconUrl = item.fullCoin.coin.imageUrl,
                        alternativeCoinIconUrl = item.fullCoin.coin.alternativeImageUrl,
                        coinIconPlaceholder = item.fullCoin.iconPlaceholder,
                        value = item.value,
                        marketDataValue = item.marketDataValue,
                        label = item.rank,
                        advice = item.signal,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (item.favorited) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.jacob)
                            .align(Alignment.CenterEnd)
                            .width(100.dp)
                            .clickable {
                                if (item.favorited) {
                                    onRemoveFavorite(item.coinUid)
                                } else {
                                    onAddFavorite(item.coinUid)
                                }
                                coroutineScope.launch {
                                    delay(200)
                                    revealedCardId = null
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = if (item.favorited) R.drawable.ic_star_off_24 else R.drawable.ic_star_24),
                            tint = ComposeAppTheme.colors.claude,
                            contentDescription = stringResource(if (item.favorited) R.string.CoinPage_Unfavorite else R.string.CoinPage_Favorite),
                        )
                    }
                    DraggableCardSimple(
                        key = item.coinUid,
                        isRevealed = revealedCardId == item.coinUid,
                        cardOffset = 100f,
                        onReveal = {
                            if (revealedCardId != item.coinUid) {
                                revealedCardId = item.coinUid
                            }
                        },
                        onConceal = {
                            revealedCardId = null
                        },
                        content = {
                            MarketCoin(
                                title = item.fullCoin.coin.code,
                                subtitle = item.fullCoin.coin.name,
                                coinIconUrl = item.fullCoin.coin.imageUrl,
                                alternativeCoinIconUrl = item.fullCoin.coin.alternativeImageUrl,
                                coinIconPlaceholder = item.fullCoin.iconPlaceholder,
                                value = item.value,
                                marketDataValue = item.marketDataValue,
                                label = item.rank,
                                advice = item.signal,
                                onClick = { onCoinClick.invoke(item.fullCoin.coin.uid) },
                                onLongClick = {
                                    if (canReorder) {
                                        enableManualOrder()
                                    }
                                }
                            )
                        }
                    )
                    Divider(
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
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