package io.horizontalsystems.bankwallet.modules.nft.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nft.collection.NftAssetItemPriced
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionViewItem
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear

@OptIn(ExperimentalCoilApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun NftsCollectionSection(
    collection: NftCollectionViewItem,
    viewModel: NftCollectionsViewModel,
    onClickAsset: (NftAssetItemPriced) -> Unit
) {
    Column {
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

        AnimatedVisibility(visible = collection.expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                collection.assets.chunked(2).forEach {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        it.forEach { asset ->
                            Box(modifier = Modifier.weight(1f)) {
                                NftPreview(asset) {
                                    onClickAsset.invoke(asset)
                                }
                            }
                        }

                        if (it.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
