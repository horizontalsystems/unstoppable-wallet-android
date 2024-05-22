package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.overview.TopPairViewItem
import io.horizontalsystems.bankwallet.modules.market.toppairs.TopPairItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

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

