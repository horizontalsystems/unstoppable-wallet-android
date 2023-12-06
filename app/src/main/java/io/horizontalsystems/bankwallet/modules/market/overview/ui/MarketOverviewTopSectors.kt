package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule
import io.horizontalsystems.bankwallet.ui.compose.components.CategoryCard
import io.horizontalsystems.marketkit.models.CoinCategory

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopSectorsBoardView(
    board: MarketOverviewModule.TopSectorsBoard,
    onItemClick: (CoinCategory) -> Unit
) {
    MarketsSectionHeader(
        title = board.title,
        icon = painterResource(board.iconRes),
    )

    MarketsHorizontalCards(board.items.size) {
        val category = board.items[it]
        CategoryCard(category) {
            onItemClick(category.coinCategory)
        }
    }
}
