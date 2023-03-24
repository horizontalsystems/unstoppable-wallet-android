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
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.NftAssetMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftEventMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionFragment
import io.horizontalsystems.bankwallet.modules.nft.collection.events.NftCollectionEventsModule
import io.horizontalsystems.bankwallet.modules.nft.collection.events.NftCollectionEventsViewModel
import io.horizontalsystems.bankwallet.modules.nft.collection.events.NftEventListType
import io.horizontalsystems.bankwallet.modules.nft.collection.events.NftEvents
import io.horizontalsystems.bankwallet.modules.nft.send.SendNftModule
import io.horizontalsystems.bankwallet.modules.nft.ui.CellLink
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController
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
        val collectionUid = requireArguments().getString(NftAssetModule.collectionUidKey)
        val nftUid = requireArguments().getString(NftAssetModule.nftUidKey)?.let { NftUid.fromUid(it) }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                NftAssetScreen(findNavController(), collectionUid, nftUid)
            }
        }
    }
}

@Composable
fun NftAssetScreen(
    navController: NavController,
    collectionUid: String?,
    nftUid: NftUid?
) {
    if (collectionUid == null || nftUid == null) return

    val viewModel = viewModel<NftAssetViewModel>(factory = NftAssetModule.Factory(collectionUid, nftUid))
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
            NftAsset(viewModel, navController)
        }

        errorMessage?.let {
            SnackbarError(it.getString())
            viewModel.errorShown()
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun NftAsset(
    viewModel: NftAssetViewModel,
    navController: NavController
) {
    val pagerState = rememberPagerState(initialPage = 0)
    val coroutineScope = viewModel.viewModelScope

    val tabs = viewModel.tabs
    val selectedTab = tabs[pagerState.currentPage]
    val tabItems = tabs.map {
        TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
    }

    Column {
        Tabs(tabItems, onClick = {
            coroutineScope.launch {
                pagerState.scrollToPage(it.ordinal)
            }
        })

        HorizontalPager(
            count = tabs.size,
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            when (tabs[page]) {
                NftAssetModule.Tab.Overview -> {
                    NftAssetInfo(viewModel, navController, coroutineScope)
                }
                NftAssetModule.Tab.Activity -> {
                    NftAssetEvents(viewModel)
                }
            }
        }
    }
}

@Composable
private fun NftAssetInfo(
    viewModel: NftAssetViewModel,
    navController: NavController,
    coroutineScope: CoroutineScope
) {
    var combinedState = viewModel.viewState

    val model = ImageRequest.Builder(LocalContext.current)
        .data(viewModel.viewItem?.imageUrl)
        .size(Size.ORIGINAL)
        .crossfade(true)
        .build()
    val painter = rememberAsyncImagePainter(model)

    if (combinedState !is ViewState.Error) {
        if (painter.state is AsyncImagePainter.State.Loading) {
            combinedState = ViewState.Loading
        }
    }

    Crossfade(combinedState) { state ->
        when (state) {
            is ViewState.Loading -> {
                Loading()
            }
            is ViewState.Error -> {
                ListErrorView(stringResource(R.string.SyncError), viewModel::refresh)
            }
            else -> {
                AssetContent(painter, viewModel.viewItem, viewModel.nftUid, navController, coroutineScope)
            }
        }
    }
}

@Composable
private fun AssetContent(
    painter: AsyncImagePainter,
    viewItem: NftAssetViewModel.ViewItem?,
    nftUid: NftUid,
    navController: NavController,
    coroutineScope: CoroutineScope,
) {
    val asset = viewItem ?: return
    val context = LocalContext.current
    val view = LocalView.current
    var showActionSelectorDialog by remember { mutableStateOf(false) }

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
                if (painter.state is AsyncImagePainter.State.Success) {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text(
                    text = asset.name,
                    color = ComposeAppTheme.colors.leah,
                    style = ComposeAppTheme.typography.headline1
                )

                CellSingleLine {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                navController.slideFromRight(
                                    R.id.nftCollectionFragment,
                                    NftCollectionFragment.prepareParams(asset.providerCollectionUid, asset.nftUid.blockchainType)
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        subhead1_jacob(text = asset.collectionName)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.grey
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End) {
                    Crossfade(
                        targetState = asset.showSend,
                        modifier = Modifier.weight(1f)
                    ) { showSend ->
                        if (showSend) {
                            Row {
                                ButtonPrimaryYellow(
                                    modifier = Modifier.weight(1f),
                                    title = stringResource(R.string.Button_Send),
                                    onClick = {
                                        navController.slideFromBottom(
                                            R.id.nftSendFragment,
                                            SendNftModule.prepareParams(nftUid.uid)
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        } else {
                            asset.providerUrl?.let { (title, url) ->
                                Row {
                                    ButtonPrimaryDefault(
                                        modifier = Modifier.weight(1f),
                                        title = title,
                                        onClick = {
                                            LinkHelper.openLinkInAppBrowser(context, url)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                        }
                    }
                    ButtonPrimaryCircle(
                        icon = R.drawable.ic_more_24,
                        onClick = {
                            showActionSelectorDialog = true
                        }
                    )
                }
            }
        }

        item {
            Column {
                Spacer(modifier = Modifier.height(24.dp))

                val prices = mutableListOf<Pair<String, NftAssetViewModel.PriceViewItem>>()
                asset.lastSale?.let {
                    prices.add(
                        Pair(
                            stringResource(id = R.string.NftAsset_Price_Purchase),
                            asset.lastSale
                        )
                    )
                }
                asset.average7d?.let {
                    prices.add(
                        Pair(
                            stringResource(id = R.string.NftAsset_Price_Average7d),
                            asset.average7d
                        )
                    )
                }
                asset.average30d?.let {
                    prices.add(
                        Pair(
                            stringResource(id = R.string.NftAsset_Price_Average30d),
                            asset.average30d
                        )
                    )
                }
                asset.collectionFloor?.let {
                    prices.add(
                        Pair(
                            stringResource(id = R.string.NftAsset_Price_Floor),
                            asset.collectionFloor
                        )
                    )
                }

                CellMultilineLawrenceSection(prices) { (title, price) ->
                    NftAssetPriceCell(title, price)
                }

                asset.sale?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    NftAssetSale(it)
                }

                asset.bestOffer?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    NftAssetBestOffer(it)
                }

                if (asset.traits.isNotEmpty()) {
                    NftAssetSectionBlock(text = stringResource(id = R.string.NftAsset_Properties)) {
                        ChipVerticalGrid(
                            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                            spacing = 7.dp
                        ) {
                            asset.traits.forEach {
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
                                body_leah(text = stringResource(id = R.string.NftAsset_ContractAddress))
                                Spacer(modifier = Modifier.weight(1f))

                                val contractAddress = asset.contractAddress

                                val clipboardManager = LocalClipboardManager.current
                                ButtonSecondaryCircle(
                                    icon = R.drawable.ic_copy_20,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(contractAddress))
                                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
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
                                asset.nftUid.tokenId.shorten()
                            )
                        }
                        asset.schemaName?.let { schemaName ->
                            add {
                                DetailItem(
                                    stringResource(id = R.string.NftAsset_TokenStandard),
                                    schemaName
                                )
                            }
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
                    asset.links.forEach { link ->
                        val icon: Painter
                        val title: String
                        when (link.type) {
                            NftAssetViewModel.LinkType.Website -> {
                                icon = painterResource(id = R.drawable.ic_globe_20)
                                title = stringResource(id = R.string.NftAsset_Links_Website)
                            }
                            is NftAssetViewModel.LinkType.Provider -> {
                                icon = painterResource(id = link.type.icon)
                                title = link.type.title
                            }
                            NftAssetViewModel.LinkType.Discord -> {
                                icon = painterResource(id = R.drawable.ic_discord_20)
                                title = stringResource(id = R.string.NftAsset_Links_Discord)
                            }
                            NftAssetViewModel.LinkType.Twitter -> {
                                icon = painterResource(id = R.drawable.ic_twitter_20)
                                title = stringResource(id = R.string.NftAsset_Links_Twitter)
                            }
                        }
                        links.add {
                            CellLink(
                                icon = icon,
                                title = title,
                                onClick = {
                                    LinkHelper.openLinkInAppBrowser(context, link.url)
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

    if (showActionSelectorDialog) {
        SelectorDialogCompose(
            items = NftAssetModule.NftAssetAction.values().map { (TabItem(stringResource(it.title), false, it)) },
            onDismissRequest = {
                showActionSelectorDialog = false
            },
            onSelectItem = { selectedOption ->
                when (selectedOption) {
                    NftAssetModule.NftAssetAction.Share -> {
                        asset.providerUrl?.second?.let {
                            ShareCompat.IntentBuilder(context)
                                .setType("text/plain")
                                .setText(it)
                                .startChooser()
                        }
                    }
                    NftAssetModule.NftAssetAction.Save -> {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val url = asset.imageUrl ?: throw IllegalStateException("No URL!")
                                val fileName = "${asset.collectionName}-${asset.nftUid.tokenId}"
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
                                HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun NftAssetEvents(nftAssetViewModel: NftAssetViewModel) {
    Crossfade(nftAssetViewModel.viewState) { viewState ->
        when (viewState) {
            is ViewState.Error -> {
                ListErrorView(stringResource(R.string.SyncError), nftAssetViewModel::refresh)
            }
            else -> {
                val nftUid = nftAssetViewModel.viewItem?.nftUid ?: return@Crossfade
                val viewModel = viewModel<NftCollectionEventsViewModel>(
                    factory = NftCollectionEventsModule.Factory(
                        NftEventListType.Asset(nftUid),
                        NftEventMetadata.EventType.All
                    )
                )

                NftEvents(viewModel, null, true)
            }
        }
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
private fun NftAssetAttribute(context: Context, trait: NftAssetViewModel.TraitViewItem) {
    Box(
        modifier = Modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(trait.searchUrl != null) {
                LinkHelper.openLinkInAppBrowser(context, trait.searchUrl ?: "")
            }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                body_leah(text = trait.value)
                trait.percent?.let { percent ->
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
            subhead2_grey(text = trait.type)
        }
    }
}

@Composable
private fun NftAssetBestOffer(bestOffer: NftAssetViewModel.PriceViewItem) {
    CellMultilineLawrenceSection {
        NftAssetPriceCell(stringResource(R.string.NftAsset_Price_BestOffer), bestOffer)
    }
}

@Composable
private fun NftAssetSale(sale: NftAssetViewModel.SaleViewItem) {
    val saleComposables = mutableListOf<@Composable () -> Unit>()

    saleComposables.add {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            val title = when (sale.type) {
                NftAssetMetadata.SaleType.OnSale -> stringResource(R.string.Nfts_Asset_OnSale)
                NftAssetMetadata.SaleType.OnAuction ->  stringResource(R.string.Nfts_Asset_OnAuction)
            }

            body_leah(text = title)

            subhead2_grey(
                modifier = Modifier.fillMaxWidth(),
                text = sale.untilDate.getString(),
            )
        }
    }

    saleComposables.add {
        val title = when (sale.type) {
            NftAssetMetadata.SaleType.OnSale -> stringResource(R.string.NftAsset_Price_BuyNow)
            NftAssetMetadata.SaleType.OnAuction ->  stringResource(R.string.NftAsset_Price_MinimumBid)
        }
        NftAssetPriceCell(title, sale.price)
    }

    CellMultilineLawrenceSection(saleComposables)
}

@Composable
private fun DetailItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        body_leah(text = title)
        Spacer(modifier = Modifier.weight(1f))
        subhead1_grey(text = value)
    }
}

@Composable
private fun NftAssetSectionBlock(text: String, content: @Composable () -> Unit) {
    Column {
        Spacer(modifier = Modifier.height(24.dp))
        CellSingleLineClear(borderTop = true) {
            body_leah(text = text)
        }
        content.invoke()
    }
}

@Composable
private fun NftAssetPriceCell(
    title: String,
    price: NftAssetViewModel.PriceViewItem
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        body_leah(text = title)
        Spacer(modifier = Modifier.weight(1f))
        Column {
            body_jacob(
                modifier = Modifier.fillMaxWidth(),
                text = price.coinValue,
                textAlign = TextAlign.End,
            )
            Spacer(modifier = Modifier.height(1.dp))
            subhead2_grey(
                modifier = Modifier.fillMaxWidth(),
                text = price.fiatValue,
                textAlign = TextAlign.End,
            )
        }
    }
}