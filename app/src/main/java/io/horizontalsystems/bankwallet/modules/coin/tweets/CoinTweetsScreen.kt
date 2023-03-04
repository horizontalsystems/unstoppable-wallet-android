package io.horizontalsystems.bankwallet.modules.coin.tweets

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.marketkit.models.FullCoin

@Composable
fun CoinTweetsScreen(
    fullCoin: FullCoin
) {
    val viewModel = viewModel<CoinTweetsViewModel>(factory = CoinTweetsModule.Factory(fullCoin))

    val items by viewModel.itemsLiveData.observeAsState(listOf())
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val context = LocalContext.current

    HSSwipeRefresh(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        Crossfade(viewState) { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }
                ViewState.Success -> {
                    if (items.isEmpty()) {
                        ListEmptyView(
                            text = stringResource(R.string.CoinPage_Twitter_NoTweets),
                            icon = R.drawable.ic_no_tweets
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxSize(),
                        ) {
                            items(items) { tweet: TweetViewItem ->
                                Spacer(modifier = Modifier.height(12.dp))
                                CellTweet(tweet) {
                                    LinkHelper.openLinkInAppBrowser(context, it.url)
                                }
                            }

                            item {
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 32.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ButtonSecondaryDefault(
                                        title = stringResource(id = R.string.CoinPage_Twitter_SeeOnTwitter),
                                        onClick = {
                                            LinkHelper.openLinkInAppBrowser(context, viewModel.twitterPageUrl)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                is ViewState.Error -> {
                    if (viewState.t is TweetsProvider.UserNotFound) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            subhead2_grey(
                                modifier = Modifier.align(Alignment.Center),
                                text = stringResource(id = R.string.CoinPage_Twitter_NotAvailable),
                            )
                        }
                    } else {
                        ListErrorView(stringResource(R.string.SyncError), viewModel::refresh)
                    }
                }
                null -> {}
            }
        }
    }
}
