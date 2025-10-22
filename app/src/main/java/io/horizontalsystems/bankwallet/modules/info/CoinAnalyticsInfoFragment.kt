package io.horizontalsystems.bankwallet.modules.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.AnalyticInfo
import io.horizontalsystems.bankwallet.modules.info.ui.BulletedText
import io.horizontalsystems.bankwallet.modules.info.ui.InfoBody
import io.horizontalsystems.bankwallet.modules.info.ui.InfoHeader
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class CoinAnalyticsInfoFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<AnalyticInfo>(navController) { input ->
            CoinAnalyticsInfoScreen(input) { navController.popBackStack() }
        }
    }
}

@Composable
private fun CoinAnalyticsInfoScreen(
    analyticsInfo: AnalyticInfo,
    onBackPress: () -> Unit
) {
    HSScaffold(
        title = "",
        onBack = onBackPress,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            InfoHeader(analyticsInfo.title)
            AnalyticsInfoBody(analyticsInfo)
            VSpacer(20.dp)
        }
    }
}

@Composable
private fun AnalyticsInfoBody(info: AnalyticInfo) {
    when (info) {
        AnalyticInfo.CexVolumeInfo -> {
            BulletedText(R.string.CoinAnalytics_CexVolume_Info1)
            BulletedText(R.string.CoinAnalytics_CexVolume_Info2)
            BulletedText(R.string.CoinAnalytics_CexVolume_Info3)
            BulletedText(R.string.CoinAnalytics_CexVolume_Info4)
        }

        AnalyticInfo.DexVolumeInfo -> {
            BulletedText(R.string.CoinAnalytics_DexVolume_Info1)
            BulletedText(R.string.CoinAnalytics_DexVolume_Info2)
            BulletedText(R.string.CoinAnalytics_DexVolume_Info3)
            BulletedText(R.string.CoinAnalytics_DexVolume_Info4)
            InfoBody(R.string.CoinAnalytics_DexVolume_TrackedDexes)
            BulletedText(R.string.CoinAnalytics_DexVolume_TrackedDexes_Info1)
            BulletedText(R.string.CoinAnalytics_DexVolume_TrackedDexes_Info2)
        }

        AnalyticInfo.DexLiquidityInfo -> {
            BulletedText(R.string.CoinAnalytics_DexLiquidity_Info1)
            BulletedText(R.string.CoinAnalytics_DexLiquidity_Info2)
            BulletedText(R.string.CoinAnalytics_DexLiquidity_Info3)
            InfoBody(R.string.CoinAnalytics_DexLiquidity_TrackedDexes)
            BulletedText(R.string.CoinAnalytics_DexLiquidity_TrackedDexes_Info1)
            BulletedText(R.string.CoinAnalytics_DexLiquidity_TrackedDexes_Info2)
        }

        AnalyticInfo.AddressesInfo -> {
            BulletedText(R.string.CoinAnalytics_ActiveAddresses_Info1)
            BulletedText(R.string.CoinAnalytics_ActiveAddresses_Info2)
            BulletedText(R.string.CoinAnalytics_ActiveAddresses_Info3)
            BulletedText(R.string.CoinAnalytics_ActiveAddresses_Info4)
            BulletedText(R.string.CoinAnalytics_ActiveAddresses_Info5)
        }

        AnalyticInfo.TransactionCountInfo -> {
            BulletedText(R.string.CoinAnalytics_TransactionCount_Info1)
            BulletedText(R.string.CoinAnalytics_TransactionCount_Info2)
            BulletedText(R.string.CoinAnalytics_TransactionCount_Info3)
            BulletedText(R.string.CoinAnalytics_TransactionCount_Info4)
            BulletedText(R.string.CoinAnalytics_TransactionCount_Info5)
        }

        AnalyticInfo.HoldersInfo -> {
            BulletedText(R.string.CoinAnalytics_Holders_Info1)
            BulletedText(R.string.CoinAnalytics_Holders_Info2)
            InfoBody(R.string.CoinAnalytics_Holders_TrackedBlockchains)
        }

        AnalyticInfo.TvlInfo -> {
            BulletedText(R.string.CoinAnalytics_ProjectTVL_Info1)
            BulletedText(R.string.CoinAnalytics_ProjectTVL_Info2)
            BulletedText(R.string.CoinAnalytics_ProjectTVL_Info3)
            BulletedText(R.string.CoinAnalytics_ProjectTVL_Info4)
            BulletedText(R.string.CoinAnalytics_ProjectTVL_Info5)
        }

        AnalyticInfo.TechnicalIndicatorsInfo -> {
            InfoBody(R.string.TechnicalAdvice_InfoDescription)
        }
    }
}

@Preview
@Composable
private fun Preview_CoinAnalyticsInfoScreen() {
    ComposeAppTheme {
        CoinAnalyticsInfoScreen(
            AnalyticInfo.CexVolumeInfo,
        ) {}
    }
}
