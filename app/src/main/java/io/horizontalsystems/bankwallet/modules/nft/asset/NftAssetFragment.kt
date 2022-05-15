package io.horizontalsystems.bankwallet.modules.nft.asset

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.lifecycle.viewModelScope
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
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL


class NftAssetFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val accountId = requireArguments().getString(NftAssetModule.accountIdKey)
        val tokenId = requireArguments().getString(NftAssetModule.tokenIdKey)
        val contractAddress = requireArguments().getString(NftAssetModule.contractAddressKey)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                NftAssetScreen(findNavController(), accountId, tokenId, contractAddress)
            }
        }
    }
}

@Composable
fun NftAssetScreen(
    navController: NavController,
    accountId: String?,
    tokenId: String?,
    contractAddress: String?
) {
    if (accountId == null || tokenId == null || contractAddress == null) return

    val viewModel =
        viewModel<NftAssetViewModel>(factory = NftAssetModule.Factory(accountId, tokenId, contractAddress))
    val viewState = viewModel.viewState
    val errorMessage = viewModel.errorMessage

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
                            viewModel.nftAssetItem?.let { asset ->
                                NftAsset(asset, viewModel.viewModelScope)
                            }
                        }
                    }
                }
            }
        }

        ErrorMessageHud(errorMessage)
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun NftAsset(
    asset: NftAssetModuleAssetItem,
    coroutineScope: CoroutineScope
) {
    val context = LocalContext.current
    val view = LocalView.current

    var nftFileByteArray by remember { mutableStateOf(byteArrayOf()) }

    val pickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    context.contentResolver.openOutputStream(uri).use { outputStream ->
                        outputStream?.write(nftFileByteArray)
                    }
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                }
            }
        }

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
                Text(
                    text = asset.name ?: "#${asset.tokenId}",
                    color = ComposeAppTheme.colors.leah,
                    style = ComposeAppTheme.typography.headline1
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = asset.collectionName,
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead1
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    var showActionSelectorDialog by remember { mutableStateOf(false) }

                    ButtonPrimaryDefault(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.NftAsset_OpenSea),
                        onClick = {
                            asset.assetLinks?.permalink?.let {
                                LinkHelper.openLinkInAppBrowser(context, it)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ButtonPrimaryCircle(
                        icon = R.drawable.ic_more_24,
                        onClick = {
                            showActionSelectorDialog = true
                        }
                    )

                    if (showActionSelectorDialog) {
                        SelectorDialogCompose(
                            items = NftAssetAction.values().map { (TabItem(stringResource(it.title), false, it)) },
                            onDismissRequest = {
                                showActionSelectorDialog = false
                            },
                            onSelectItem = { selectedOption ->
                                when (selectedOption) {
                                    NftAssetAction.Share -> {
                                        asset.assetLinks?.permalink?.let {
                                            ShareCompat.IntentBuilder(context)
                                                .setType("text/plain")
                                                .setText(it)
                                                .startChooser()
                                        }
                                    }
                                    NftAssetAction.Save -> {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val url = asset.imageUrl ?: throw IllegalStateException("No URL!")
                                                val fileName = "${asset.collectionName}-${asset.tokenId}"
                                                var extension: String?

                                                val connection = URL(url).openConnection()
                                                connection.connect()
                                                connection.getInputStream().use { input ->
                                                    val disposition = try {
                                                        connection.getHeaderField("Content-Disposition")
                                                    } catch (e: Exception) {
                                                        null
                                                    }
                                                    val headerFileName = if (disposition != null) {
                                                        val index = disposition.indexOf("filename=")
                                                        if (index > 0) {
                                                            disposition.substring(index + 10, disposition.length - 1)
                                                        } else {
                                                            null
                                                        }
                                                    } else {
                                                        url.substring(url.lastIndexOf("/") + 1, url.length)
                                                    }

                                                    extension = headerFileName?.split(".")?.lastOrNull()
                                                    nftFileByteArray = input.readBytes()
                                                }

                                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                                    addCategory(Intent.CATEGORY_OPENABLE)
                                                    type = connection.contentType
                                                    putExtra(
                                                        Intent.EXTRA_TITLE,
                                                        "$fileName${extension?.let { ".$it" } ?: ""}")
                                                }

                                                pickerLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Column {
                Spacer(modifier = Modifier.height(24.dp))

                val prices = mutableListOf<Pair<String, Price?>>()
                prices.add(
                    Pair(
                        stringResource(id = R.string.NftAsset_Price_Purchase),
                        asset.stats.lastSale
                    )
                )
                prices.add(
                    Pair(
                        stringResource(id = R.string.NftAsset_Price_Average7d),
                        asset.stats.average7d
                    )
                )
                prices.add(
                    Pair(
                        stringResource(id = R.string.NftAsset_Price_Average30d),
                        asset.stats.average30d
                    )
                )
                prices.add(
                    Pair(
                        stringResource(id = R.string.NftAsset_Price_Floor),
                        asset.stats.collectionFloor
                    )
                )

                CellMultilineLawrenceSection(prices) { (title, price) ->
                    NftAssetPriceCell(title, price)
                }

                asset.stats.sale?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    NftAssetSale(it)
                }

                asset.stats.bestOffer?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    NftAssetBestOffer(it)
                }

                if (asset.attributes.isNotEmpty()) {
                    NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Properties)) {
                        ChipVerticalGrid(
                            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                            spacing = 7.dp
                        ) {
                            asset.attributes.forEach {
                                NftAssetAttribute(context, it)
                            }
                        }
                    }
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
                    val links = mutableListOf<@Composable () -> Unit>()
                    asset.assetLinks?.external_link?.let { external_link ->
                        links.add {
                            CellLink(
                                icon = painterResource(id = R.drawable.ic_globe_20),
                                title = stringResource(id = R.string.NftAsset_Links_Website),
                                onClick = {
                                    LinkHelper.openLinkInAppBrowser(context, external_link)
                                }
                            )
                        }
                    }
                    asset.assetLinks?.permalink?.let { permalink ->
                        links.add {
                            CellLink(
                                icon = painterResource(id = R.drawable.ic_opensea_20),
                                title = stringResource(id = R.string.NftAsset_Links_OpenSea),
                                onClick = {
                                    LinkHelper.openLinkInAppBrowser(context, permalink)
                                }
                            )
                        }
                    }
                    asset.collectionLinks?.discord_url?.let { discord_url ->
                        links.add {
                            CellLink(
                                icon = painterResource(id = R.drawable.ic_discord_20),
                                title = stringResource(id = R.string.NftAsset_Links_Discord),
                                onClick = {
                                    LinkHelper.openLinkInAppBrowser(context, discord_url)
                                }
                            )
                        }
                    }
                    asset.collectionLinks?.twitter_username?.let { twitter_username ->
                        links.add {
                            CellLink(
                                icon = painterResource(id = R.drawable.ic_twitter_20),
                                title = stringResource(id = R.string.NftAsset_Links_Twitter),
                                onClick = {
                                    LinkHelper.openLinkInAppBrowser(context, "https://twitter.com/$twitter_username")
                                }
                            )
                        }
                    }
                    CellSingleLineLawrenceSection(links)
                }

                Spacer(modifier = Modifier.height(32.dp))
                CellFooter(text = stringResource(id = R.string.PoweredBy_OpenSeaAPI))
            }
        }
    }
}

