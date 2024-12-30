package cash.p.terminal.featureStacking.ui.pirateCoinScreen

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.toSpanned
import cash.p.terminal.featureStacking.R
import cash.p.terminal.featureStacking.data.toAnnotatedString
import cash.p.terminal.ui_compose.components.ButtonPrimaryCircle
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellowWithIcon
import cash.p.terminal.ui_compose.components.CardThreeLines
import cash.p.terminal.ui_compose.components.HSCircularProgressIndicator
import cash.p.terminal.ui_compose.theme.ColorDivider
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.chart.ChartModule
import io.horizontalsystems.chartview.chart.ChartUiState
import io.horizontalsystems.chartview.chart.SelectedItem
import io.horizontalsystems.chartview.ui.Chart
import io.horizontalsystems.core.entities.ViewState
import io.horizontalsystems.core.models.HsTimePeriod
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal

@Composable
internal fun PirateCoinScreen(
    onBuyClicked: (Token) -> Unit,
    onCalculatorClicked: () -> Unit,
    onChartClicked: (String) -> Unit,
    viewModel: PirateCoinViewModel = koinViewModel(),
    chartViewModel: PirateChartViewModel = koinViewModel()
) {
    LaunchedEffect(viewModel) { viewModel.loadBalance() }
    LaunchedEffect(viewModel.uiState.value.receiveAddress) {
        viewModel.uiState.value.receiveAddress?.let(chartViewModel::setReceiveAddress)
    }
    PirateCoinScreenContent(
        uiState = viewModel.uiState.value,
        onBuyClicked = onBuyClicked,
        onCalculatorClicked = onCalculatorClicked,
        onChartClicked = onChartClicked,
        graphUIState = chartViewModel.uiState,
        getSelectedPointCallback = chartViewModel::getSelectedPoint,
        onSelectChartInterval = chartViewModel::onSelectChartInterval
    )
}

@Composable
internal fun PirateCoinScreenContent(
    uiState: PirateCoinUIState,
    onBuyClicked: (Token) -> Unit,
    onCalculatorClicked: () -> Unit,
    onChartClicked: (String) -> Unit,
    graphUIState: ChartUiState,
    getSelectedPointCallback: (SelectedItem) -> ChartModule.ChartHeaderView,
    onSelectChartInterval: (HsTimePeriod?) -> Unit
) {
    Column {
        when (uiState.unpaid) {
            null -> {
                LoadingScreen()
            }

            BigDecimal.ZERO -> {
                NoCoins(
                    balance = uiState.balance,
                    onBuyClicked = {
                        uiState.token?.let { onBuyClicked(it) }
                    }
                )
            }

            else -> {
                PirateCoinScreenWithGraph(
                    uiState = uiState,
                    onBuyClicked = {
                        uiState.token?.let { onBuyClicked(it) }
                    },
                    onCalculatorClicked = onCalculatorClicked,
                    onChartClicked = {
                        uiState.token?.let { onChartClicked(it.coin.uid) }
                    },
                    graphUIState = graphUIState,
                    getSelectedPointCallback = getSelectedPointCallback,
                    onSelectChartInterval = onSelectChartInterval
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize()) {
        HSCircularProgressIndicator(
            modifier = Modifier.align(
                Alignment.Center
            )
        )
    }
}

@Composable
private fun NoCoins(balance: BigDecimal, onBuyClicked: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 52.dp),
    ) {
        val waitingForStacking = balance >= BigDecimal(PirateCoinViewModel.MIN_STACKING_AMOUNT)
        val imageRes = if (waitingForStacking) {
            R.drawable.ic_pirate_waiting_stacking
        } else {
            R.drawable.ic_pirate_no_coins
        }
        val noStackingDescription = if (waitingForStacking) {
            annotatedWaitingStakingDescription()
        } else if (balance == BigDecimal.ZERO) {
            stringResource(id = R.string.no_active_stacking_descritpion).toSpanned()
                .toAnnotatedString()
        } else {
            annotatedStakingDescription(BigDecimal(PirateCoinViewModel.MIN_STACKING_AMOUNT) - balance)
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = imageRes),
            modifier = Modifier.size(150.dp),
            contentDescription = null
        )
        Text(
            style = ComposeAppTheme.typography.body.copy(
                fontWeight = FontWeight.Medium
            ),
            color = ComposeAppTheme.colors.leah,
            text = stringResource(id = R.string.no_active_stacking),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.bran.copy(alpha = 0.6f),
            text = noStackingDescription,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        ButtonPrimaryYellowWithIcon(
            title = stringResource(id = R.string.buy_pirate),
            onClick = onBuyClicked,
            icon = R.drawable.ic_swap_24,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        Spacer(modifier = Modifier.weight(3f))
    }
}

@Composable
private fun PirateCoinScreenWithGraph(
    uiState: PirateCoinUIState,
    onBuyClicked: () -> Unit,
    onCalculatorClicked: () -> Unit,
    onChartClicked: () -> Unit,
    graphUIState: ChartUiState,
    getSelectedPointCallback: (SelectedItem) -> ChartModule.ChartHeaderView,
    onSelectChartInterval: (HsTimePeriod?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
//            .fillMaxSize()
//            .verticalScroll(rememberScrollState())
    ) {
        item {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ButtonPrimaryYellowWithIcon(
                    title = stringResource(id = R.string.buy_pirate),
                    onClick = onBuyClicked,
                    icon = R.drawable.ic_swap_24,
                    modifier = Modifier
                        .weight(1f)
                )
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_calculator,
                    contentDescription = stringResource(R.string.stacking_calculator),
                    onClick = onCalculatorClicked
                )
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_chart_24,
                    contentDescription = stringResource(R.string.Coin_Info),
                    onClick = onChartClicked
                )
            }
            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardThreeLines(
                    title = stringResource(id = R.string.total_balance),
                    subtitle = uiState.balance.toPlainString(),
                    description = uiState.secondaryAmount.orEmpty(),
                    modifier = Modifier.weight(1f)
                )
                CardThreeLines(
                    title = stringResource(id = R.string.total_income),
                    subtitle = uiState.totalIncome.toPlainString(),
                    description = uiState.totalIncomeSecondary.orEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardThreeLines(
                    title = stringResource(id = R.string.unpaid),
                    subtitle = uiState.unpaid?.toPlainString().orEmpty(),
                    description = uiState.unpaidSecondary.orEmpty(),
                    modifier = Modifier.weight(1f)
                )
                CardThreeLines(
                    title = stringResource(id = R.string.estimated_annual_interest),
                    subtitle = stringResource(R.string.estimated_annual_interest_value),
                    description = "",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorDivider)
            )
            Text(
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah,
                text = stringResource(id = R.string.investment_chart),
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp)
            )
            Chart(
                uiState = graphUIState,
                getSelectedPointCallback = getSelectedPointCallback,
                onSelectChartInterval = onSelectChartInterval
            )
        }
        item {
            Spacer(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorDivider)
            )
        }
        payoutList(
            payoutItemsMap = uiState.payoutItems
        )
    }
}

