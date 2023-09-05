package io.horizontalsystems.bankwallet.modules.info

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.AnalyticInfo
import io.horizontalsystems.bankwallet.modules.info.ui.BulletedText
import io.horizontalsystems.bankwallet.modules.info.ui.InfoBody
import io.horizontalsystems.bankwallet.modules.info.ui.InfoHeader
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable

class CoinAnalyticsInfoFragment : BaseComposeFragment() {

    private val analyticsInfo by lazy {
        requireArguments().parcelable<AnalyticInfo>(analyticsInfoKey)
    }

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            CoinAnalyticsInfoScreen(
                analyticsInfo
            ) { findNavController().popBackStack() }
        }
    }

    companion object {
        private const val analyticsInfoKey = "analyticsInfoKey"

        fun prepareParams(analyticsInfo: AnalyticInfo): Bundle {
            return bundleOf(analyticsInfoKey to analyticsInfo)
        }
    }

}

@Composable
private fun CoinAnalyticsInfoScreen(
    analyticsInfo: AnalyticInfo?,
    onBackPress: () -> Unit
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                analyticsInfo?.let{ info ->
                    InfoHeader(info.title)
                    AnalyticsInfoBody(info)
                    Spacer(Modifier.height(20.dp))
                } ?: run {
                    ScreenMessageWithAction(
                        text = stringResource(R.string.Error),
                        icon = R.drawable.ic_error_48
                    ) {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .padding(horizontal = 48.dp)
                                .fillMaxWidth(),
                            title = stringResource(R.string.Button_Close),
                            onClick = onBackPress
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsInfoBody(info: AnalyticInfo) {
    when(info) {
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
        AnalyticInfo.TechnicalIndicatorsInfo-> {
            BulletedText(R.string.CoinAnalytics_TechIndicators_Info1)
            BulletedText(R.string.CoinAnalytics_TechIndicators_Info2)
            BulletedText(R.string.CoinAnalytics_TechIndicators_Info3)
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
