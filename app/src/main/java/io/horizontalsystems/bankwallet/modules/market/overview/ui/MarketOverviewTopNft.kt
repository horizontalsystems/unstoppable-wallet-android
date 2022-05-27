package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule
import io.horizontalsystems.bankwallet.modules.market.topnftcollections.TopNftCollectionViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinFirstRow
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinSecondRow
import io.horizontalsystems.bankwallet.ui.compose.components.MultilineClear

@Composable
fun TopNftCollectionsBoardView(
    board: MarketOverviewModule.TopNftCollectionsBoard,
    onSelectTimeDuration: (TimeDuration) -> Unit,
    onClickCollection: (String) -> Unit,
    onClickSeeAll: () -> Unit
) {
    TopBoardHeader(
        title = board.title,
        iconRes = board.iconRes,
        select = board.timeDurationSelect,
        onSelect = onSelectTimeDuration
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        board.collections.forEach { collection ->
            TopNftCollectionView(collection) {
                onClickCollection(collection.uid)
            }
        }

        SeeAllButton(onClickSeeAll)
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun TopNftCollectionView(
    collection: TopNftCollectionViewItem,
    onClick: () -> Unit
) {
    MultilineClear(
        onClick = onClick,
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
                collection.floorPrice,
                MarketDataValue.Diff(collection.volumeDiff),
                "${collection.order}"
            )
        }

    }
}
