package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinFirstRow
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinSecondRow
import io.horizontalsystems.bankwallet.ui.compose.components.MultilineClear

@Composable
fun TopNftCollectionsBoardView(
    board: MarketOverviewModule.TopNftCollectionsBoard,
    onSelectTimeDuration: (TimeDuration) -> Unit,
    onClickSeeAll: () -> Unit
) {
    TopBoardHeader(
        title = board.title,
        iconRes = board.iconRes,
        select = board.timeDurationSelect,
        onSelect = onSelectTimeDuration
    )

    board.collections.forEachIndexed { index, collection ->
        TopNftCollectionView(collection, firstItem = index == 0)
    }

    SeeAllButton(onClickSeeAll)
}

@Composable
private fun TopNftCollectionView(
    collection: MarketOverviewModule.TopNftCollectionViewItem,
    firstItem: Boolean
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(getRoundedCornerShape(firstItem))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        MultilineClear(
            onClick = { },
            borderBottom = true
        ) {
            CoinImage(
                iconUrl = collection.imageUrl ?: "",
                placeholder = R.drawable.coin_placeholder,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                MarketCoinFirstRow(collection.name, collection.volume)
                Spacer(modifier = Modifier.height(3.dp))
                MarketCoinSecondRow(
                    collection.floorPrice ?: "",
                    MarketDataValue.Diff(collection.volumeDiff),
                    "${collection.order}"
                )
            }
        }
    }
}
