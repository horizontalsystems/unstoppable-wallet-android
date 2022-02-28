package io.horizontalsystems.bankwallet.modules.nft.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nft.collection.NftAssetItemPricedWithCurrency
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionViewItem
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear

fun LazyListScope.nftsCollectionSection(
    collection: NftCollectionViewItem,
    viewModel: NftCollectionsViewModel,
    onClickAsset: (NftAssetItemPricedWithCurrency) -> Unit
) {
    item(key = "${collection.slug}-header") {
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
            Image(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp)),
                painter = rememberImagePainter(collection.imageUrl),
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                text = collection.name,
                style = ComposeAppTheme.typography.headline2,
                color = ComposeAppTheme.colors.leah
            )
            Text(
                text = collection.ownedAssetCount.toString(),
                style = ComposeAppTheme.typography.subhead1,
                color = ComposeAppTheme.colors.grey,
            )

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
            item(key = "${collection.slug}-content-row-$index") {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    assets.forEach { asset ->
                        Box(modifier = Modifier.weight(1f)) {
                            NftPreview(asset) {
                                onClickAsset.invoke(asset)
                            }
                        }
                    }

                    if (assets.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "${collection.slug}-content-bottom-space") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
