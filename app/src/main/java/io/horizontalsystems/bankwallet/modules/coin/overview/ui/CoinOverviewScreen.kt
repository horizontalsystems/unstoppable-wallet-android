package io.horizontalsystems.bankwallet.modules.coin.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.CoinDataItem
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.modules.coin.RoiViewItem
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinSubtitleAdapter
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.*
import io.horizontalsystems.bankwallet.ui.compose.components.CellFooter

@Composable
fun CoinOverviewScreen(
    subtitle: CoinSubtitleAdapter.ViewItemWrapper,
    marketData: List<CoinDataItem>,
    roi: List<RoiViewItem>,
    categories: List<String>,
    contractInfo: List<ContractInfo>,
    aboutText: String,
    links: List<CoinLink>,
    onCoinLinkClick: (CoinLink) -> Unit,
    showFooter: Boolean,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Title(subtitle)

        if (marketData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            MarketData(marketData)
        }

        if (roi.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Roi(roi)
        }

        if (categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Categories(categories)
        }

        if (contractInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Contracts(contractInfo)
        }

        if (aboutText.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
//            About(aboutText)
        }

        if (links.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Links(links, onCoinLinkClick)
        }

        if (showFooter) {
            Spacer(modifier = Modifier.height(32.dp))
            CellFooter(text = stringResource(id = R.string.Market_PoweredByApi))
        }
    }
}
