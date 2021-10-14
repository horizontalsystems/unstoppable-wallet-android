package io.horizontalsystems.bankwallet.modules.coin.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.CoinDataItem
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.modules.coin.RoiViewItem
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinSubtitleAdapter
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellFooter
import io.horizontalsystems.core.entities.Currency

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
    loading: Boolean,
    coinInfoError: String,
    chartInfo: CoinChartAdapter.ViewItemWrapper?,
    currency: Currency,
    listener: CoinChartAdapter.Listener
) {
    if (loading) {
        Loading()
    } else if (coinInfoError.isNotEmpty()) {
        Error(coinInfoError)
    } else {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            Title(subtitle)

            if (chartInfo != null) {
                ChartInfo(chartInfo, currency, listener)
            }

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
                About(aboutText)
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
}

@Preview
@Composable
fun LoadingPreview() {
    ComposeAppTheme {
        Loading()
    }
}

@Composable
fun Error(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey,
        )
    }
}

@Composable
fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 2.dp
        )
    }
}
