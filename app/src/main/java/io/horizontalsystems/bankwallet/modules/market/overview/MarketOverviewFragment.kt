package io.horizontalsystems.bankwallet.modules.market.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.market.category.MarketCategoryFragment
import io.horizontalsystems.bankwallet.modules.market.overview.ui.BoardsView
import io.horizontalsystems.bankwallet.modules.market.overview.ui.MetricChartsView
import io.horizontalsystems.bankwallet.modules.market.overview.ui.TopNftCollectionsBoardView
import io.horizontalsystems.bankwallet.modules.market.overview.ui.TopSectorsBoardView
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketTopCoinsFragment
import io.horizontalsystems.bankwallet.modules.market.topnftcollections.TopNftCollectionsFragment
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.core.findNavController

class MarketOverviewFragment : BaseFragment() {

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
                    MarketOverviewScreen(findNavController())
                }
            }
        }
    }
}

@Composable
private fun MarketOverviewScreen(
    navController: NavController,
    viewModel: MarketOverviewViewModel = viewModel(factory = MarketOverviewModule.Factory())
) {
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val viewItem by viewModel.viewItem.observeAsState()

    val scrollState = rememberScrollState()

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = {
            viewModel.refresh()
        }
    ) {
        Crossfade(viewState) { viewState ->
            when (viewState) {
                is ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }
                is ViewState.Success -> {
                    viewItem?.let { viewItem ->
                        Column(
                            modifier = Modifier.verticalScroll(scrollState)
                        ) {
                            Box(
                                modifier = Modifier.height(240.dp)
                            ) {
                                MetricChartsView(viewItem.marketMetrics, navController)
                            }
                            BoardsView(
                                boards = viewItem.boards,
                                navController = navController,
                                onClickSeeAll = { listType ->
                                    val (sortingField, topMarket, marketField) = viewModel.getTopCoinsParams(
                                        listType
                                    )
                                    val args = MarketTopCoinsFragment.prepareParams(
                                        sortingField,
                                        topMarket,
                                        marketField
                                    )

                                    navController.slideFromBottom(R.id.marketTopCoinsFragment, args)
                                },
                                onSelectTopMarket = { topMarket, listType ->
                                    viewModel.onSelectTopMarket(topMarket, listType)
                                }
                            )

                            TopSectorsBoardView(
                                board = viewItem.topSectorsBoard,
                                onClickSeeAll = {
                                    navController.slideFromRight(R.id.marketSearchFragment)
                                },
                                onItemClick = { coinCategory ->
                                    navController.slideFromBottom(
                                        R.id.marketCategoryFragment,
                                        bundleOf(MarketCategoryFragment.categoryKey to coinCategory)
                                    )
                                }
                            )

                            TopNftCollectionsBoardView(
                                viewItem.topNftCollectionsBoard,
                                onSelectTimeDuration = { timeDuration ->
                                    viewModel.onSelectTopNftsTimeDuration(timeDuration)
                                },
                                onClickCollection = { collectionUid ->
                                    val args = NftCollectionFragment.prepareParams(collectionUid)
                                    navController.slideFromBottom(R.id.nftCollectionFragment, args)
                                },
                                onClickSeeAll = {
                                    val (sortingField, timeDuration) = viewModel.topNftCollectionsParams
                                    val args = TopNftCollectionsFragment.prepareParams(sortingField, timeDuration)

                                    navController.slideFromBottom(R.id.marketTopNftCollectionsFragment, args)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
