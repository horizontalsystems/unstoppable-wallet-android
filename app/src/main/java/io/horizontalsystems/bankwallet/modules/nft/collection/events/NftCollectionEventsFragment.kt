package io.horizontalsystems.bankwallet.modules.nft.collection.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModule
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.OnBottomReached
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper

class NftCollectionEventsFragment : BaseFragment() {

    private val viewModel by navGraphViewModels<NftCollectionViewModel>(R.id.nftCollectionFragment)

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
                    NftCollectionEventsScreen(findNavController(), viewModel.collectionUid)
                }
            }
        }
    }
}

@Composable
private fun NftCollectionEventsScreen(navController: NavController, collectionUid: String) {
    val viewModel = viewModel<NftCollectionEventsViewModel>(factory = NftCollectionEventsModule.Factory(collectionUid))

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(viewModel.isRefreshing),
        onRefresh = {
            viewModel.refresh()
        }
    ) {
        Crossfade(viewModel.viewState) { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }
                ViewState.Success -> {
                    NftEvents(navController, viewModel)
                }
            }
        }
    }
}

@Composable
private fun NftEvents(
    navController: NavController,
    viewModel: NftCollectionEventsViewModel
) {
    val listState = rememberLazyListState()
    val viewItem = viewModel.viewItem ?: return

    Column {
        Header(borderBottom = true) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(viewItem.eventTypeSelect.selected.title) {
                    viewModel.onClickEventType()
                }
            }
        }
        LazyColumn(state = listState) {
            viewItem.events.forEachIndexed { index, event ->
                item(key = "content-row-$index") {
                    NftEvent(
                        name = event.eventType.title.getString(),
                        subtitle = event.date?.let { DateHelper.getFullDate(it) } ?: "",
                        iconUrl = event.asset.imageUrl ?: "",
                        coinValue = event.amount?.coinValue?.getFormattedFull(),
                        currencyValue = event.amount?.currencyValue?.getFormattedFull()
                    ) {
                        val asset = event.asset
                        navController.slideFromBottom(
                            R.id.nftAssetFragment,
                            NftAssetModule.prepareParams(
                                asset.collectionUid,
                                asset.contract.address,
                                asset.tokenId
                            )
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(26.dp))
            }

            if (viewModel.loadingMore) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HSCircularProgressIndicator()
                    }
                }
            }
        }
    }

    listState.OnBottomReached(buffer = 6) {
        viewModel.onBottomReached()
    }

    when (val option = viewModel.eventTypeSelectorState) {
        is SelectorDialogState.Opened -> {
            AlertGroup(
                R.string.NftCollection_EventType_SelectorTitle,
                option.select,
                onSelect = { viewModel.onSelectEvenType(it) },
                onDismiss = { viewModel.onDismissEventTypeDialog() }
            )
        }
    }
}

@Composable
private fun NftEvent(
    name: String,
    subtitle: String,
    iconUrl: String,
    coinValue: String?,
    currencyValue: String?,
    onClick: () -> Unit
) {
    MultilineClear(
        borderBottom = true,
        onClick = onClick
    ) {
        Image(
            painter = rememberImagePainter(
                data = iconUrl,
                builder = { error(R.drawable.coin_placeholder) }
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketCoinFirstRow(name, coinValue)
            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = currencyValue ?: "",
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
            }
        }
    }
}
