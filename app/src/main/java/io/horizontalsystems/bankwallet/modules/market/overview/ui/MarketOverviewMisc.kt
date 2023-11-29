package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah

fun onItemClick(marketViewItem: MarketViewItem, navController: NavController) {
    val arguments = CoinFragment.prepareParams(marketViewItem.coinUid, "market_overview")
    navController.slideFromRight(R.id.coinFragment, arguments)
}

@Composable
fun SeeAllButton(onClick: () -> Unit) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        body_leah(
            text = stringResource(R.string.Market_SeeAll),
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = "right arrow icon",
        )
    }
}
