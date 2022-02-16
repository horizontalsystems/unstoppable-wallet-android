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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.nft.collection.NftAssetItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.Coin
import java.math.BigDecimal

@OptIn(ExperimentalCoilApi::class)
@Composable
fun NftPreview(asset: NftAssetItem, onClick: () -> Unit) {
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
                painter = rememberImagePainter(asset.imagePreviewUrl),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
            if (asset.onSale) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    text = stringResource(id = R.string.Nfts_Asset_OnSale),
                )
            }
        }
        Text(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp),
            text = asset.name,
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

private val asset = NftAssetItem(
    accountId = "accountId",
    tokenId = "108510973921457929967077298367545831468135648058682555520544982493970263179265",
    name = "Crypto Punk 312",
    imageUrl = "https://lh3.googleusercontent.com/FalCKtVbAX1qBf2_O7g72UufouUsMStkpYfDAe3O-4OO06O4ESwcv63GAnKmEslOaaE4XUyy4X1xdc5CqDFtmDYVwXEFE5P9pUi_",
    imagePreviewUrl = "https://lh3.googleusercontent.com/FalCKtVbAX1qBf2_O7g72UufouUsMStkpYfDAe3O-4OO06O4ESwcv63GAnKmEslOaaE4XUyy4X1xdc5CqDFtmDYVwXEFE5P9pUi_",
    ownedCount = 2,
    coinPrice = CoinValue(CoinValue.Kind.Coin(Coin("", "Ethereum", "ETH"), 8), BigDecimal("112.2979871")),
    currencyPrice = CurrencyValue(Currency("USD", "$", 2), BigDecimal("112.2979871")),
    onSale = false,
    prices = NftAssetItem.Prices(
        average7d = null,
        average30d = null,
        last = null
    )
)

@Preview
@Composable
fun NftPreviewPreview() {
    ComposeAppTheme {
        NftPreview(asset, {})
    }
}