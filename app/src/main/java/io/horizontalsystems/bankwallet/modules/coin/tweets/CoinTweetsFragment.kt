package io.horizontalsystems.bankwallet.modules.coin.tweets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.CellTweet
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper

@ExperimentalCoilApi
class CoinTweetsFragment : BaseFragment() {
    private val vmFactory by lazy { CoinTweetsModule.Factory(coinViewModel.fullCoin) }

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by viewModels<CoinTweetsViewModel> { vmFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComposeAppTheme {
                    CoinTweetsScreen(viewModel)
                }
            }
        }
    }
}

@ExperimentalCoilApi
@Composable
fun CoinTweetsScreen(viewModel: CoinTweetsViewModel) {
    val items by viewModel.itemsLiveData.observeAsState(listOf())
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val context = LocalContext.current

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { viewModel.refresh() },
    ) {
        when (val tmp = viewState) {
            ViewState.Success -> {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
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
            is ViewState.Error -> {
                if (tmp.t is TweetsProvider.UserNotFound) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(id = R.string.CoinPage_Twitter_NotAvailable),
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.subhead2,
                        )
                    }
                } else {
                    ListErrorView(
                        stringResource(R.string.Market_SyncError)
                    ) {
                        viewModel.refresh()
                    }
                }
            }
        }
    }
}
