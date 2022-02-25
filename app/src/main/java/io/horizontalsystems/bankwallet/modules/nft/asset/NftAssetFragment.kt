package io.horizontalsystems.bankwallet.modules.nft.asset

import android.content.Intent
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import coil.size.OriginalSize
import coil.size.Scale
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.shortenedAddress
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.nft.collection.NftAssetItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

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

@Composable
fun NftAssetScreen(navController: NavController, accountId: String?, tokenId: String?) {
    if (accountId == null || tokenId == null) return

    val viewModel =
        viewModel<NftAssetViewModel>(factory = NftAssetModule.Factory(accountId, tokenId))
    val viewState = viewModel.viewState

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close)
                    ) {
                        navController.popBackStack()
                    }
                )
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
                            NftAsset(asset)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun NftAsset(asset: NftAssetItem) {
    LazyColumn {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(12.dp))
                Box {
                    val painter = rememberImagePainter(
                        data = asset.imageUrl,
                        builder = {
                            size(OriginalSize)
                            scale(Scale.FIT)
                        })
                    if (painter.state !is ImagePainter.State.Success) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(328.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ComposeAppTheme.colors.steel20)
                        )
                    }
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                asset.name?.let {
                    Text(
                        text = it,
                        color = ComposeAppTheme.colors.leah,
                        style = ComposeAppTheme.typography.headline1
                    )
                }

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
                    prices.add(
                        Pair(
                            stringResource(id = R.string.Nfts_PriceType_Days_7),
                            it
                        )
                    )
                }
                asset.prices.average30d?.let {
                    prices.add(
                        Pair(
                            stringResource(id = R.string.Nfts_PriceType_Days_30),
                            it
                        )
                    )
                }
                asset.prices.last?.let {
                    prices.add(
                        Pair(
                            stringResource(id = R.string.Nfts_PriceType_LastSale),
                            it
                        )
                    )
                }

                CellMultilineLawrenceSection(prices) { (title, price) ->
                    NftAssetPriceCell(title, price)
                }

                NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Properties)) {

                }
                if (!asset.description.isNullOrBlank()) {
                    NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Description)) {
                        InfoText(asset.description)
                    }
                }

                NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Details)) {
                    val composableItems = mutableListOf<@Composable () -> Unit>().apply {
                        add {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.NftAsset_ContractAddress),
                                    style = ComposeAppTheme.typography.body,
                                    color = ComposeAppTheme.colors.leah
                                )
                                Spacer(modifier = Modifier.weight(1f))

                                val contractAddress = asset.contract.address

                                val clipboardManager = LocalClipboardManager.current
                                val view = LocalView.current
                                ButtonSecondaryCircle(
                                    icon = R.drawable.ic_copy_20,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(contractAddress))
                                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                val context = LocalContext.current
                                ButtonSecondaryCircle(
                                    icon = R.drawable.ic_share_20,
                                    onClick = {
                                        context.startActivity(Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, contractAddress)
                                            type = "text/plain"
                                        })
                                    }
                                )
                            }
                        }
                        add {
                            DetailItem(
                                stringResource(id = R.string.NftAsset_TokenId),
                                asset.tokenId.shortenedAddress()
                            )
                        }
                        add {
                            DetailItem(
                                stringResource(id = R.string.NftAsset_TokenStandard),
                                asset.contract.type
                            )
                        }
                        add {
                            DetailItem(
                                stringResource(id = R.string.NftAsset_Blockchain),
                                "Ethereum"
                            )
                        }
                    }
                    CellSingleLineLawrenceSection(composableItems = composableItems)
                }

                NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Links)) {

                }

                Spacer(modifier = Modifier.height(32.dp))
                CellFooter(text = stringResource(id = R.string.PoweredBy_OpenSeaAPI))
            }
        }
    }
}

@Composable
private fun DetailItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.grey,
        )
    }
}

@Composable
private fun NftAssetSectionBlock(text: String, content: @Composable () -> Unit) {
    Column {
        Spacer(modifier = Modifier.height(24.dp))
        CellSingleLineClear(borderTop = true) {
            Text(
                text = text,
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah
            )
        }
        content.invoke()
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