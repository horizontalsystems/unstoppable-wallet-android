package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.ActionsRow
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.launch

@Composable
fun CoinList(
    items: List<MarketViewItem>,
    scrollToTop: Boolean,
    onAddFavorite: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onCoinClick: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var revealedCardId by remember { mutableStateOf<String?>(null) }

    LazyColumn(state = listState) {
        itemsIndexed(items, key = { _, item -> item.coinUid }) { index, item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                ActionsRow(
                    content = {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(if (item.favorited) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.jacob)
                                .width(100.dp)
                                .clickable {
                                    if (item.favorited) {
                                        onRemoveFavorite(item.coinUid)
                                    } else {
                                        onAddFavorite(item.coinUid)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = if (item.favorited) R.drawable.ic_star_off_24 else R.drawable.ic_star_24),
                                tint = ComposeAppTheme.colors.claude,
                                contentDescription = "delete",
                            )
                        }
                    }
                )
                DraggableCardSimple(
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
                            item.fullCoin.coin.name,
                            item.fullCoin.coin.code,
                            item.fullCoin.coin.iconUrl,
                            item.fullCoin.iconPlaceholder,
                            item.coinRate,
                            item.marketDataValue,
                            item.rank
                        ) { onCoinClick.invoke(item.fullCoin.coin.uid) }
                    }
                )
                Divider(
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
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

@Composable
fun ListErrorView(
    errorText: String,
    onClick: () -> Unit
) {
    ScreenMessageWithAction(
        text = errorText,
        icon = R.drawable.ic_sync_error,
        action = Pair(stringResource(id = R.string.Button_Retry), onClick)
    )
}

@Composable
fun ListEmptyView(
    text: String,
    @DrawableRes icon: Int
) {
    ScreenMessageWithAction(text = text, icon = icon, action = null)
}

@Composable
fun ScreenMessageWithAction(
    text: String,
    @DrawableRes icon: Int,
    action: Pair<String, (() -> Unit)>?
) {
    Column {
        Row(Modifier.weight(22f)) {}

        Row(modifier = Modifier.weight(78f), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(ComposeAppTheme.colors.steel20, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(icon),
                        contentDescription = text,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .padding(horizontal = 48.dp),
                    text = text,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2
                )
                action?.let { (name, onClick) ->
                    Spacer(Modifier.height(32.dp))
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .padding(horizontal = 48.dp)
                            .fillMaxWidth()
                            .height(50.dp),
                        title = name,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
fun SortMenu(title: TranslatableString, onClick: () -> Unit) {
    ButtonSecondaryTransparent(
        title = title.getString(),
        iconRight = R.drawable.ic_down_arrow_20,
        onClick = onClick
    )
}

@Composable
fun SortMenu(titleRes: Int, onClick: () -> Unit) {
    SortMenu(TranslatableString.ResString(titleRes), onClick)
}

@Composable
@Deprecated("Use Header component")
fun HeaderWithSorting(
    sortingTitleRes: Int,
    topMarketSelect: Select<TopMarket>?,
    onSelectTopMarket: ((TopMarket) -> Unit)?,
    marketFieldSelect: Select<MarketField>,
    onSelectMarketField: (MarketField) -> Unit,
    onSortMenuClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(sortingTitleRes) {
                    onSortMenuClick()
                }
            }
            topMarketSelect?.let {
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    ButtonSecondaryToggle(
                        select = topMarketSelect,
                        onSelect = onSelectTopMarket ?: {}) //TODO
                }
            }

            Box(modifier = Modifier.padding(start = 8.dp)) {
                ButtonSecondaryToggle(select = marketFieldSelect, onSelect = onSelectMarketField)
            }
        }
        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
    }
}

@Composable
fun TopCloseButton(
    interactionSource: MutableInteractionSource,
    onCloseButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onCloseButtonClick.invoke()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "close icon",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp),
                tint = ComposeAppTheme.colors.jacob
            )
        }
    }
}

@Composable
fun DescriptionCard(title: String, description: String, image: ImageSource) {
    Column {
        Row(
            modifier = Modifier
                .height(108.dp)
                .background(ComposeAppTheme.colors.tyler)
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 8.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = ComposeAppTheme.typography.headline1,
                    color = ComposeAppTheme.colors.oz,
                )
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 6.dp),
                    style = ComposeAppTheme.typography.subhead2,
                    color = ComposeAppTheme.colors.grey,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Image(
                painter = image.painter(),
                contentDescription = "category image",
                modifier = Modifier
                    .fillMaxHeight()
                    .width(76.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RowScope.CategoryCard(
    type: DiscoveryItem,
    onClick: (DiscoveryItem) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .height(128.dp)
            .weight(1f),
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        backgroundColor = ComposeAppTheme.colors.lawrence,
        onClick = {
            onClick.invoke(type)
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (type) {
                DiscoveryItem.TopCoins -> {
                    Image(
                        painter = painterResource(R.drawable.ic_top_coins),
                        contentDescription = "category image",
                        modifier = Modifier
                            .height(108.dp)
                            .width(76.dp)
                            .align(Alignment.TopEnd),
                    )
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.Market_Category_TopCoins),
                            style = ComposeAppTheme.typography.subhead1,
                            color = ComposeAppTheme.colors.oz,
                            maxLines = 1
                        )
                    }
                }
                is DiscoveryItem.Category -> {
                    Image(
                        painter = rememberImagePainter(type.coinCategory.imageUrl),
                        contentDescription = "category image",
                        modifier = Modifier
                            .height(108.dp)
                            .width(76.dp)
                            .align(Alignment.TopEnd),
                    )
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = type.coinCategory.name,
                            style = ComposeAppTheme.typography.subhead1,
                            color = ComposeAppTheme.colors.oz,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewListErrorView() {
    ComposeAppTheme {
        ListErrorView(errorText = "Sync error. Try again") {
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardPreview() {
    ComposeAppTheme {
        Row {
            CategoryCard(DiscoveryItem.TopCoins, { })
        }
    }
}
