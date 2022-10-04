package io.horizontalsystems.bankwallet.modules.nft.holdings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.TotalUIState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModule
import io.horizontalsystems.bankwallet.modules.nft.ui.NftAssetPreview
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class NftHoldingsFragment : BaseFragment() {
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
                NftHoldingsScreen(findNavController())
            }
        }
    }
}

@Composable
fun NftHoldingsScreen(navController: NavController) {
    val account = App.accountManager.activeAccount ?: return

    val viewModel = viewModel<NftHoldingsViewModel>(factory = NftHoldingsModule.Factory(account))

    val viewState = viewModel.viewState
    val collections = viewModel.viewItems
    val errorMessage = viewModel.errorMessage

    val loading = viewModel.refreshing

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Nfts_Title),
                navigationIcon = {
                    HsIconButton(onClick = navController::popBackStack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }

            )
            HSSwipeRefresh(
                state = rememberSwipeRefreshState(loading),
                onRefresh = viewModel::refresh
            ) {
                Crossfade(viewState) { viewState ->
                    when (viewState) {
                        ViewState.Loading -> {
                            Loading()
                        }
                        is ViewState.Error -> {
                            ListErrorView(stringResource(R.string.SyncError), viewModel::refresh)
                        }
                        ViewState.Success -> {
                            if (collections.isEmpty()) {
                                ListEmptyView(
                                    text = stringResource(R.string.Nfts_Empty),
                                    icon = R.drawable.ic_image_empty
                                )
                            } else {
                                Column {
                                    val context = LocalContext.current

                                    when (val totalState = viewModel.totalState) {
                                        TotalUIState.Hidden -> {
                                            DoubleText(
                                                title = "*****",
                                                body = "*****",
                                                dimmed = false,
                                                onClickTitle = {
                                                    viewModel.onBalanceClick()
                                                    HudHelper.vibrate(context)
                                                },
                                                onClickBody = {

                                                }
                                            )
                                        }
                                        is TotalUIState.Visible -> {
                                            DoubleText(
                                                title = totalState.currencyValueStr,
                                                body = totalState.coinValueStr,
                                                dimmed = totalState.dimmed,
                                                onClickTitle = {
                                                    viewModel.onBalanceClick()
                                                    HudHelper.vibrate(context)
                                                },
                                                onClickBody = {
                                                    viewModel.toggleTotalType()
                                                    HudHelper.vibrate(context)
                                                }
                                            )
                                        }
                                    }

                                    CellSingleLineClear(borderTop = true) {
                                        subhead2_grey(
                                            text = stringResource(R.string.Nfts_PriceMode),
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        var priceType by remember { mutableStateOf(viewModel.priceType) }

                                        ButtonSecondaryToggle(
                                            select = Select(priceType, PriceType.values().toList()),
                                            onSelect = {
                                                viewModel.updatePriceType(it)
                                                priceType = it
                                            }
                                        )
                                    }

                                    LazyColumn(
                                        contentPadding = PaddingValues(bottom = 32.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        collections.forEach { collection ->
                                            nftsCollectionSection(collection, viewModel) { asset ->
                                                navController.slideFromBottom(
                                                    R.id.nftAssetFragment,
                                                    NftAssetModule.prepareParams(
                                                        asset.collectionUid,
                                                        asset.nftUid
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    errorMessage?.let {
        SnackbarError(it.getString())
        viewModel.errorShown()
    }
}

fun LazyListScope.nftsCollectionSection(
    collection: NftCollectionViewItem,
    viewModel: NftHoldingsViewModel,
    onClickAsset: (NftAssetViewItem) -> Unit
) {
    item(key = "${collection.uid}-header") {
        CellSingleLineClear(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    viewModel.toggleCollection(collection)
                },
            borderTop = true
        ) {
            NftIcon(
                modifier = Modifier.size(24.dp),
                iconUrl = collection.imageUrl,
            )
            headline2_leah(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                text = collection.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subhead1_grey(text = collection.count.toString())

            val painter = if (collection.expanded) {
                painterResource(R.drawable.ic_arrow_big_up_20)
            } else {
                painterResource(R.drawable.ic_arrow_big_down_20)
            }

            Icon(
                modifier = Modifier.padding(start = 8.dp),
                painter = painter,
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
    }

    if (collection.expanded) {
        collection.assets.chunked(2).forEachIndexed { index, assets ->
            item(key = "${collection.uid}-content-row-$index") {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    assets.forEach { asset ->
                        Box(modifier = Modifier.weight(1f)) {
                            NftAssetPreview(
                                name = asset.name,
                                imageUrl = asset.imageUrl,
                                onSale = asset.onSale,
                                coinPrice = asset.price,
                                currencyPrice = asset.priceInFiat,
                                onClick = {
                                    onClickAsset.invoke(asset)
                                }
                            )
                        }
                    }

                    if (assets.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "${collection.uid}-content-bottom-space") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

