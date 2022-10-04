package io.horizontalsystems.bankwallet.modules.market.topnftcollections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType

class TopNftCollectionsFragment : BaseFragment() {

    private val sortingField by lazy {
        arguments?.getParcelable<SortingField>(sortingFieldKey)!!
    }
    private val timeDuration by lazy {
        arguments?.getParcelable<TimeDuration>(timeDurationKey)!!
    }
    val viewModel by viewModels<TopNftCollectionsViewModel> {
        TopNftCollectionsModule.Factory(sortingField, timeDuration)
    }

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
                    TopNftCollectionsScreen(
                        viewModel,
                        { findNavController().popBackStack() },
                        { blockchainType, collectionUid ->
                            val args = NftCollectionFragment.prepareParams(collectionUid, blockchainType)
                            findNavController().slideFromBottom(R.id.nftCollectionFragment, args)
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val sortingFieldKey = "sorting_field"
        private const val timeDurationKey = "time_duration"

        fun prepareParams(
            sortingField: SortingField,
            timeDuration: TimeDuration
        ): Bundle {
            return bundleOf(
                sortingFieldKey to sortingField,
                timeDurationKey to timeDuration
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopNftCollectionsScreen(
    viewModel: TopNftCollectionsViewModel,
    onCloseButtonClick: () -> Unit,
    onClickCollection: (BlockchainType, String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val menu = viewModel.menu
    val header = viewModel.header

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(interactionSource, onCloseButtonClick)

            HSSwipeRefresh(
                state = rememberSwipeRefreshState(viewModel.isRefreshing),
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
            MultilineClear(
                onClick = { onClickCollection(collection.blockchainType, collection.uid) },
                borderBottom = true
            ) {
                NftIcon(
                    iconUrl = collection.imageUrl ?: "",
                    placeholder = R.drawable.coin_placeholder,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp)
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
