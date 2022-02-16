package io.horizontalsystems.bankwallet.modules.nft.asset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class NftAssetFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val accountId = requireArguments().getString(NftAssetModule.accountIdKey)
        val tokenId = requireArguments().getString(NftAssetModule.tokenIdKey)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                NftAssetScreen(findNavController(), accountId, tokenId)
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun NftAssetScreen(navController: NavController, accountId: String?, tokenId: String?) {
    if (accountId == null || tokenId == null) return

    val viewModel =
        viewModel<NftAssetViewModel>(factory = NftAssetModule.Factory(accountId, tokenId))
    val viewState = viewModel.viewState

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
                state = rememberSwipeRefreshState(false),
                onRefresh = { }
            ) {
                when (viewState) {
                    is ViewState.Error -> {
                        ListErrorView(stringResource(R.string.Error)) {

                        }
                    }
                    ViewState.Success -> {
                        viewModel.assetItem?.let { asset ->
                            LazyColumn {
                                item {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Image(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(328.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            painter = rememberImagePainter(asset.imageUrl),
                                            contentDescription = null,
                                            contentScale = ContentScale.FillWidth
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = asset.name,
                                            color = ComposeAppTheme.colors.leah,
                                            style = ComposeAppTheme.typography.headline1
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "collection name",
                                            color = ComposeAppTheme.colors.grey,
                                            style = ComposeAppTheme.typography.subhead1
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))
                                        Row {
                                            ButtonPrimaryDefault(
                                                modifier = Modifier.weight(1f),
                                                title = stringResource(id = R.string.NftAsset_OpenSea),
                                                onClick = { 
                                                    
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            ButtonPrimaryCircle(
                                                icon = R.drawable.ic_more_24,
                                                onClick = { /*TODO*/ }
                                            )
                                        }

                                    }
                                }

                                item {
                                    Column {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        CellSingleLineLawrenceSection {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_nft_amount_20),
                                                    contentDescription = null,
                                                    tint = ComposeAppTheme.colors.grey
                                                )

                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    text = stringResource(id = R.string.NftAsset_OwnedCount),
                                                    color = ComposeAppTheme.colors.leah,
                                                    style = ComposeAppTheme.typography.body
                                                )

                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = asset.ownedCount.toString(),
                                                    color = ComposeAppTheme.colors.grey,
                                                    style = ComposeAppTheme.typography.subhead1
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))
                                        val prices = mutableListOf<Pair<String, CoinValue>>()
                                        asset.prices.average7d?.let {
                                            prices.add(Pair(stringResource(id = R.string.Nfts_PriceType_Days_7), it))
                                        }
                                        asset.prices.average30d?.let {
                                            prices.add(Pair(stringResource(id = R.string.Nfts_PriceType_Days_30), it))
                                        }
                                        asset.prices.last?.let {
                                            prices.add(Pair(stringResource(id = R.string.Nfts_PriceType_LastPrice), it))
                                        }

                                        CellMultilineLawrenceSection(prices) { (title, price) ->
                                            NftAssetPriceCell(title, price)
                                        }

                                        NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Properties))
                                        NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Description))
                                        NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Details))
                                        NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Links))

                                        Spacer(modifier = Modifier.height(32.dp))
                                        CellFooter(text = stringResource(id = R.string.PoweredBy_OpenSeaAPI))
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

@Composable
private fun NftAssetSectionBlock(text: String) {
    Spacer(modifier = Modifier.height(24.dp))
    CellSingleLineClear(borderTop = true) {
        Text(
            text = text,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah
        )
    }
}

@Composable
private fun NftAssetPriceCell(
    title: String,
    coinValue: CoinValue
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.body,
        )
        Spacer(modifier = Modifier.weight(1f))
        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = coinValue.getFormatted(),
                color = ComposeAppTheme.colors.jacob,
                style = ComposeAppTheme.typography.body,
                textAlign = TextAlign.End,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "$1,234.56",
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
                textAlign = TextAlign.End,
            )
        }
    }
}