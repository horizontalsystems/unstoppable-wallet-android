package io.horizontalsystems.bankwallet.modules.market.overview.ui

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

fun onItemClick(marketViewItem: MarketViewItem, navController: NavController) {
    val arguments = CoinFragment.prepareParams(marketViewItem.coinUid)
    navController.slideFromRight(R.id.coinFragment, arguments)
}

fun getRoundedCornerShape(firstItem: Boolean): RoundedCornerShape {
    return if (firstItem) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    } else {
        RoundedCornerShape(0.dp)
    }
}

@Composable
fun SeeAllButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.Market_SeeAll),
                color = ComposeAppTheme.colors.oz,
                style = ComposeAppTheme.typography.body,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "right arrow icon",
            )
        }
    }
}
