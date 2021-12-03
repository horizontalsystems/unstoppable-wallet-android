package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.market.*
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.launch

@Composable
fun CoinList(
    items: List<MarketViewItem>,
    scrollToTop: Boolean,
    onCoinClick: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(items) { item ->
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
fun MarketCoin(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    coinRate: String? = null,
    marketDataValue: MarketDataValue? = null,
    label: String? = null,
    onClick: (() -> Unit)? = null
) {
    MultilineClear(
        onClick = onClick,
        borderBottom = true
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketCoinFirstRow(coinName, coinRate)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(coinCode, marketDataValue, label)
        }
    }
}

@Composable
fun MultilineClear(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clickable { onClick?.invoke() }
    ) {
        if (borderTop) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        if (borderBottom) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
fun MarketCoinFirstRow(coinName: String, rate: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = coinName,
            color = ComposeAppTheme.colors.oz,
            style = ComposeAppTheme.typography.body,
            maxLines = 1,
        )
        rate?.let {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = rate,
                color = ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.body,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun MarketCoinSecondRow(
    coinCode: String,
    marketDataValue: MarketDataValue?,
    label: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        label?.let { labelText ->
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ComposeAppTheme.colors.jeremy)
            ) {
                Text(
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 1.dp),
                    text = labelText,
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.microSB,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = coinCode,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2,
            maxLines = 1,
        )
        marketDataValue?.let {
            Spacer(modifier = Modifier.weight(1f))
            MarketDataValueComponent(marketDataValue)
        }
    }
}

@Composable
private fun MarketDataValueComponent(marketDataValue: MarketDataValue) {
    when (marketDataValue) {
        is MarketDataValue.MarketCap -> {
            Row {
                Text(
                    text = "MCap",
                    color = ComposeAppTheme.colors.jacob,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = marketDataValue.value,
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
            }
        }
        is MarketDataValue.Volume -> {
            Row {
                Text(
                    text = "Vol",
                    color = ComposeAppTheme.colors.jacob,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = marketDataValue.value,
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
            }
        }
        is MarketDataValue.Diff -> {
            Text(
                text = RateText(marketDataValue.value),
                color = RateColor(marketDataValue.value),
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
        }
        is MarketDataValue.DiffNew -> {
            Text(
                text = formatValueAsDiff(marketDataValue.value),
                color = diffColor(marketDataValue.value.raw()),
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ListErrorView(
    errorText: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier
                .size(48.dp),
            painter = painterResource(id = R.drawable.ic_attention_24),
            contentDescription = errorText,
            colorFilter = ColorFilter.tint(ComposeAppTheme.colors.grey)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = errorText,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2,
        )
        Spacer(Modifier.height(24.dp))
        onClick?.let {
            ButtonSecondaryDefault(
                modifier = Modifier
                    .width(145.dp)
                    .height(28.dp),
                title = stringResource(id = R.string.Button_Retry),
                onClick = {
                    it.invoke()
                }
            )
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

@ExperimentalCoilApi
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

@ExperimentalCoilApi
@ExperimentalMaterialApi
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
        ListErrorView(errorText = "Sync Error 123")
    }
}

@ExperimentalMaterialApi
@Preview(showBackground = true)
@Composable
fun CardPreview() {
    ComposeAppTheme {
        Row {
            CategoryCard(DiscoveryItem.TopCoins, { })
        }
    }
}
