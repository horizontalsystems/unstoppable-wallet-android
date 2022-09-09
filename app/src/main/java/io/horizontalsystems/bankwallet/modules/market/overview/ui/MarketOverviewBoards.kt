package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinClear
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah

@Composable
fun BoardsView(
    boards: List<MarketOverviewModule.Board>,
    navController: NavController,
    onClickSeeAll: (MarketModule.ListType) -> Unit,
    onSelectTopMarket: (TopMarket, MarketModule.ListType) -> Unit
) {
    boards.forEach { boardItem ->
        TopBoardHeader(
            title = boardItem.boardHeader.title,
            iconRes = boardItem.boardHeader.iconRes,
            select = boardItem.boardHeader.topMarketSelect,
            onSelect = { topMarket -> onSelectTopMarket(topMarket, boardItem.type) },
            onClickSeeAll = { onClickSeeAll(boardItem.type) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ){
            boardItem.marketViewItems.forEach { coin ->
                MarketCoinWithBackground(coin, navController)
            }

            SeeAllButton { onClickSeeAll(boardItem.type) }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun <T : WithTranslatableTitle> TopBoardHeader(
    title: Int,
    iconRes: Int,
    select: Select<T>,
    onSelect: (T) -> Unit,
    onClickSeeAll: () -> Unit
) {
    Column {
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
                        painter = painterResource(iconRes),
                        contentDescription = "Section Header Icon"
                    )
                    body_leah(
                        text = stringResource(title),
                        maxLines = 1,
                    )
            }
            Spacer(Modifier.weight(1f))
            Row(
                    modifier = Modifier.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                    ButtonSecondaryToggle(
                            select = select,
                            onSelect = onSelect
                    )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun MarketCoinWithBackground(
    marketViewItem: MarketViewItem,
    navController: NavController
) {
    MarketCoinClear(
        marketViewItem.coinName,
        marketViewItem.coinCode,
        marketViewItem.iconUrl,
        marketViewItem.iconPlaceHolder,
        marketViewItem.coinRate,
        marketViewItem.marketDataValue,
        marketViewItem.rank
    ) {
        onItemClick(marketViewItem, navController)
    }
}
