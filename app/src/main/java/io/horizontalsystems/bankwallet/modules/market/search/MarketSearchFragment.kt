package io.horizontalsystems.bankwallet.modules.market.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.category.MarketTopCoinsFragment
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Platform

class MarketSearchFragment : BaseFragment() {

    private val viewModel by viewModels<MarketSearchViewModel> { MarketSearchModule.Factory() }

    @ExperimentalMaterialApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                val coinResult: List<FullCoin> by viewModel.coinResult.observeAsState(listOf())

                MarketSearchScreen(
                    coinResult = coinResult,
                    viewItems = viewModel.viewItems,
                    onBackButtonClick = { findNavController().popBackStack() },
                    onFilterButtonClick = {
                        findNavController().navigate(
                            R.id.marketSearchFragment_to_marketAdvancedSearchFragment,
                            null,
                            navOptions()
                        )
                    },
                    onCoinClick = { coin ->
                        val arguments = CoinFragment.prepareParams(coin.uid)
                        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
                    },
                    onCategoryClick = { viewItemType ->
                        when(viewItemType){
                            //todo differentiate opening of CoinCategory and TopCoins
                            MarketSearchModule.ViewType.TopCoinsType -> {
                                val arguments = MarketTopCoinsFragment.prepareParams("top_coins")
                                findNavController().navigate(R.id.marketSearchFragment_to_marketTopCoinsFragment, arguments, navOptionsFromBottom())
                            }
                            is MarketSearchModule.ViewType.CoinCategoryType -> {
                                val arguments = MarketTopCoinsFragment.prepareParams(viewItemType.coinCategory.uid)
                                findNavController().navigate(R.id.marketSearchFragment_to_marketTopCoinsFragment, arguments, navOptionsFromBottom())
                            }
                        }
                         },
                    onSearchQueryChange = { query -> viewModel.searchByQuery(query) }
                )
            }
        }
    }

}

@ExperimentalMaterialApi
@Composable
fun MarketSearchScreen(
    coinResult: List<FullCoin>,
    viewItems: List<MarketSearchModule.ViewItem>,
    onBackButtonClick: () -> Unit,
    onFilterButtonClick: () -> Unit,
    onCoinClick: (Coin) -> Unit,
    onCategoryClick: (MarketSearchModule.ViewType) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val searchTextState = remember { mutableStateOf(TextFieldValue("")) }

    ComposeAppTheme {
        Column {
            SearchView(
                searchText = searchTextState,
                onSearchTextChange = {
                    searchTextState.value = it
                    onSearchQueryChange.invoke(it.text)
                },
                onRightTextButtonClick = onFilterButtonClick,
                leftIcon = R.drawable.ic_back,
                onBackButtonClick = onBackButtonClick
            )
            if (searchTextState.value.text.isEmpty()) {
                CardsGrid(viewItems, onCategoryClick)
            } else {
                if (searchTextState.value.text.length > 1 && coinResult.isEmpty()) {
                    NoResults()
                } else {
                    MarketSearchResults(coinResult, onCoinClick)
                }
            }
        }
    }
}

@Composable
private fun NoResults() {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.EmptyResults),
            modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.Center),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey,
        )
    }
}

