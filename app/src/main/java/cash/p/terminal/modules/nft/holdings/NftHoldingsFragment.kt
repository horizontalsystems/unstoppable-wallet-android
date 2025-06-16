package cash.p.terminal.modules.nft.holdings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.ui_compose.BaseComposeFragment
import io.horizontalsystems.core.slideFromBottom
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.modules.balance.TotalUIState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.nft.asset.NftAssetModule
import cash.p.terminal.modules.nft.ui.NftAssetPreview
import cash.p.terminal.ui_compose.components.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonSecondaryToggle
import cash.p.terminal.ui_compose.components.CellBorderedRowUniversal
import cash.p.terminal.ui_compose.components.CellSingleLineClear
import cash.p.terminal.ui.compose.components.DoubleText
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui_compose.components.NftIcon
import cash.p.terminal.ui.compose.components.SnackbarError
import cash.p.terminal.ui_compose.components.headline2_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper

class NftHoldingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        NftHoldingsScreen(navController)
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

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(R.string.Nfts_Title),
            navigationIcon = {
                HsBackButton(onClick = navController::popBackStack)
            }

        )
        HSSwipeRefresh(
            refreshing = loading,
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

                                when (val totalState = viewModel.totalUiState) {
                                    TotalUIState.Hidden -> {
                                        DoubleText(
                                            title = "*****",
                                            body = "*****",
                                            dimmed = false,
                                            onClickTitle = {
                                                viewModel.toggleBalanceVisibility()
                                                HudHelper.vibrate(context)
                                            },
                                            onClickSubtitle = {
                                                viewModel.toggleTotalType()
                                                HudHelper.vibrate(context)
                                            },
                                        )
                                    }

                                    is TotalUIState.Visible -> {
                                        DoubleText(
                                            title = totalState.primaryAmountStr,
                                            body = totalState.secondaryAmountStr,
                                            dimmed = totalState.dimmed,
                                            onClickTitle = {
                                                viewModel.toggleBalanceVisibility()
                                                HudHelper.vibrate(context)
                                            },
                                            onClickSubtitle = {
                                                viewModel.toggleTotalType()
                                                HudHelper.vibrate(context)
                                            },
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
                                                NftAssetModule.Input(asset.collectionUid, asset.nftUid)
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
        CellBorderedRowUniversal(
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

