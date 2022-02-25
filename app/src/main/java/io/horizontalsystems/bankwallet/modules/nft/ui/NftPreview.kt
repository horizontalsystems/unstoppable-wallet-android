package io.horizontalsystems.bankwallet.modules.nft.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nft.collection.NftAssetItemPricedWithCurrency
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeRatingD

@OptIn(ExperimentalCoilApi::class)
@Composable
fun NftPreview(asset: NftAssetItemPricedWithCurrency, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 4.dp, end = 4.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.steel20)
        ) {
            Image(
                modifier = Modifier.matchParentSize(),
                painter = rememberImagePainter(asset.assetItem.imagePreviewUrl),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
            if (asset.assetItem.onSale) {
                BadgeRatingD(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    text = stringResource(id = R.string.Nfts_Asset_OnSale),
                )
            }
        }
        Text(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp),
            text = asset.assetItem.name,
            style = ComposeAppTheme.typography.microSB,
            color = ComposeAppTheme.colors.grey
        )
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(end = 4.dp),
                text = asset.coinPrice?.getFormatted() ?: "---",
                style = ComposeAppTheme.typography.captionSB,
                color = ComposeAppTheme.colors.leah
            )
            asset.currencyPrice?.let { currencyPrice ->
                Text(
                    text = currencyPrice.getFormatted(),
                    style = ComposeAppTheme.typography.micro,
                    color = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}
