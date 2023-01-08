package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.category.MarketCategoryFragment
import io.horizontalsystems.bankwallet.modules.market.overview.ui.*
import io.horizontalsystems.bankwallet.modules.market.platform.MarketPlatformFragment
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketTopCoinsFragment
import io.horizontalsystems.bankwallet.modules.market.topnftcollections.TopNftCollectionsFragment
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformsFragment
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionFragment
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView

@Composable
fun MarketOverviewScreen(
    navController: NavController,
    viewModel: MarketOverviewViewModel = viewModel(factory = MarketOverviewModule.Factory())
) {
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val viewItem by viewModel.viewItem.observeAsState()

    val scrollState = rememberScrollState()

    HSSwipeRefresh(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.refresh()
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
                    viewItem?.let { viewItem ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
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
                                onClickCollection = { blockchainType, collectionUid ->
                                    val args = NftCollectionFragment.prepareParams(collectionUid, blockchainType)
                                    navController.slideFromBottom(R.id.nftCollectionFragment, args)
                                },
                                onClickSeeAll = {
                                    val (sortingField, timeDuration) = viewModel.topNftCollectionsParams
                                    val args = TopNftCollectionsFragment.prepareParams(sortingField, timeDuration)

                                    navController.slideFromBottom(R.id.marketTopNftCollectionsFragment, args)
                                }
                            )

                            TopPlatformsBoardView(
                                viewItem.topPlatformsBoard,
                                onSelectTimeDuration = { timeDuration ->
                                    viewModel.onSelectTopPlatformsTimeDuration(timeDuration)
                                },
                                onItemClick = {
                                    val args = MarketPlatformFragment.prepareParams(it)
                                    navController.slideFromRight(R.id.marketPlatformFragment, args)
                                },
                                onClickSeeAll = {
                                    val timeDuration = viewModel.topPlatformsTimeDuration
                                    val args = TopPlatformsFragment.prepareParams(timeDuration)

                                    navController.slideFromBottom(R.id.marketTopPlatformsFragment, args)
                                }
                            )
                        }
                    }
                }
                null -> {}
            }
        }
    }
}
