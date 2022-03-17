package io.horizontalsystems.bankwallet.modules.market.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.category.MarketCategoryFragment
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.CoinItem
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

                MarketSearchScreen(
                    screenState = screenState,
                    onBackButtonClick = { findNavController().popBackStack() },
                    onFilterButtonClick = {
                        findNavController().slideFromRight(
                            R.id.marketSearchFragment_to_marketAdvancedSearchFragment
                        )
                    },
                    onCoinClick = { coin ->
                        val arguments = CoinFragment.prepareParams(coin.uid)
                        findNavController().slideFromRight(R.id.coinFragment, arguments)
                    },
                    onCategoryClick = { viewItemType ->
                        when (viewItemType) {
                            MarketSearchModule.DiscoveryItem.TopCoins -> {
                                findNavController().slideFromBottom(
                                    R.id.marketSearchFragment_to_marketTopCoinsFragment
                                )
                            }
                            is MarketSearchModule.DiscoveryItem.Category -> {
                                findNavController().slideFromBottom(
                                    R.id.marketCategoryFragment,
                                    bundleOf(MarketCategoryFragment.categoryKey to viewItemType.coinCategory)
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

@Composable
fun MarketSearchScreen(
    screenState: MarketSearchModule.DataState?,
    onBackButtonClick: () -> Unit,
    onFilterButtonClick: () -> Unit,
    onCoinClick: (Coin) -> Unit,
    onFavoriteClick: (Boolean, String) -> Unit,
    onCategoryClick: (MarketSearchModule.DiscoveryItem) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            SearchView(
                onSearchTextChange = {
                    onSearchQueryChange.invoke(it)
                },
                onRightTextButtonClick = onFilterButtonClick,
                leftIcon = R.drawable.ic_back,
                onBackButtonClick = onBackButtonClick
            )
            when (screenState) {
                is MarketSearchModule.DataState.Discovery -> {
                    CardsGrid(screenState.discoveryItems, onCategoryClick)
                }
                is MarketSearchModule.DataState.SearchResult -> {
                    if (screenState.coinItems.isEmpty()) {
                        ListEmptyView(
                            text = stringResource(R.string.EmptyResults),
                            icon = R.drawable.ic_not_found
                        )
                    } else {
                        MarketSearchResults(
                            screenState.coinItems,
                            onCoinClick,
                            onFavoriteClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketSearchResults(
    coinResult: List<CoinItem>,
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
                favorited = coinViewItem.favourited,
                label = coinViewItem.fullCoin.typeLabel,
                onClick = { onCoinClick(coinViewItem.fullCoin.coin) },
                onFavoriteClick = {
                    onFavoriteClick(
                        coinViewItem.favourited,
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
    onSearchTextChange: (String) -> Unit,
    onRightTextButtonClick: () -> Unit,
    leftIcon: Int,
    onBackButtonClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var searchText by rememberSaveable { mutableStateOf("") }

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
                searchText = value
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
                text = stringResource(R.string.Market_Filters),
                style = ComposeAppTheme.typography.headline2,
                color = ComposeAppTheme.colors.jacob,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

}

@Composable
fun CardsGrid(
    viewItems: List<MarketSearchModule.DiscoveryItem>,
    onCategoryClick: (MarketSearchModule.DiscoveryItem) -> Unit
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

        IconButton(onClick = onFavoriteClick) {
            Icon(
                painter = painterResource(if (favorited) R.drawable.ic_star_filled_20 else R.drawable.ic_star_20),
                contentDescription = "coin icon",
                tint = if (favorited) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey
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
        SearchView(
            onSearchTextChange = { },
            onRightTextButtonClick = { },
            leftIcon = R.drawable.ic_back,
            onBackButtonClick = { }
        )
    }
}
