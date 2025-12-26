package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.coin.RoiViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.marketkit.models.HsTimePeriod
import java.math.BigDecimal

@Composable
fun Roi(roi: List<RoiViewItem>, navController: NavController) {
    SectionUniversalLawrence {
        roi.forEachIndexed { index, item ->
            Box {
                if (index != 0) {
                    HsDivider(
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                Row(
                    modifier = Modifier.height(38.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (item) {
                        is RoiViewItem.HeaderRowViewItem -> {
                            subhead2_grey(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.CoinPage_ROI_Vs),
                                textAlign = TextAlign.Center
                            )
                            item.periods.forEach { period: HsTimePeriod ->
                                VerticalDivider(
                                    modifier = Modifier.height(48.dp).width(1.dp),
                                    thickness = 1.dp,
                                    color = ComposeAppTheme.colors.blade,
                                )
                                caption_grey(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(id = period.periodNameStringResId),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is RoiViewItem.RowViewItem -> {
                            captionSB_leah(
                                modifier = Modifier.weight(1f),
                                text = item.title,
                                textAlign = TextAlign.Center
                            )
                            item.values.forEach { value ->
                                VerticalDivider(
                                    modifier = Modifier.fillMaxHeight().width(1.dp),
                                    thickness = 1.dp,
                                    color = ComposeAppTheme.colors.blade,
                                )
                                val text: String
                                val color: Color
                                if (value != null) {
                                    val sign = if (value >= BigDecimal.ZERO) "+" else "-"
                                    text = App.numberFormatter.format(value.abs(), 0, 2, sign, "%")
                                    color = if (value >= BigDecimal.ZERO) ComposeAppTheme.colors.remus else ComposeAppTheme.colors.lucian

                                } else {
                                    text = "---"
                                    color = ComposeAppTheme.colors.grey
                                }

                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = text,
                                    style = ComposeAppTheme.typography.caption,
                                    color = color,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        CellUniversal(
            onClick = { navController.slideFromRight(R.id.roiSelectCoinsFragment) }
        ) {
            subhead2_leah(text = stringResource(R.string.CoinPage_ROI_SelectCoins))
            HFillSpacer(16.dp)
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = ""
            )
        }
    }

}

val HsTimePeriod.periodNameStringResId: Int
    get() = when (this) {
        HsTimePeriod.Day1 -> R.string.CoinPage_Performance_Day1
        HsTimePeriod.Week1 -> R.string.CoinPage_Performance_Week1
        HsTimePeriod.Week2 -> R.string.CoinPage_Performance_Week2
        HsTimePeriod.Month1 -> R.string.CoinPage_Performance_Month1
        HsTimePeriod.Month3 -> R.string.CoinPage_Performance_Month3
        HsTimePeriod.Month6 -> R.string.CoinPage_Performance_Month6
        HsTimePeriod.Year1 -> R.string.CoinPage_Performance_Year1
        HsTimePeriod.Year2 -> R.string.CoinPage_Performance_Year2
        HsTimePeriod.Year5 -> R.string.CoinPage_Performance_Year5
    }
