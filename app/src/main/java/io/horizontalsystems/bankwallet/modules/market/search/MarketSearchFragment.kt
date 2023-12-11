package io.horizontalsystems.bankwallet.modules.market.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.CoinItem
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderStick
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinFirstRow
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinSecondRow
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Optional
import kotlin.jvm.optionals.getOrDefault

class MarketSearchFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val viewModel = viewModel<MarketSearchViewModel>(
            factory = MarketSearchModule.Factory()
        )
        MarketSearchScreen(viewModel, navController)
    }
}

@Composable
fun MarketSearchScreen(viewModel: MarketSearchViewModel, navController: NavController) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val uiState = viewModel.uiState

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        SearchView(
            focusRequester = focusRequester,
            onSearchTextChange = { query -> viewModel.searchByQuery(query) },
            leftIcon = R.drawable.ic_back,
            onBackButtonClick = { navController.popBackStack() }
        )

        val itemSections = when (uiState.page) {
            is MarketSearchViewModel.Page.Discovery -> {
                mapOf(
                    Optional.of(stringResource(R.string.Market_Search_Sections_RecentTitle)) to uiState.page.recent,
                    Optional.of(stringResource(R.string.Market_Search_Sections_PopularTitle)) to uiState.page.popular,
                )
            }
            is MarketSearchViewModel.Page.SearchResults -> {
                mapOf(
                    Optional.ofNullable<String>(null) to uiState.page.items
                )
            }
        }

        MarketSearchResults(
            uiState.listId,
            itemSections = itemSections,
            onCoinClick = { coin ->
                viewModel.onCoinOpened(coin)
                navController.slideFromRight(
                    R.id.coinFragment,
                    CoinFragment.prepareParams(coin.uid, "market_search")
                )
            }
        ) { favorited, coinUid ->
            viewModel.onFavoriteClick(favorited, coinUid)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketSearchResults(
    vararg inputs: Any?,
    itemSections: Map<Optional<String>, List<CoinItem>>,
    onCoinClick: (Coin) -> Unit,
    onFavoriteClick: (Boolean, String) -> Unit,
) {
    if (itemSections.all { (_, items) -> items.isEmpty() }) {
        ListEmptyView(
            text = stringResource(R.string.EmptyResults),
            icon = R.drawable.ic_not_found
        )
    } else {
        val coroutineScope = rememberCoroutineScope()
        var revealedCardId by remember(*inputs) { mutableStateOf<String?>(null) }

        LazyColumn(
            state = rememberSaveable(
                *inputs,
                saver = LazyListState.Saver
            ) {
                LazyListState()
            }
        ) {
            itemSections.forEach { (title, coinItems) ->
                title.ifPresent {
                    stickyHeader {
                        HeaderStick(
                            borderTop = true,
                            text = title.get()
                        )
                    }
                }
                items(coinItems) { item ->
                    val coin = item.fullCoin.coin

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(if (item.favourited) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.jacob)
                                .align(Alignment.CenterEnd)
                                .width(100.dp)
                                .clickable {
                                    onFavoriteClick(
                                        item.favourited,
                                        coin.uid
                                    )

                                    coroutineScope.launch {
                                        delay(200)
                                        revealedCardId = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = if (item.favourited) R.drawable.ic_star_off_24 else R.drawable.ic_star_24),
                                tint = ComposeAppTheme.colors.claude,
                                contentDescription = stringResource(if (item.favourited) R.string.CoinPage_Unfavorite else R.string.CoinPage_Favorite),
                            )
                        }
                        val cardId = title.getOrDefault("") + coin.uid
                        DraggableCardSimple(
                            key = cardId,
                            isRevealed = revealedCardId == cardId,
                            cardOffset = 100f,
                            onReveal = {
                                if (revealedCardId != cardId) {
                                    revealedCardId = cardId
                                }
                            },
                            onConceal = {
                                revealedCardId = null
                            },
                            content = {
                                Box(modifier = Modifier.background(ComposeAppTheme.colors.tyler)) {
                                    MarketCoin(
                                        coinCode = coin.code,
                                        coinName = coin.name,
                                        coinIconUrl = coin.imageUrl,
                                        coinIconPlaceholder = item.fullCoin.iconPlaceholder,
                                        onClick = { onCoinClick(coin) }
                                    )
                                }
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
                Divider(
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
                )
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SearchView(
    focusRequester: FocusRequester = remember { FocusRequester() },
    onSearchTextChange: (String) -> Unit,
    leftIcon: Int,
    onBackButtonClick: () -> Unit,
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
                .focusRequester(focusRequester)
                .weight(1f),
            singleLine = true,
            textStyle = ColoredTextStyle(
                color = ComposeAppTheme.colors.leah,
                textStyle = ComposeAppTheme.typography.body
            ),
            decorationBox = { innerTextField ->
                if (searchText.isEmpty()) {
                    body_grey50(stringResource(R.string.Market_Search_Hint))
                }
                innerTextField()
            },
            cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
        )
    }
}

@Composable
private fun MarketCoin(
    coinCode: String,
    coinName: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    onClick: () -> Unit,
    coinRate: String? = null,
    marketDataValue: MarketDataValue? = null,
) {

    SectionItemBorderedRowUniversalClear(
        borderTop = true,
        onClick = onClick
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            MarketCoinFirstRow(coinCode, coinRate)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(coinName, marketDataValue, null)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarketCoinPreview() {
    val coin = Coin("ether", "Ethereum", "ETH")
    ComposeAppTheme {
        MarketCoin(
            coin.code,
            coin.name,
            coin.imageUrl,
            R.drawable.coin_placeholder,
            {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchViewPreview() {
    ComposeAppTheme {
        SearchView(
            onSearchTextChange = { },
            leftIcon = R.drawable.ic_back
        ) { }
    }
}
