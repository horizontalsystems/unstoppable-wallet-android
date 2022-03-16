package io.horizontalsystems.bankwallet.modules.nft.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModule
import io.horizontalsystems.bankwallet.modules.nft.ui.nftsCollectionSection
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class NftCollectionsFragment : BaseFragment() {

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
                NftCollectionsScreen(findNavController())
            }
        }
    }
}

@Composable
fun NftCollectionsScreen(navController: NavController) {
    val viewModel = viewModel<NftCollectionsViewModel>(factory = NftCollectionsModule.Factory())

    val viewState = viewModel.viewState
    val collections = viewModel.collectionViewItems
    val totalCurrencyValue = viewModel.totalCurrencyPrice
    val errorMessage = viewModel.errorMessage

    val loading = viewModel.loading

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Nfts_Title),
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
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
                        is ViewState.Loading -> {
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
                                    CellSingleLineClear(borderTop = true) {
                                        Text(
                                            text = totalCurrencyValue?.getFormatted() ?: "",
                                            style = ComposeAppTheme.typography.headline2,
                                            color = ComposeAppTheme.colors.jacob,
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

                                    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                                        collections.forEach { collection ->
                                            nftsCollectionSection(collection, viewModel) {
                                                navController.slideFromBottom(
                                                    R.id.nftAssetFragment,
                                                    NftAssetModule.prepareParams(
                                                        it.assetItem.accountId,
                                                        it.assetItem.tokenId,
                                                        it.assetItem.contract.address
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

    ErrorMessageHud(errorMessage)
}

@Composable
private fun ErrorMessageHud(errorMessage: TranslatableString?) {
    errorMessage?.let {
        HudHelper.showErrorMessage(LocalView.current, it.getString())
    }
}
