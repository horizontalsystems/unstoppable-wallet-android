package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.overview.TopPairViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinFirstRow
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinSecondRow
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemBorderedRowUniversalClear

@Composable
fun TopPairsBoardView(
    topMarketPairs: List<TopPairViewItem>,
    onItemClick: (TopPairViewItem) -> Unit,
    onClickSeeAll: () -> Unit
) {
    MarketsSectionHeader(
        title = R.string.Market_Overview_TopPairs,
        icon = painterResource(R.drawable.ic_pairs_24),
        onClick = onClickSeeAll
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        topMarketPairs.forEach {
            TopPairItem(item = it, borderBottom = true, onItemClick = onItemClick)
        }

        SeeAllButton(onClickSeeAll)
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun TopPairItem(
    item: TopPairViewItem,
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    onItemClick: (TopPairViewItem) -> Unit,
) {
    SectionItemBorderedRowUniversalClear(
        borderTop = borderTop,
        borderBottom = borderBottom,
        onClick = { onItemClick(item) }
    ) {
        CoinImage(
            iconUrl = item.iconUrl,
            placeholder = R.drawable.ic_platform_placeholder_32,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            MarketCoinFirstRow(item.title, item.volume)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(
                subtitle = item.name,
                marketDataValue = item.price?.let { MarketDataValue.Volume(it) },
                label = item.rank
            )
        }
    }
}
