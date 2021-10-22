package io.horizontalsystems.bankwallet.modules.market.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.typeLabel
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.category.MarketCategoryFragment
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.CoinViewItem
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.ScreenState
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
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
                val screenState by viewModel.screenStateLiveData.observeAsState()
                val searchText: String by viewModel.searchTextLiveData.observeAsState("")

                MarketSearchScreen(
                    searchText = searchText,
                    screenState = screenState,
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
                        when (viewItemType) {
                            MarketSearchModule.CardViewItem.MarketTopCoins -> {
                                findNavController().navigate(
                                    R.id.marketSearchFragment_to_marketTopCoinsFragment,
                                    null,
                                    navOptionsFromBottom()
                                )
                            }
                            is MarketSearchModule.CardViewItem.MarketCoinCategory -> {
                                val args =
                                    MarketCategoryFragment.prepareParams(viewItemType.coinCategory)
                                findNavController().navigate(
                                    R.id.marketCategoryFragment,
                                    args,
                                    navOptions()
                                )
                            }
                        }
                    },
                    onSearchQueryChange = { query -> viewModel.searchByQuery(query) },
                    onFavoriteClick = { favorited, coinUid ->
                        viewModel.onFavoriteClick(favorited, coinUid)
                    }
                )
            }
        }
    }

}

@ExperimentalMaterialApi
@Composable
fun MarketSearchScreen(
    searchText: String,
    screenState: ScreenState?,
    onBackButtonClick: () -> Unit,
    onFilterButtonClick: () -> Unit,
    onCoinClick: (Coin) -> Unit,
    onFavoriteClick: (Boolean, String) -> Unit,
    onCategoryClick: (MarketSearchModule.CardViewItem) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {

    ComposeAppTheme {
        Column {
            SearchView(
                searchText = searchText,
                onSearchTextChange = {
                    onSearchQueryChange.invoke(it)
                },
                onRightTextButtonClick = onFilterButtonClick,
                leftIcon = R.drawable.ic_back,
                onBackButtonClick = onBackButtonClick
            )
            when (screenState) {
                is ScreenState.CardsList -> {
                    CardsGrid(screenState.cards, onCategoryClick)
                }
                ScreenState.EmptySearchResult -> {
                    NoResults()
                }
                is ScreenState.SearchResult -> {
                    MarketSearchResults(
                        screenState.coins,
                        onCoinClick,
                        onFavoriteClick
                    )
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
fun MarketSearchResults(
    coinResult: List<CoinViewItem>,
    onCoinClick: (Coin) -> Unit,
    onFavoriteClick: (Boolean, String) -> Unit
) {
    LazyColumn {
        item {
            Divider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
            )
        }
        items(coinResult) { coinViewItem ->
            MarketCoin(
                coinViewItem.fullCoin.coin.name,
                coinViewItem.fullCoin.coin.code,
                coinViewItem.fullCoin.coin.iconUrl,
                coinViewItem.fullCoin.iconPlaceholder,
                favorited = coinViewItem.favorited,
                label = coinViewItem.fullCoin.typeLabel,
                onClick = { onCoinClick(coinViewItem.fullCoin.coin) },
                onFavoriteClick = {
                    onFavoriteClick(
                        coinViewItem.favorited,
                        coinViewItem.fullCoin.coin.uid
                    )
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SearchView(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
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
            value = searchText,
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
                if (searchText.isEmpty()) {
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
fun CardsGrid(
    viewItems: List<MarketSearchModule.CardViewItem>,
    onCategoryClick: (MarketSearchModule.CardViewItem) -> Unit
) {
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
                CategoryCard(chunk[0], onCategoryClick)
                if (chunk.size > 1) {
                    CategoryCard(chunk[1], onCategoryClick)
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

@Composable
private fun MarketCoin(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    favorited: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    coinRate: String? = null,
    marketDataValue: MarketDataValue? = null,
    label: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    MultilineClear(
        borderBottom = true,
        onClick = onClick
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            MarketCoinFirstRow(coinName, coinRate)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(coinCode, marketDataValue, label)
        }
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onFavoriteClick()
            }
        ) {
            Image(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                painter = painterResource(R.drawable.ic_star_20),
                contentDescription = "coin icon",
                colorFilter = ColorFilter.tint(if (favorited) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey),
            )
        }
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
        MarketCoin(
            fullCoin.coin.name,
            fullCoin.coin.code,
            fullCoin.coin.iconUrl,
            fullCoin.iconPlaceholder,
            false,
            {},
            {},
            label = fullCoin.typeLabel,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchViewPreview() {
    ComposeAppTheme {
        var textState = remember { "" }
        SearchView(
            searchText = textState,
            onSearchTextChange = {
                textState = it
            },
            onRightTextButtonClick = { },
            leftIcon = R.drawable.ic_back,
            onBackButtonClick = { }
        )
    }
}
