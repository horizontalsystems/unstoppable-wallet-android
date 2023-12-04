package io.horizontalsystems.bankwallet.ui.compose.components

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.AdviceBlock
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.AdviceViewItem
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.TechnicalIndicatorData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import kotlinx.parcelize.Parcelize

@Composable
fun TechnicalIndicatorsChart(
    rows: List<TechnicalIndicatorData>,
    selectedPeriod: HsPointTimePeriod,
    modifier: Modifier = Modifier,
    navController: NavController,
    onPeriodChange: (HsPointTimePeriod) -> Unit
) {

    Column(modifier = modifier) {
        rows.forEach { row ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    subhead1_grey(row.title)
                    Text(
                        text = stringResource(row.advice.title),
                        style = ComposeAppTheme.typography.subhead1,
                        color = row.advice.color,
                    )
                }
                VSpacer(8.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    row.blocks.forEach { block ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (block.filled) block.type.color else block.type.color20)
                                .weight(1f),
                        )
                    }
                }
                VSpacer(24.dp)
            }
        }
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
            modifier = Modifier.fillMaxWidth()
        )
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            subhead2_grey(
                text = stringResource(R.string.CoinPage_Period),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f)
            )
            ButtonSecondaryWithIcon(
                modifier = Modifier.height(28.dp),
                title = stringResource(selectedPeriod.title),
                iconRight = painterResource(R.drawable.ic_down_arrow_20),
                onClick = {
                    navController.slideFromBottomForResult<PeriodSelectDialog.HsPointTimePeriodParcelable>(
                        R.id.periodSelectDialog,
                        PeriodSelectDialog.HsPointTimePeriodParcelable(selectedPeriod)
                    ) { result ->
                        result.selectedPeriod?.let {
                            onPeriodChange.invoke(it)
                        }
                    }
                }
            )
        }
    }
}

class PeriodSelectDialog : BaseComposableBottomSheetFragment() {
    private val periods = listOf(
        HsPointTimePeriod.Hour1,
        HsPointTimePeriod.Hour4,
        HsPointTimePeriod.Day1,
        HsPointTimePeriod.Week1,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                val navController = findNavController()
                val input = navController.getInput<HsPointTimePeriodParcelable>()
                val selectedPeriod = input?.selectedPeriod ?: periods[2]
                ComposeAppTheme {
                    BottomSheetScreen(
                        periods = periods,
                        selectedItem = selectedPeriod,
                        onSelectListener = { selectedTimePeriod ->
                            navController.setNavigationResultX(HsPointTimePeriodParcelable(selectedTimePeriod))
                        },
                        onCloseClick = { close() }
                    )
                }
            }
        }
    }

    @Parcelize
    data class HsPointTimePeriodParcelable(val selectedPeriodString: String) : Parcelable {
        val selectedPeriod: HsPointTimePeriod?
            get() = HsPointTimePeriod.fromString(selectedPeriodString)
        constructor(selectedPeriod: HsPointTimePeriod) : this(selectedPeriod.value)
    }
}

@Composable
private fun BottomSheetScreen(
    periods: List<HsPointTimePeriod>,
    selectedItem: HsPointTimePeriod,
    onSelectListener: (HsPointTimePeriod) -> Unit,
    onCloseClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_circle_clock_24),
        title = stringResource(R.string.Coin_Analytics_SelectPeriod),
        onCloseClick = onCloseClick,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        Spacer(Modifier.height(12.dp))
        CellUniversalLawrenceSection(periods, showFrame = true) { item ->
            RowUniversal(
                onClick = {
                    onSelectListener.invoke(item)
                    onCloseClick.invoke()
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                body_leah(
                    modifier = Modifier.weight(1f),
                    text = stringResource(item.title)
                )
                if (item == selectedItem) {
                    Image(
                        modifier = Modifier.padding(start = 5.dp),
                        painter = painterResource(id = R.drawable.ic_checkmark_20),
                        colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                        contentDescription = null
                    )
                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}

@Preview
@Composable
private fun TechnicalIndicatorsChart_Preview() {
    val blocks1 = listOf(
        AdviceBlock(type = AdviceViewItem.STRONGSELL, filled = true),
        AdviceBlock(type = AdviceViewItem.SELL, filled = true),
        AdviceBlock(type = AdviceViewItem.NEUTRAL, filled = true),
        AdviceBlock(type = AdviceViewItem.BUY, filled = true),
        AdviceBlock(type = AdviceViewItem.STRONGBUY, filled = true),
    )
    val blocks2 = listOf(
        AdviceBlock(type = AdviceViewItem.STRONGSELL, filled = true),
        AdviceBlock(type = AdviceViewItem.SELL, filled = true),
        AdviceBlock(type = AdviceViewItem.NEUTRAL, filled = true),
        AdviceBlock(type = AdviceViewItem.BUY, filled = true),
        AdviceBlock(type = AdviceViewItem.STRONGBUY, filled = false),
    )
    val blocks3 = listOf(
        AdviceBlock(type = AdviceViewItem.STRONGSELL, filled = true),
        AdviceBlock(type = AdviceViewItem.SELL, filled = true),
        AdviceBlock(type = AdviceViewItem.NEUTRAL, filled = true),
        AdviceBlock(type = AdviceViewItem.BUY, filled = false),
        AdviceBlock(type = AdviceViewItem.STRONGBUY, filled = false),
    )
    val rows = listOf(
        TechnicalIndicatorData(title = "Summary", AdviceViewItem.STRONGBUY, blocks1),
        TechnicalIndicatorData(title = "MA", AdviceViewItem.BUY, blocks2),
        TechnicalIndicatorData(title = "Oscillators", AdviceViewItem.NEUTRAL, blocks3),
    )
    val navController = rememberNavController()
    ComposeAppTheme {
        TechnicalIndicatorsChart(
            rows,
            HsPointTimePeriod.Day1,
            navController = navController,
            onPeriodChange = {}
        )
    }
}
