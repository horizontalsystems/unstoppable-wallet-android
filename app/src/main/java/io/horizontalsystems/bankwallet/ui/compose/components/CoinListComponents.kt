package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CoinListSlidable(
    listState: LazyListState = rememberLazyListState(),
    items: List<MarketViewItem>,
    scrollToTop: Boolean,
    onAddFavorite: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onCoinClick: (String) -> Unit,
    userScrollEnabled: Boolean = true,
    preItems: LazyListScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var revealedCardId by remember { mutableStateOf<String?>(null) }

    LazyColumn(state = listState, userScrollEnabled = userScrollEnabled) {
        preItems.invoke(this)
        itemsIndexed(items, key = { _, item -> item.coinUid }) { _, item ->
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
                        painter = painterResource(id = if (item.favorited) R.drawable.ic_heart_broke_24 else R.drawable.ic_heart_24),
                        tint = ComposeAppTheme.colors.blade,
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
                            subtitle = item.subtitle,
                            coinIconUrl = item.fullCoin.coin.imageUrl,
                            alternativeCoinIconUrl = item.fullCoin.coin.alternativeImageUrl,
                            coinIconPlaceholder = item.fullCoin.iconPlaceholder,
                            value = item.value,
                            marketDataValue = item.marketDataValue,
                            label = item.rank,
                            advice = item.signal,
                            onClick = { onCoinClick.invoke(item.fullCoin.coin.uid) }
                        )
                    }
                )
                HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
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
fun CoinList(
    listState: LazyListState = rememberLazyListState(),
    items: List<MarketViewItem>,
    scrollToTop: Boolean,
    onAddFavorite: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onCoinClick: (String) -> Unit,
    userScrollEnabled: Boolean = true,
    preItems: LazyListScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(state = listState, userScrollEnabled = userScrollEnabled) {
        preItems.invoke(this)
        itemsIndexed(items, key = { _, item -> item.coinUid }) { _, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 24.dp)
                    .clickable { onCoinClick.invoke(item.fullCoin.coin.uid) }
                    .background(ComposeAppTheme.colors.tyler)
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HsImage(
                    url = item.fullCoin.coin.imageUrl,
                    alternativeUrl = item.fullCoin.coin.alternativeImageUrl,
                    placeholder = item.fullCoin.iconPlaceholder,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    MarketCoinFirstRow(item.fullCoin.coin.code, item.value, item.signal)
                    Spacer(modifier = Modifier.height(3.dp))
                    MarketCoinSecondRow(item.subtitle, item.marketDataValue, item.rank)
                }
                HSpacer(16.dp)
                if (item.favorited) {
                    HsIconButton(
                        modifier = Modifier.size(20.dp),
                        onClick = {
                            onRemoveFavorite(item.coinUid)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_heart_filled_20),
                            contentDescription = "heart icon button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                } else {
                    HsIconButton(
                        modifier = Modifier.size(20.dp),
                        onClick = {
                            onAddFavorite(item.coinUid)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_heart_20),
                            contentDescription = "heart icon button",
                            tint = ComposeAppTheme.colors.grey
                        )
                    }
                }
            }
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
fun ListErrorView(
    errorText: String,
    onClick: () -> Unit
) {
    ListErrorView(
        errorText = errorText,
        icon = R.drawable.ic_sync_error,
        onClick = onClick,
    )
}

@Composable
fun ListErrorView(
    errorText: String,
    @DrawableRes icon: Int = R.drawable.ic_sync_error,
    onClick: () -> Unit
) {
    ScreenMessageWithAction(
        text = errorText,
        icon = icon,
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Button_Retry),
            onClick = onClick
        )
    }
}

@Composable
fun ListEmptyView(
    paddingValues: PaddingValues = PaddingValues(),
    text: String,
    @DrawableRes icon: Int
) {
    ScreenMessageWithAction(
        paddingValues = paddingValues,
        text = text,
        icon = icon
    )
}

@Composable
fun ScreenMessageWithAction(
    text: String,
    @DrawableRes icon: Int,
    paddingValues: PaddingValues = PaddingValues(),
    actionsComposable: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = ComposeAppTheme.colors.raina,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(icon),
                contentDescription = text,
                tint = ComposeAppTheme.colors.grey
            )
        }
        Spacer(Modifier.height(32.dp))
        subhead2_grey(
            modifier = Modifier.padding(horizontal = 48.dp),
            text = text,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        actionsComposable?.let { composable ->
            Spacer(Modifier.height(32.dp))
            composable.invoke()
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
fun TopCloseButton(
    onCloseButtonClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

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
                    color = ComposeAppTheme.colors.leah,
                )
                subhead2_grey(
                    text = description,
                    modifier = Modifier.padding(top = 6.dp),
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
fun CategoryCard(
    type: DiscoveryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.height(128.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        backgroundColor = ComposeAppTheme.colors.lawrence,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (type) {
                is DiscoveryItem.TopCoins -> {
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
                        subhead1_leah(
                            text = stringResource(R.string.Market_Category_TopCoins),
                            maxLines = 1
                        )
                    }
                }

                is DiscoveryItem.Category -> {
                    Crossfade(
                        targetState = type.coinCategory.imageUrl,
                        animationSpec = tween(500),
                        modifier = Modifier
                            .height(108.dp)
                            .width(76.dp)
                            .align(Alignment.TopEnd)
                    ) { imageRes ->
                        Image(
                            painter = rememberAsyncImagePainter(imageRes),
                            contentDescription = "category image",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        subhead1_leah(
                            text = type.coinCategory.name,
                            maxLines = 1
                        )
                        AnimatedVisibility(
                            visible = type.marketData != null,
                        ) {
                            type.marketData?.let { marketData ->
                                Row(modifier = Modifier.padding(top = 8.dp)) {
                                    caption_grey(
                                        text = marketData.marketCap ?: "",
                                        maxLines = 1
                                    )
                                    AnimatedVisibility(
                                        visible = marketData.diff != null,
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = fadeOut() + shrinkHorizontally()
                                    ) {
                                        marketData.diff?.let { diff ->
                                            Text(
                                                text = diffText(diff),
                                                color = diffColor(diff),
                                                style = ComposeAppTheme.typography.caption,
                                                maxLines = 1,
                                                modifier = Modifier.padding(start = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
        CategoryCard(DiscoveryItem.TopCoins, { })
    }
}
