package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select

class FavedButton(
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun MultilineClear(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    coinRate: String? = null,
    marketDataValue: MarketDataValue? = null,
    label: String? = null,
    favedButton: FavedButton? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .height(61.dp)
            .clickable { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinImage(
                iconUrl = coinIconUrl,
                placeholder = coinIconPlaceholder,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp)
            )
            Column(
                modifier = Modifier.padding(end = 16.dp).weight(1f)
            ) {
                MarketCoinFirstRow(coinName, coinRate)
                Spacer(modifier = Modifier.height(3.dp))
                MarketCoinSecondRow(coinCode, marketDataValue, label)
            }
            favedButton?.let {
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        favedButton.onClick()
                    }
                ) {
                    Image(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        painter = painterResource(R.drawable.ic_star_20),
                        contentDescription = "coin icon",
                        colorFilter = ColorFilter.tint(if (favedButton.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey),
                    )
                }
            }
        }
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MarketCoinFirstRow(coinName: String, rate: String?) {
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
private fun MarketCoinSecondRow(
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
    }
}

@Composable
fun ListLoadingView() {
    Box(
        modifier = Modifier
            .height(240.dp)
            .fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 2.dp,
        )
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
        Spacer(Modifier.height(20.dp))
        Text(
            text = errorText,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2,
        )
        Spacer(Modifier.height(24.dp))
        ButtonSecondaryDefault(
            modifier = Modifier
                .width(145.dp)
                .height(28.dp),
            title = stringResource(id = R.string.Button_Retry),
            onClick = {
                onClick?.invoke()
            }
        )
    }
}

@Composable
fun SortMenu(titleRes: Int, onClick: () -> Unit) {
    ButtonSecondaryTransparent(
        title = stringResource(titleRes),
        iconRight = R.drawable.ic_down_arrow_20,
        onClick = onClick
    )
}

fun LazyListScope.coinList(
    items: List<MarketViewItem>,
    onCoinClick: (String) -> Unit
) {
    items(items) { item ->
        MultilineClear(
            item.fullCoin.coin.name,
            item.fullCoin.coin.code,
            item.fullCoin.coin.iconUrl,
            item.fullCoin.iconPlaceholder,
            item.coinRate,
            item.marketDataValue,
            item.rank
        ) { onCoinClick.invoke(item.fullCoin.coin.uid) }
    }
}

@Composable
fun HeaderWithSorting(
    sortingFieldSelect: Select<SortingField>,
    onSelectSortingField: (SortingField) -> Unit,
    topMarketSelect: Select<TopMarket>?,
    onSelectTopMarket: ((TopMarket) -> Unit)?,
    marketFieldSelect: Select<MarketField>,
    onSelectMarketField: (MarketField) -> Unit,
    onSortMenuClick: (select: Select<SortingField>, onSelect: ((SortingField) -> Unit)) -> Unit,
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
                SortMenu(sortingFieldSelect.selected.titleResId) {
                    onSortMenuClick(sortingFieldSelect, onSelectSortingField)
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
            modifier = Modifier.height(108.dp).background(ComposeAppTheme.colors.tyler)
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
    type: MarketSearchModule.CardViewItem,
    onClick: (MarketSearchModule.CardViewItem) -> Unit
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
                MarketSearchModule.CardViewItem.MarketTopCoins -> {
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
                is MarketSearchModule.CardViewItem.MarketCoinCategory -> {
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
            CategoryCard(MarketSearchModule.CardViewItem.MarketTopCoins, { })
        }
    }
}