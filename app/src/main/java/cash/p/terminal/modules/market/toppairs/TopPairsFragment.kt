package cash.p.terminal.modules.market.toppairs

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.modules.market.overview.ui.TopPairItem
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.components.DescriptionCard
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.TopCloseButton
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.helpers.LinkHelper

class TopPairsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        TopPairsScreen(navController)
    }
}

@Composable
fun TopPairsScreen(navController: NavController) {
    val viewModel = viewModel<TopPairsViewModel>(factory = TopPairsViewModel.Factory())
    val uiState = viewModel.uiState
    val context = LocalContext.current

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(modifier = Modifier.padding(it)) {
            TopCloseButton { navController.popBackStack() }

            HSSwipeRefresh(
                refreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh
            ) {
                Crossfade(uiState.viewState, label = "") { viewState ->
                    when (viewState) {
                        ViewState.Loading -> {
                            Loading()
                        }

                        is ViewState.Error -> {
                            ListErrorView(
                                stringResource(R.string.SyncError),
                                viewModel::onErrorClick
                            )
                        }

                        ViewState.Success -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    DescriptionCard(
                                        stringResource(id = R.string.TopPairs_Title),
                                        stringResource(id = R.string.TopPairs_Description),
                                        ImageSource.Local(R.drawable.ic_token_pairs)
                                    )
                                }
                                itemsIndexed(uiState.items) { i, item ->
                                    TopPairItem(item, borderTop = i == 0, borderBottom = true) {
                                        it.tradeUrl?.let {
                                            LinkHelper.openLinkInAppBrowser(context, it)

                                            stat(page = StatPage.TopMarketPairs, event = StatEvent.Open(StatPage.ExternalMarketPair))
                                        }
                                    }
                                }
                                item {
                                    VSpacer(height = 32.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
