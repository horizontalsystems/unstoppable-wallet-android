package io.horizontalsystems.bankwallet.modules.nft.collection.events

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModule
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.OnBottomReached
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun NftCollectionEventsScreen(navController: NavController, blockchainType: BlockchainType, collectionUid: String) {
    val viewModel = viewModel<NftCollectionEventsViewModel>(
        factory = NftCollectionEventsModule.Factory(
            NftEventListType.Collection(
                blockchainType,
                collectionUid
            )
        )
    )

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
                    NftEvents(viewModel, navController)
                }
            }
        }
    }
}

@Composable
fun NftEvents(
    viewModel: NftCollectionEventsViewModel,
    navController: NavController?,
    hideEventIcon: Boolean = false,
) {
    val listState = rememberLazyListState()
    val viewItem = viewModel.viewItem ?: return

    Column {
        HeaderSorting(borderBottom = true) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(viewItem.eventTypeSelect.selected.title) {
                    viewModel.onClickEventType()
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            viewItem.events?.forEachIndexed { index, event ->
                item(key = "content-row-$index") {
                    NftEvent(
                        name = NftEventTypeWrapper.title(event.type).getString(),
                        subtitle = event.date?.let { DateHelper.getFullDate(it) } ?: "",
                        iconUrl = if (hideEventIcon) null else event.imageUrl ?: "",
                        coinValue = event.price?.getFormattedFull(),
                        currencyValue = event.priceInFiat?.getFormattedFull(),
                        onClick = navController?.let {
                            {
                                navController.slideFromBottom(
                                    R.id.nftAssetFragment,
                                    NftAssetModule.prepareParams(event.providerCollectionUid, event.nftUid)
                                )
                            }
                        }
                    )
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

        if (viewItem.events != null && viewItem.events.isEmpty()) {
            ListEmptyView(
                text = stringResource(R.string.NftAssetActivity_Empty),
                icon = R.drawable.ic_outgoingraw
            )
        }
    }

    listState.OnBottomReached {
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
        SelectorDialogState.Closed -> {}
    }
}

@Composable
fun NftEvent(
    name: String,
    subtitle: String,
    iconUrl: String?,
    coinValue: String?,
    currencyValue: String?,
    onClick: (() -> Unit)? = null
) {
    MultilineClear(
        borderBottom = true,
        onClick = onClick
    ) {
        iconUrl?.let {
            Image(
                painter = rememberAsyncImagePainter(
                    model = iconUrl,
                    error = painterResource(R.drawable.coin_placeholder)
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketCoinFirstRow(name, coinValue)
            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                subhead2_grey(
                    text = subtitle,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                subhead2_grey(
                    text = currencyValue ?: "",
                    maxLines = 1,
                )
            }
        }
    }
}
