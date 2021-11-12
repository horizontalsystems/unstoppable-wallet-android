package io.horizontalsystems.bankwallet.modules.market.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper

class MarketPostsFragment : BaseFragment() {

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
                ComposeAppTheme {
                    MarketPostsScreen()
                }
            }
        }
    }
}

@Composable
private fun MarketPostsScreen(viewModel: MarketPostsViewModel = viewModel(factory = MarketPostsModule.Factory())) {
    val items by viewModel.itemsLiveData.observeAsState(listOf())
    val loading by viewModel.loadingLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState()
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val context = LocalContext.current

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing ?: false || loading ?: false),
        onRefresh = {
            viewModel.refresh()
        }
    ) {
        when (viewState) {
            ViewState.Error -> {
                ListErrorView(
                    stringResource(R.string.Market_SyncError)
                ) {
                    viewModel.onErrorClick()
                }
            }
            ViewState.Success -> {
                LazyColumn {
                    items(items) { postItem ->
                        Spacer(modifier = Modifier.height(12.dp))
                        MarketPostView(postItem) { link ->
                            LinkHelper.openLinkInAppBrowser(context, link)
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MarketPostView(item: MarketPostsModule.PostViewItem, onClick: (String) -> Unit) {
    var titleLines by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = ComposeAppTheme.colors.lawrence,
        onClick = { onClick.invoke(item.url) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.source,
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.captionSB,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                color = ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.headline2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { res -> titleLines = res.lineCount }
            )
            if (titleLines < 3) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.body,
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (titleLines == 1) 2 else 1,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.timeAgo,
                color = ComposeAppTheme.colors.grey50,
                style = ComposeAppTheme.typography.micro,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
fun PreviewMarketPostView() {
    val item = MarketPostsModule.PostViewItem(
        "Tidal",
        "3iQ’s The Ether Fund begins \$CAD trading on TSX after Bitcoin The Ether Fund begins after Bitcoin",
        "Traders in East Asia are ready to take on more built by Wipro to streamline its liquefied.",
        "1h ago",
        "https://www.binance.org/news"
    )
    ComposeAppTheme {
        MarketPostView(item, {})
    }
}
