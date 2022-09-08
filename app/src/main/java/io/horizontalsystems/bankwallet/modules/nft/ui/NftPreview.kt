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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeRatingD
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_leah

@Composable
fun NftAssetPreview(
    name: String,
    imageUrl: String?,
    onSale: Boolean,
    coinPrice: CoinValue?,
    currencyPrice: CurrencyValue?,
    onClick: () -> Unit
) {
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
                .height(156.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.steel20)
        ) {
            val painter = rememberAsyncImagePainter(imageUrl)
            if (painter.state !is AsyncImagePainter.State.Success) {
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp)
                        .align(Alignment.Center),
                    text = name,
                    style = ComposeAppTheme.typography.microSB,
                    color = ComposeAppTheme.colors.grey
                )
            }
            Image(
                modifier = Modifier.matchParentSize(),
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            if (onSale) {
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
            text = name,
            style = ComposeAppTheme.typography.microSB,
            color = ComposeAppTheme.colors.grey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            captionSB_leah(
                modifier = Modifier.padding(end = 4.dp),
                text = coinPrice?.getFormattedFull() ?: "---",
            )
            currencyPrice?.let { currencyPrice ->
                Text(
                    text = currencyPrice.getFormattedFull(),
                    style = ComposeAppTheme.typography.micro,
                    color = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}
