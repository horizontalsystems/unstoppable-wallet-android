package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.CategoryCard
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.marketkit.models.CoinCategory

@Composable
fun TopSectorsBoardView(
    board: MarketOverviewModule.TopSectorsBoard,
    onClickSeeAll: () -> Unit,
    onItemClick: (CoinCategory) -> Unit
) {
    Divider(
        thickness = 1.dp,
        color = ComposeAppTheme.colors.steel10
    )
    Row(modifier = Modifier.height(42.dp)) {
        Row(
            modifier = Modifier
                    .height(42.dp)
                    .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                    ) { onClickSeeAll.invoke() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.padding(horizontal = 16.dp),
                painter = painterResource(board.iconRes),
                contentDescription = "Section Header Icon"
            )
            body_leah(
                text = stringResource(board.title),
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        ButtonSecondaryDefault(
            title = stringResource(R.string.Market_SeeAll),
            modifier = Modifier.padding(end = 16.dp),
            onClick = onClickSeeAll,
        )
    }

    TopSectorsGrid(board.items, onItemClick)
}

@Composable
private fun TopSectorsGrid(
    items: List<MarketSearchModule.DiscoveryItem.Category>,
    onItemClick: (CoinCategory) -> Unit,
) {

    if (items.isNotEmpty() && items.size >= 2) {
        Row(modifier = Modifier.padding(horizontal = 10.dp)) {
            CategoryCard(items[0]) { onItemClick(items[0].coinCategory) }
            CategoryCard(items[1]) { onItemClick(items[1].coinCategory) }
        }
    }

    if (items.isNotEmpty() && items.size >= 4) {
        Row(modifier = Modifier.padding(horizontal = 10.dp)) {
            CategoryCard(items[2]) { onItemClick(items[2].coinCategory) }
            CategoryCard(items[3]) { onItemClick(items[3].coinCategory) }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))
}
