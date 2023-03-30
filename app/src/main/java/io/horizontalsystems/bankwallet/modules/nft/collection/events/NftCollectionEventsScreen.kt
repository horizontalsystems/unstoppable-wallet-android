package io.horizontalsystems.bankwallet.modules.nft.collection.events

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.OnBottomReached
import io.horizontalsystems.bankwallet.ui.compose.SelectOptional
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch

@Composable
fun NftCollectionEventsScreen(navController: NavController, blockchainType: BlockchainType, collectionUid: String, contracts: List<ContractInfo>) {
    val viewModel = viewModel<NftCollectionEventsViewModel>(
        factory = NftCollectionEventsModule.Factory(
            NftEventListType.Collection(
                blockchainType,
                collectionUid,
                contracts
            )
        )
    )

    HSSwipeRefresh(
        refreshing = viewModel.isRefreshing,
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
private fun ContractBottomSheet(
    contractSelect: SelectOptional<ContractInfo>?,
    onSelect: (ContractInfo) -> Unit,
    onClose: (() -> Unit)
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.icon_paper_contract_20),
        title = stringResource(R.string.CoinPage_Contracts),
        onCloseClick = onClose,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        Spacer(Modifier.height(12.dp))

        val contracts = contractSelect?.options ?: listOf()
        val selected = contractSelect?.selected
        CellUniversalLawrenceSection(
            items = contracts,
            showFrame = true
        ) { contract ->
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    onSelect(contract)
                    onClose()
                }
            ) {
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = rememberAsyncImagePainter(
                        model = contract.imgUrl,
                        error = painterResource(R.drawable.ic_platform_placeholder_32)
                    ),
                    contentDescription = "platform"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    contract.name?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            body_leah(
                                modifier = Modifier.weight(1f, fill = false),
                                text = it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            contract.schema?.let { labelText ->
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ComposeAppTheme.colors.jeremy)
                                ) {
                                    Text(
                                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 1.dp),
                                        text = labelText,
                                        color = ComposeAppTheme.colors.bran,
                                        style = ComposeAppTheme.typography.microSB,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(1.dp))
                    }
                    subhead2_grey(
                        text = contract.shortened,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (contract == selected) {
                    Image(
                        modifier = Modifier.padding(start = 5.dp),
                        painter = painterResource(id = R.drawable.ic_checkmark_20),
                        colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                        contentDescription = null
                    )
                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NftEvents(
    viewModel: NftCollectionEventsViewModel,
    navController: NavController?,
    hideEventIcon: Boolean = false,
) {
    val listState = rememberLazyListState()
    var eventTypeSelectorState by remember { mutableStateOf(SelectorDialogState.Closed) }
    val events = viewModel.events

    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            ContractBottomSheet(
                contractSelect = viewModel.contractSelect,
                onSelect = { contract -> viewModel.onSelect(contract) },
                onClose = { coroutineScope.launch { modalBottomSheetState.hide() } }
            )
        },
    ) {
        Column {
            HeaderSorting(borderBottom = true) {
                SortMenu(viewModel.eventTypeSelect.selected.title) {
                    eventTypeSelectorState = SelectorDialogState.Opened
                }
                Spacer(modifier = Modifier.weight(1f))
                viewModel.contractSelect?.selected?.let { selected ->
                    ButtonSecondaryWithIcon(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .height(28.dp),
                        title = selected.name ?: selected.shortened,
                        iconRight = painterResource(R.drawable.ic_down_arrow_20),
                        onClick = {
                            coroutineScope.launch {
                                modalBottomSheetState.show()
                            }
                        }
                    )
                }
            }
            if (events != null && events.isEmpty()) {
                ListEmptyView(
                    text = stringResource(R.string.NftAssetActivity_Empty),
                    icon = R.drawable.ic_outgoingraw
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    events?.forEachIndexed { index, event ->
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
            }
        }
    }

    listState.OnBottomReached {
        viewModel.onBottomReached()
    }

    when (eventTypeSelectorState) {
        SelectorDialogState.Opened -> {
            AlertGroup(
                R.string.NftCollection_EventType_SelectorTitle,
                viewModel.eventTypeSelect,
                onSelect = { eventType ->
                    viewModel.onSelect(eventType)
                    eventTypeSelectorState = SelectorDialogState.Closed
                },
                onDismiss = { eventTypeSelectorState = SelectorDialogState.Closed }
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
    SectionItemBorderedRowUniversalClear(
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
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
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
