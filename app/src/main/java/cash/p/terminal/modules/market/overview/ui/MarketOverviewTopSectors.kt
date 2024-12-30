package cash.p.terminal.modules.market.overview.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import cash.p.terminal.modules.market.overview.MarketOverviewModule
import cash.p.terminal.ui.compose.components.CategoryCard
import cash.p.terminal.wallet.models.CoinCategory

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
