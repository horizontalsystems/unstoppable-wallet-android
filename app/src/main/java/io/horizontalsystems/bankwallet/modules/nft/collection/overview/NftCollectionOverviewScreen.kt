package io.horizontalsystems.bankwallet.modules.nft.collection.overview

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.About
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Contracts
import io.horizontalsystems.bankwallet.modules.nft.ui.CellLink
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun NftCollectionOverviewScreen(
    viewModel: NftCollectionViewModel,
    onCopyText: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val collection = viewModel.overviewViewItem

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
                    collection?.let { collection ->
                        LazyColumn {
                            item {
                                Header(collection.name, collection.imageUrl)
                            }
                            item {
                                Stats(collection)
                            }
                            if (collection.description?.isNotBlank() == true) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    About(collection.description)
                                }
                            }
                            if (collection.contracts.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Contracts(
                                        contracts = collection.contracts,
                                        onClickCopy = { onCopyText(it.rawValue) },
                                        onClickExplorer = { onOpenUrl(it) }
                                    )
                                }
                            }
                            if (collection.links.isNotEmpty()) {
                                item {
                                    Links(collection.links, onOpenUrl)
                                }
                            }
                            item {
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

@Composable
private fun Links(links: List<NftCollectionOverviewViewItem.Link>, onClick: (String) -> Unit) {
    Column {
        Spacer(modifier = Modifier.height(24.dp))
        CellSingleLineClear(borderTop = true) {
            body_leah(text = stringResource(id = R.string.NftAsset_Links))
        }
        CellSingleLineLawrenceSection(
            buildList {
                links.forEach { link ->
                    add {
                        CellLink(
                            icon = painterResource(link.icon),
                            title = link.title.getString(),
                            onClick = { onClick(link.url) }
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun Header(name: String, imageUrl: String?) {
    Row(modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 24.dp, end = 16.dp)) {
        Image(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(15.dp)),
            painter = rememberAsyncImagePainter(imageUrl),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
                .align(Alignment.CenterVertically),
            text = name,
            style = ComposeAppTheme.typography.headline1,
            color = ComposeAppTheme.colors.leah,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Stats(collection: NftCollectionOverviewViewItem) {
    Row(Modifier.padding(horizontal = 16.dp)) {
        Card(
            modifier = Modifier
                .height(64.dp)
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            backgroundColor = ComposeAppTheme.colors.lawrence
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                caption_grey(text = stringResource(R.string.NftCollection_Owners))
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = collection.ownersCount,
                    style = ComposeAppTheme.typography.subhead1,
                    color = ComposeAppTheme.colors.bran,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            modifier = Modifier
                .height(64.dp)
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            backgroundColor = ComposeAppTheme.colors.lawrence
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                caption_grey(text = stringResource(R.string.NftCollection_Items))
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = collection.totalSupply,
                    style = ComposeAppTheme.typography.subhead1,
                    color = ComposeAppTheme.colors.bran,
                )
            }
        }
    }

   /* val chartCards = buildList {
        collection.volumeChartDataWrapper?.let { add(it) }
        collection.salesChartDataWrapper?.let { add(it) }
        collection.floorPriceChartDataWrapper?.let { add(it) }
        collection.averagePriceChartDataWrapper?.let { add(it) }
    }

    val rows = chartCards.chunked(2)
    for (row in rows) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.padding(horizontal = 16.dp)) {
            if (row.size == 2) {
                ChartCard(row[0])
                Spacer(modifier = Modifier.width(8.dp))
                ChartCard(row[1])
            } else {
                ChartCard(row[0])
            }
        }
    }*/
}

/*
@Composable
private fun RowScope.ChartCard(chartDataWrapper: NftCollectionOverviewViewItem.ChartDataWrapper) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(12.dp)
    ) {
        caption_grey(text = chartDataWrapper.title)
        Divider(
            modifier = Modifier.padding(top = 10.dp),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = chartDataWrapper.primaryValue,
                style = ComposeAppTheme.typography.subhead1,
                color = ComposeAppTheme.colors.bran
            )
            chartDataWrapper.diff?.let { diff ->
                Text(
                    text = formatValueAsDiff(diff),
                    style = ComposeAppTheme.typography.subhead1,
                    color = diffColor(diff.raw())
                )
            }
        }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = chartDataWrapper.secondaryValue ?: "",
            style = ComposeAppTheme.typography.micro,
            color = ComposeAppTheme.colors.grey
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 17.dp)
                .height(32.dp),
            factory = { context ->
                ChartMinimal(context)
            },
            update = { view ->
                view.doOnLayout {
                    view.setData(chartDataWrapper.chartData)
                }
            }
        )
    }
}
*/
