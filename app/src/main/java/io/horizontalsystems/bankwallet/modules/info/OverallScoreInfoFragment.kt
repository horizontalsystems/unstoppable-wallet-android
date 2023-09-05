package io.horizontalsystems.bankwallet.modules.info

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.OverallScore
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.ScoreCategory
import io.horizontalsystems.bankwallet.modules.info.ui.InfoHeader
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_jacob
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable
import java.math.BigDecimal

class OverallScoreInfoFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val scoreCategory = requireArguments().parcelable<ScoreCategory>(SCORE_CATEGORY_KEY)
        val categoryScores = getScores(scoreCategory)
        ComposeAppTheme {
            if (scoreCategory == null) {
                ScreenMessageWithAction(
                    text = stringResource(R.string.Error),
                    icon = R.drawable.ic_error_48
                ) {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .padding(horizontal = 48.dp)
                            .fillMaxWidth(),
                        title = stringResource(R.string.Button_Close),
                        onClick = { findNavController().popBackStack() }
                    )
                }
            } else {
                InfoScreen(
                    scoreCategory.title,
                    scoreCategory.description,
                    categoryScores,
                    findNavController()
                )
            }
        }
    }

    companion object {
        private const val SCORE_CATEGORY_KEY = "score_category_key"

        fun prepareParams(scoreCategory: ScoreCategory) = bundleOf(SCORE_CATEGORY_KEY to scoreCategory)
    }

}

@Composable
private fun InfoScreen(
    categoryTitle: Int,
    description: Int,
    categoryScores: Map<OverallScore, String>,
    navController: NavController
) {
    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoHeader(R.string.Coin_Analytics_OverallScore)
                VSpacer(12.dp)
                headline2_jacob(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(categoryTitle)
                )
                InfoTextBody(stringResource(description))
                VSpacer(12.dp)
                val items = buildList<@Composable () -> Unit> {
                    categoryScores.forEach { (score, value) ->
                        val color = when (score) {
                            OverallScore.Excellent -> Color(0xFF05C46B)
                            OverallScore.Good -> Color(0xFFFFA800)
                            OverallScore.Fair -> Color(0xFFFF7A00)
                            OverallScore.Poor -> Color(0xFFFF3D00)
                        }
                        add {
                            RowUniversal(
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                Image(
                                    painter = painterResource(score.icon),
                                    contentDescription = null
                                )
                                HSpacer(8.dp)
                                Text(
                                    text = stringResource(score.title).uppercase(),
                                    style = ComposeAppTheme.typography.subhead1,
                                    color = color,
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = value,
                                    style = ComposeAppTheme.typography.subhead1,
                                    color = color,
                                )
                            }
                        }
                    }
                }
                CellUniversalLawrenceSection(items)
                VSpacer(24.dp)
            }
        }
    }
}

private fun formatUsd(number: Int): String {
    val symbol = "$"
    val decimals = 0

    return App.numberFormatter.formatFiatShort(BigDecimal(number), symbol, decimals)
}

private fun formatNumber(number: Int): String {
    return App.numberFormatter.formatNumberShort(BigDecimal(number), 2)
}

private fun getScores(scoreCategory: ScoreCategory?): Map<OverallScore, String> {
    return when (scoreCategory) {
        ScoreCategory.CexScoreCategory -> {
            mapOf(
                OverallScore.Excellent to "> " + formatUsd(10_000_000),
                OverallScore.Good to "> " + formatUsd(5_000_000),
                OverallScore.Fair to "> " + formatUsd(1_000_000),
                OverallScore.Poor to "< " + formatUsd(1_000_000),
            )
        }

        ScoreCategory.DexVolumeScoreCategory -> {
            mapOf(
                OverallScore.Excellent to "> " + formatUsd(1_000_000),
                OverallScore.Good to "> " + formatUsd(500_000),
                OverallScore.Fair to "> " + formatUsd(100_000),
                OverallScore.Poor to "< " + formatUsd(100_000),
            )
        }

        ScoreCategory.DexLiquidityScoreCategory -> {
            mapOf(
                OverallScore.Excellent to "> " + formatUsd(1_000_000),
                OverallScore.Good to "> " + formatUsd(1_000_000),
                OverallScore.Fair to "> " + formatUsd(500_000),
                OverallScore.Poor to "< " + formatUsd(500_000),
            )
        }

        ScoreCategory.AddressesScoreCategory -> {
            mapOf(
                OverallScore.Excellent to "> 500",
                OverallScore.Good to "> 200",
                OverallScore.Fair to "> 100",
                OverallScore.Poor to "< 100",
            )
        }

        ScoreCategory.TransactionCountScoreCategory -> {
            mapOf(
                OverallScore.Excellent to "> " + formatNumber(10_000),
                OverallScore.Good to "> " + formatNumber(5_000),
                OverallScore.Fair to "> " + formatNumber(1_000),
                OverallScore.Poor to "< " + formatNumber(1_000),
            )
        }

        ScoreCategory.HoldersScoreCategory -> {
            mapOf(
                OverallScore.Excellent to "> " + formatNumber(100_000),
                OverallScore.Good to "> " + formatNumber(50_000),
                OverallScore.Fair to "> " + formatNumber(30_000),
                OverallScore.Poor to "< " + formatNumber(30_000),
            )
        }

        ScoreCategory.TvlScoreCategory -> {
            mapOf(
                OverallScore.Excellent to "> " + formatUsd(200_000_000),
                OverallScore.Good to "> " + formatUsd(100_000_000),
                OverallScore.Fair to "> " + formatUsd(50_000_000),
                OverallScore.Poor to "< " + formatUsd(50_000_000),
            )
        }

        null -> emptyMap()
    }
}