@Composable
private fun annotatedStakingDescription(tokenCount: BigDecimal) = buildAnnotatedString {
    append(stringResource(R.string.no_active_stacking_buy_more_descritpion_1))
    withStyle(style = SpanStyle(color = Color(0xFFFF3D43))) {
        append(" ${tokenCount.toPlainString()} ${stringResource(R.string.tokens)} ")
    }
    append(stringResource(R.string.no_active_stacking_buy_more_descritpion_2))
}

@Composable
private fun annotatedWaitingStakingDescription() = buildAnnotatedString {
    append(stringResource(R.string.waiting_for_stacking_1))
    withStyle(style = SpanStyle(color = Color(0xFFFF3D43))) {
        append(" ${stringResource(R.string.waiting_for_stacking_2)}")
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF888888,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    showBackground = true,
    backgroundColor = 0,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PirateCoinScreenContentPreview() {
    ComposeAppTheme {
        PirateCoinScreenContent(
            uiState = PirateCoinUIState(
                balance = BigDecimal(100),
                unpaid = BigDecimal(1)
            ),
            onBuyClicked = {},
            onChartClicked = {},
            graphUIState = ChartUiState(
                chartHeaderView = ChartModule.ChartHeaderView(
                    value = "Value",
                    valueHint = "Value hint",
                    date = "Date",
                    diff = null,
                    extraData = ChartModule.ChartHeaderExtraData.Volume("Volume")
                ),
                tabItems = listOf(),
                loading = false,
                viewState = ViewState.Success,
                hasVolumes = false,
                chartViewType = ChartViewType.Line,
                chartInfoData = null
            ),
            getSelectedPointCallback = {
                ChartModule.ChartHeaderView(
                    value = "Value",
                    valueHint = "Value hint",
                    date = "Date",
                    diff = null,
                    extraData = ChartModule.ChartHeaderExtraData.Volume("Volume")
                )
            },
            onSelectChartInterval = {},
            onCalculatorClicked = {}
        )
    }
}