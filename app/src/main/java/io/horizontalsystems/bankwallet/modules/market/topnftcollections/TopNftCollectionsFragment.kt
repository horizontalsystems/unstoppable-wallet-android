package io.horizontalsystems.bankwallet.modules.market.topnftcollections

import android.os.Parcelable
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.DescriptionCard
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinFirstRow
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinSecondRow
import io.horizontalsystems.bankwallet.ui.compose.components.NftIcon
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.bankwallet.ui.compose.components.SortMenu
import io.horizontalsystems.bankwallet.ui.compose.components.TopCloseButton
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class TopNftCollectionsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()
        val viewModel = viewModel<TopNftCollectionsViewModel>(
            factory = TopNftCollectionsModule.Factory(
                input.sortingField,
                input.timeDuration
            )
        )

        TopNftCollectionsScreen(
            viewModel,
            { navController.popBackStack() },
            { blockchainType, collectionUid ->
                val args = NftCollectionFragment.Input(collectionUid, blockchainType.uid)
                navController.slideFromBottom(R.id.nftCollectionFragment, args)
            }
        )
    }

    @Parcelize
    data class Input(
        val sortingField: SortingField,
        val timeDuration: TimeDuration
    ) : Parcelable
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopNftCollectionsScreen(
    viewModel: TopNftCollectionsViewModel,
    onCloseButtonClick: () -> Unit,
    onClickCollection: (BlockchainType, String) -> Unit,
) {
    val menu = viewModel.menu
    val header = viewModel.header

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(onCloseButtonClick)

            HSSwipeRefresh(
                refreshing = viewModel.isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                Crossfade(viewModel.viewState) { state ->
                    when (state) {
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
                            TopNftCollectionsList(
                                collections = viewModel.viewItems,
                                sortingField = viewModel.sortingField,
                                timeDuration = viewModel.timeDuration,
                                onClickCollection = onClickCollection,
                                preItems = {
                                    item {
                                        DescriptionCard(
                                            header.title,
                                            header.description,
                                            header.icon
                                        )
                                    }

                                    stickyHeader {
                                        HeaderSorting(borderTop = true, borderBottom = true) {
                                            SortMenu(menu.sortingFieldSelect.selected.title) {
                                                viewModel.onClickSortingFieldMenu()
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            ButtonSecondaryToggle(
                                                select = menu.timeDurationSelect,
                                                onSelect = { timeDuration ->
                                                    viewModel.onSelectTimeDuration(timeDuration)
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        //Dialog
        viewModel.sortingFieldSelectDialog?.let { sortingFieldSelect ->
            AlertGroup(
                R.string.Market_Sort_PopupTitle,
                sortingFieldSelect,
                onSelect = { viewModel.onSelectSortingField(it) },
                onDismiss = { viewModel.onSelectorDialogDismiss() }
            )
        }
    }
}

@Composable
private fun TopNftCollectionsList(
    collections: List<TopNftCollectionViewItem>,
    sortingField: SortingField,
    timeDuration: TimeDuration,
    onClickCollection: (BlockchainType, String) -> Unit,
    preItems: LazyListScope.() -> Unit
) {
    val state = rememberSaveable(sortingField, timeDuration, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize()
    ) {
        preItems.invoke(this)
        items(collections) { collection ->
            SectionItemBorderedRowUniversalClear(
                onClick = { onClickCollection(collection.blockchainType, collection.uid) },
                borderBottom = true
            ) {
                NftIcon(
                    iconUrl = collection.imageUrl ?: "",
                    placeholder = R.drawable.coin_placeholder,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    MarketCoinFirstRow(collection.name, collection.volume)
                    Spacer(modifier = Modifier.height(3.dp))
                    MarketCoinSecondRow(
                        collection.floorPrice,
                        MarketDataValue.Diff(collection.volumeDiff),
                        "${collection.order}"
                    )
                }
            }
        }
    }

}