@Composable
private fun ErrorMessageHud(errorMessage: TranslatableString?) {
    errorMessage?.let {
        HudHelper.showErrorMessage(LocalView.current, it.getString())
    }
}

@Composable
private fun ChipVerticalGrid(
    modifier: Modifier = Modifier,
    spacing: Dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        var currentRow = 0
        var currentOrigin = IntOffset.Zero
        val spacingValue = spacing.toPx().toInt()
        val placeables = measurables.map { measurable ->
            val placeable = measurable.measure(constraints)

            if (currentOrigin.x > 0f && currentOrigin.x + placeable.width > constraints.maxWidth) {
                currentRow += 1
                currentOrigin = currentOrigin.copy(x = 0, y = currentOrigin.y + placeable.height + spacingValue)
            }

            placeable to currentOrigin.also {
                currentOrigin = it.copy(x = it.x + placeable.width + spacingValue)
            }
        }

        layout(
            width = constraints.maxWidth,
            height = placeables.lastOrNull()?.run { first.height + second.y } ?: 0
        ) {
            placeables.forEach {
                val (placeable, origin) = it
                placeable.place(origin.x, origin.y)
            }
        }
    }
}

@Composable
private fun NftAssetAttribute(context: Context, attribute: NftAssetModuleAssetItem.Attribute) {
    Box(
        modifier = Modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable {
                LinkHelper.openLinkInAppBrowser(context, attribute.searchUrl)
            }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = attribute.value,
                    color = ComposeAppTheme.colors.leah,
                    style = ComposeAppTheme.typography.body,
                )
                attribute.percent?.let { percent ->
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ComposeAppTheme.colors.jeremy)
                    ) {
                        Text(
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 1.dp),
                            text = percent,
                            color = ComposeAppTheme.colors.bran,
                            style = ComposeAppTheme.typography.microSB,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                text = attribute.type,
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
            )
        }
    }
}

@Composable
private fun NftAssetBestOffer(bestOffer: Price) {
    CellMultilineLawrenceSection {
        NftAssetPriceCell(stringResource(R.string.NftAsset_Price_BestOffer), bestOffer)
    }
}

@Composable
private fun NftAssetSale(sale: Sale) {
    val saleComposables = mutableListOf<@Composable () -> Unit>()

    saleComposables.add {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.Nfts_Asset_OnSale),
                color = ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.body,
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.Nfts_Asset_OnSaleUntil,
                    sale.untilDate?.let { DateHelper.getFullDate(it) } ?: "---"),
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
            )
        }
    }

    saleComposables.add {
        val title = when (sale.type) {
            Sale.PriceType.BuyNow -> stringResource(R.string.NftAsset_Price_BuyNow)
            Sale.PriceType.TopBid -> stringResource(R.string.NftAsset_Price_TopBid)
            Sale.PriceType.MinimumBid -> stringResource(R.string.NftAsset_Price_MinimumBid)
        }
        NftAssetPriceCell(title, sale.price)
    }

    CellMultilineLawrenceSection(saleComposables)
}

@Composable
private fun CellLink(icon: Painter, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = icon,
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
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
    price: Price?
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
                text = price?.coinValue?.getFormatted(4) ?: "---",
                color = ComposeAppTheme.colors.jacob,
                style = ComposeAppTheme.typography.body,
                textAlign = TextAlign.End,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = price?.currencyValue?.getFormatted() ?: "---",
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
                textAlign = TextAlign.End,
            )
        }
    }
}