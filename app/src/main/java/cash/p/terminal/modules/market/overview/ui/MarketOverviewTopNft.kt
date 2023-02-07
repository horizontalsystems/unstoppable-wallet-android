package cash.p.terminal.modules.market.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.market.MarketDataValue
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.overview.MarketOverviewModule
import cash.p.terminal.modules.market.topnftcollections.TopNftCollectionViewItem
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.MarketCoinFirstRow
import cash.p.terminal.ui.compose.components.MarketCoinSecondRow
import cash.p.terminal.ui.compose.components.NftIcon
import cash.p.terminal.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun TopNftCollectionsBoardView(
    board: MarketOverviewModule.TopNftCollectionsBoard,
    onSelectTimeDuration: (TimeDuration) -> Unit,
    onClickCollection: (BlockchainType, String) -> Unit,
    onClickSeeAll: () -> Unit
) {
    TopBoardHeader(
        title = board.title,
        iconRes = board.iconRes,
        select = board.timeDurationSelect,
        onSelect = onSelectTimeDuration,
        onClickSeeAll = onClickSeeAll
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
                onClickCollection(collection.blockchainType, collection.uid)
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
    SectionItemBorderedRowUniversalClear(
        onClick = onClick,
        borderBottom = true
    ) {
        NftIcon(
            iconUrl = collection.imageUrl ?: "",
            placeholder = R.drawable.coin_placeholder,
            modifier = Modifier.padding(end = 16.dp)
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
