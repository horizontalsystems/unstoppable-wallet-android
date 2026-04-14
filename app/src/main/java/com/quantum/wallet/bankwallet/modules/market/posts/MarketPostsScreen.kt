package com.quantum.wallet.bankwallet.modules.market.posts

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatSection
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.HSSwipeRefresh
import com.quantum.wallet.bankwallet.ui.compose.components.CellNews
import com.quantum.wallet.bankwallet.ui.compose.components.ListErrorView
import com.quantum.wallet.bankwallet.ui.helpers.LinkHelper

@Composable
fun MarketPostsScreen(viewModel: MarketPostsViewModel = viewModel(factory = MarketPostsModule.Factory())) {
    val items by viewModel.itemsLiveData.observeAsState(listOf())
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val context = LocalContext.current

    HSSwipeRefresh(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.refresh()

            stat(page = StatPage.Markets, event = StatEvent.Refresh, section = StatSection.News)
        }
    ) {
        Crossfade(viewState) { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }
                ViewState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items) { postItem ->
                            Spacer(modifier = Modifier.height(12.dp))
                            CellNews(
                                source = postItem.source,
                                title = postItem.title,
                                body = postItem.body,
                                date = postItem.timeAgo,
                            ) {
                                LinkHelper.openLinkInAppBrowser(context, postItem.url)

                                stat(
                                    page = StatPage.Markets,
                                    event = StatEvent.Open(StatPage.ExternalNews),
                                    section = StatSection.News
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(140.dp))
                        }
                    }
                }
                null -> {}
            }
        }
    }
}

@Preview
@Composable
fun PreviewMarketPostView() {
    val postItem = MarketPostsModule.PostViewItem(
        "Tidal",
        "3iQ’s The Ether Fund begins \$CAD trading on TSX after Bitcoin The Ether Fund begins after Bitcoin",
        "Traders in East Asia are ready to take on more built by Wipro to streamline its liquefied.",
        "1h ago",
        "https://www.binance.org/news"
    )
    ComposeAppTheme {
        CellNews(
            source = postItem.source,
            title = postItem.title,
            body = postItem.body,
            date = postItem.timeAgo
        ) {}
    }
}
