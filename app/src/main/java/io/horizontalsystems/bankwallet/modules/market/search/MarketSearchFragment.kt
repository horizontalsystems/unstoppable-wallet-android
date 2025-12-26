package io.horizontalsystems.bankwallet.modules.market.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statSection
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderStick
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottom.BottomSearchBar
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Optional

class MarketSearchFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val viewModel = viewModel<MarketSearchViewModel>(
            factory = MarketSearchModule.Factory()
        )
        MarketSearchScreen(viewModel, navController)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MarketSearchScreen(
    viewModel: MarketSearchViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState

    var searchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var isSearchActive by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberSaveable(
        uiState.listId,
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            if (isSearchActive) {
                isSearchActive = false
            }
        }
    }

    val itemSections = when (uiState.page) {
        is MarketSearchViewModel.Page.Discovery -> {
            mapOf(
                MarketSearchSection.Recent to uiState.page.recent,
                MarketSearchSection.Popular to uiState.page.popular,
            )
        }

        is MarketSearchViewModel.Page.SearchResults -> {
            mapOf(
                MarketSearchSection.SearchResults to uiState.page.items
            )
        }
    }

    HSScaffold(
        title = stringResource(R.string.Market_Search),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Market_Filters),
                icon = R.drawable.ic_manage_2_24,
                onClick = {
                    navController.slideFromRight(R.id.marketAdvancedSearchFragment)

                    stat(
                        page = StatPage.Markets,
                        event = StatEvent.Open(StatPage.AdvancedSearch)
                    )
                },
            )
        )
    ) {
        Column {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!uiState.loading && itemSections.all { (_, items) -> items.isEmpty() }) {
                    ListEmptyView(
                        text = stringResource(R.string.Search_NotFounded),
                        icon = R.drawable.warning_filled_24
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .imePadding()
                            .fillMaxSize(),
                        state = lazyListState
                    ) {
                        itemSections.forEach { (section, coinItems) ->
                            if (coinItems.isNotEmpty()) {
                                section.title.ifPresent {
                                    stickyHeader {
                                        HeaderStick(
                                            borderTop = true,
                                            text = stringResource(id = section.title.get())
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
                                        Box(modifier = Modifier.background(ComposeAppTheme.colors.tyler)) {
                                            MarketCoin(
                                                coinUid = coin.uid,
                                                coinCode = coin.code,
                                                coinName = coin.name,
                                                coinIconUrl = coin.imageUrl,
                                                alternativeCoinIconUrl = coin.alternativeImageUrl,
                                                coinIconPlaceholder = item.fullCoin.iconPlaceholder,
                                                favourited = item.favourited,
                                                onFavoriteClick = { favorited, _ ->
                                                    viewModel.onFavoriteClick(favorited, coin.uid)
                                                },
                                                onClick = {
                                                    isSearchActive = false
                                                    coroutineScope.launch {
                                                        delay(200)

                                                        viewModel.onCoinOpened(coin)
                                                        navController.slideFromRight(
                                                            R.id.coinFragment,
                                                            CoinFragment.Input(coin.uid)
                                                        )
                                                    }
                                                    stat(
                                                        page = StatPage.MarketSearch,
                                                        event = StatEvent.OpenCoin(coin.uid),
                                                        section = section.statSection
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            HsDivider()
                        }
                        item {
                            VSpacer(72.dp)
                        }
                    }
                }

                BottomSearchBar(
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    keepCancelButton = true,
                    onActiveChange = { active ->
                        isSearchActive = active
                    },
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        viewModel.searchByQuery(query)
                    }
                ) {
                    navController.popBackStack()
                }
            }
        }
    }
}

enum class MarketSearchSection(val title: Optional<Int>) {
    Recent(Optional.of(R.string.Market_Search_Sections_RecentTitle)),
    Popular(Optional.of(R.string.Market_Search_Sections_PopularTitle)),
    SearchResults(Optional.empty())
}

@Composable
private fun MarketCoin(
    coinUid: String,
    coinCode: String,
    coinName: String,
    coinIconUrl: String,
    alternativeCoinIconUrl: String?,
    coinIconPlaceholder: Int,
    favourited: Boolean,
    onFavoriteClick: (Boolean, String) -> Unit,
    onClick: () -> Unit,
) {

    SectionItemBorderedRowUniversalClear(
        borderTop = true,
        onClick = onClick
    ) {
        HsImage(
            url = coinIconUrl,
            alternativeUrl = alternativeCoinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            headline2_leah(
                text = coinCode,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            subhead2_grey(
                text = coinName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HSpacer(16.dp)
        if (favourited) {
            HsIconButton(
                modifier = Modifier.size(20.dp),
                onClick = {
                    onFavoriteClick(true, coinUid)
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
                    onFavoriteClick(false, coinUid)
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
}

@Preview(showBackground = true)
@Composable
fun MarketCoinPreview() {
    val coin = Coin("ether", "Ethereum", "ETH")
    ComposeAppTheme {
        MarketCoin(
            coin.uid,
            coin.code,
            coin.name,
            coin.imageUrl,
            null,
            R.drawable.coin_placeholder,
            false,
            { _, _ -> },
            {}
        )
    }
}