@Composable
fun MarketSearchResults(coinResult: List<FullCoin>, onCoinClick: (Coin) -> Unit) {
    LazyColumn {
        item {
            Divider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
            )
        }
        items(coinResult) { fullCoin ->
            MarketCoin(fullCoin, onCoinClick)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SearchView(
    searchText: MutableState<TextFieldValue>,
    onSearchTextChange: (TextFieldValue) -> Unit,
    onRightTextButtonClick: () -> Unit,
    leftIcon: Int,
    onBackButtonClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onBackButtonClick.invoke()
            }
        ) {
            Icon(
                painter = painterResource(id = leftIcon),
                contentDescription = "back icon",
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .size(24.dp),
                tint = ComposeAppTheme.colors.jacob
            )
        }
        BasicTextField(
            value = searchText.value,
            onValueChange = { value ->
                onSearchTextChange(value)
            },
            modifier = Modifier
                .weight(1f),
            singleLine = true,
            textStyle = ColoredTextStyle(
                color = ComposeAppTheme.colors.oz,
                textStyle = ComposeAppTheme.typography.body
            ),
            decorationBox = { innerTextField ->
                if (searchText.value.text.isEmpty()) {
                    Text(
                        stringResource(R.string.Market_Search_Hint),
                        color = ComposeAppTheme.colors.grey50,
                        style = ComposeAppTheme.typography.body
                    )
                }
                innerTextField()
            },
            cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
        )
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onRightTextButtonClick.invoke()
            }
        ) {
            Text(
                text = stringResource(R.string.Market_Filter),
                style = ComposeAppTheme.typography.headline2,
                color = ComposeAppTheme.colors.jacob,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

}

@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardsGrid(viewItems: List<MarketSearchModule.ViewItem>, onCategoryClick: (MarketSearchModule.ViewType) -> Unit) {
    LazyColumn {
        item {
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
        }
        item {
            Text(
                text = stringResource(R.string.Market_Search_BrowseCategories),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
                maxLines = 1,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
            )
        }
        // Turning the list in a list of lists of two elements each
        items(viewItems.windowed(2, 2, true)) { chunk ->
            Row(modifier = Modifier.padding(horizontal = 10.dp)) {
                CategoryCard(chunk[0].name, chunk[0].imageUrl) { onCategoryClick.invoke(chunk[0].type) }
                if (chunk.size > 1) {
                    CategoryCard(chunk[1].name, chunk[1].imageUrl) { onCategoryClick.invoke(chunk[1].type) }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@ExperimentalCoilApi
@ExperimentalMaterialApi
@Composable
private fun RowScope.CategoryCard(
    title: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .height(128.dp)
            .weight(1f),
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        backgroundColor = ComposeAppTheme.colors.lawrence,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberImagePainter(imageUrl),
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
                    text = title,
                    style = ComposeAppTheme.typography.subhead1,
                    color = ComposeAppTheme.colors.oz,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MarketCoin(fullCoin: FullCoin, onCoinClick: (Coin) -> Unit) {
    val favedState = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .height(60.dp)
            .clickable {
                onCoinClick.invoke(fullCoin.coin)
            }
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinImage(
                iconUrl = fullCoin.coin.iconUrl,
                placeholder = fullCoin.iconPlaceholder,
                modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
            )
            Column(
                modifier = Modifier.padding(end = 16.dp).weight(1f)
            ) {
                Text(
                    text = fullCoin.coin.name,
                    color = ComposeAppTheme.colors.oz,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (true) {
                        Box(
                            modifier = Modifier.padding(end = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ComposeAppTheme.colors.jeremy)
                        ) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    end = 4.dp,
                                    bottom = 1.dp
                                ),
                                text = "label",
                                color = ComposeAppTheme.colors.bran,
                                style = ComposeAppTheme.typography.microSB,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = fullCoin.coin.code,
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2,
                        maxLines = 1,
                    )
                }
            }
            Box(
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    favedState.value = !favedState.value
                }
            ) {
                Image(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    painter = painterResource(R.drawable.ic_star_20),
                    contentDescription = "coin icon",
                    colorFilter = ColorFilter.tint(if (favedState.value) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey),
                )
            }
        }
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MarketCoinPreview() {
    val coin = Coin("ether", "Ethereum", "ETH")
    val fullCoin = FullCoin(
        coin = coin,
        platforms = listOf(Platform(CoinType.fromId("ethereum"), 18, "ethereum"))
    )
    ComposeAppTheme {
        MarketCoin(fullCoin) { }
    }
}

@ExperimentalMaterialApi
@Preview(showBackground = true)
@Composable
fun CardPreview() {
    ComposeAppTheme {
        Row {
            CategoryCard(
                "Top Coins",
                ""
            ) { }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchViewPreview() {
    ComposeAppTheme {
        val textState = remember { mutableStateOf(TextFieldValue("")) }
        SearchView(
            searchText = textState,
            onSearchTextChange = {
                textState.value = it
            },
            onRightTextButtonClick = { },
            leftIcon = R.drawable.ic_back,
            onBackButtonClick = { }
        )
    }
}