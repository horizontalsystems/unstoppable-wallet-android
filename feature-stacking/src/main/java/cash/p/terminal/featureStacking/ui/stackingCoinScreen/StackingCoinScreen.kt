package cash.p.terminal.featureStacking.ui.stackingCoinScreen

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.toSpanned
import cash.p.terminal.featureStacking.R
import cash.p.terminal.featureStacking.data.toAnnotatedString
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.ui_compose.components.ButtonPrimaryCircle
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellowWithIcon
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSCircularProgressIndicator
import cash.p.terminal.ui_compose.components.HsImage
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.TitleAndTwoValuesCell
import cash.p.terminal.ui_compose.components.TitleAndValueCell
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.theme.ColorDivider
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.alternativeImageUrl
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.imagePlaceholder
import cash.p.terminal.wallet.imageUrl
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.chart.ChartModule
import io.horizontalsystems.chartview.chart.ChartUiState
import io.horizontalsystems.chartview.chart.SelectedItem
import io.horizontalsystems.chartview.ui.Chart
import io.horizontalsystems.core.entities.ViewState
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.models.HsTimePeriod
import java.math.BigDecimal

@Composable
internal fun StackingCoinScreen(
    onBuyClicked: (Token) -> Unit,
    onCalculatorClicked: () -> Unit,
    onChartClicked: (String) -> Unit,
    viewModel: StackingCoinViewModel,
    chartViewModel: StackingCoinChartViewModel
) {
    LaunchedEffect(viewModel) { viewModel.loadData() }
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
        onSelectChartInterval = chartViewModel::onSelectChartInterval,
        onToggleBalanceVisibility = viewModel::toggleBalanceVisibility
    )
}

@Composable
internal fun PirateCoinScreenContent(
    uiState: StackingCoinUIState,
    onBuyClicked: (Token) -> Unit,
    onCalculatorClicked: () -> Unit,
    onChartClicked: (String) -> Unit,
    graphUIState: ChartUiState,
    getSelectedPointCallback: (SelectedItem) -> ChartModule.ChartHeaderView,
    onSelectChartInterval: (HsTimePeriod?) -> Unit,
    onToggleBalanceVisibility: () -> Unit
) {
    Column {
        if (uiState.loading) {
            LoadingScreen()
        } else {
            if (uiState.balance == BigDecimal.ZERO && uiState.payoutItems.isEmpty()) {
                NoCoins(
                    uiState = uiState,
                    onBuyClicked = {
                        uiState.token?.let { onBuyClicked(it) }
                    }
                )
            } else {
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
                    onSelectChartInterval = onSelectChartInterval,
                    onToggleBalanceVisibility = onToggleBalanceVisibility
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
private fun NoCoins(uiState: StackingCoinUIState, onBuyClicked: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 52.dp),
    ) {
        val stringResId = if (uiState.stackingType == StackingType.PCASH) {
            R.string.no_active_stacking_pirate_description
        } else {
            R.string.no_active_stacking_cosanta_description
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.ic_no_investment),
            modifier = Modifier.size(100.dp),
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
            text = stringResource(id = stringResId).toSpanned()
                .toAnnotatedString(),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        ButtonPrimaryYellowWithIcon(
            title = stringResource(
                id = if (uiState.stackingType == StackingType.PCASH) {
                    R.string.buy_pirate
                } else {
                    R.string.buy_cosanta
                }
            ),
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
    uiState: StackingCoinUIState,
    onBuyClicked: () -> Unit,
    onCalculatorClicked: () -> Unit,
    onChartClicked: () -> Unit,
    graphUIState: ChartUiState,
    getSelectedPointCallback: (SelectedItem) -> ChartModule.ChartHeaderView,
    onSelectChartInterval: (HsTimePeriod?) -> Unit,
    onToggleBalanceVisibility: () -> Unit
) {
    LazyColumn {
        item {
            CoinBalanceBlock(
                coin = uiState.token?.coin,
                balanceStr = uiState.balanceStr,
                secondaryAmount = uiState.secondaryAmount.orEmpty(),
                visible = !uiState.balanceHidden,
                onToggleBalanceVisibility = onToggleBalanceVisibility
            )
        }
        warningCard(uiState)
        item {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ButtonPrimaryYellowWithIcon(
                    title = stringResource(
                        id = if (uiState.stackingType == StackingType.PCASH) {
                            R.string.buy_pirate
                        } else {
                            R.string.buy_cosanta
                        }
                    ),
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
            TotalSection(uiState, Modifier.padding(vertical = 24.dp))
            Spacer(
                modifier = Modifier
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
private fun TotalSection(uiState: StackingCoinUIState, modifier: Modifier) {
    val waitingForStackingPlaceholder = if (uiState.isWaitingForStacking()) "-" else null
    CellUniversalLawrenceSection(
        composableItems = buildList {
            add {
                val totalIncome =
                    if (uiState.balanceHidden) "*****" else "${uiState.totalIncomeStr} ${uiState.token?.coin?.code}"
                TitleAndTwoValuesCell(
                    title = stringResource(R.string.total_income),
                    value = waitingForStackingPlaceholder ?: totalIncome,
                    value2 = if (uiState.isWaitingForStacking()) {
                        null
                    } else {
                        if (uiState.balanceHidden) "*****" else uiState.totalIncomeSecondary.orEmpty()
                    }
                )
            }
            add {
                val unpaid =
                    if (uiState.balanceHidden) "*****" else "${uiState.unpaidStr} ${uiState.token?.coin?.code}"
                TitleAndTwoValuesCell(
                    title = stringResource(R.string.unpaid),
                    value = waitingForStackingPlaceholder ?: unpaid,
                    value2 = if (uiState.isWaitingForStacking()) {
                        null
                    } else {
                        if (uiState.balanceHidden) "*****" else uiState.unpaidSecondary.orEmpty()
                    }
                )
            }
            add {
                TitleAndValueCell(
                    title = stringResource(R.string.estimated_annual_interest),
                    value = uiState.annualInterest,
                    modifier = Modifier.height(48.dp)
                )
            }
        },
        modifier = modifier
    )
}

private fun LazyListScope.warningCard(uiState: StackingCoinUIState) {
    if (!uiState.isWaitingForStacking()) return
    val notEnoughCoins = uiState.balance < uiState.minStackingAmount

    item {
        val stringDescription = if (notEnoughCoins) {
            stringResource(
                if (uiState.stackingType == StackingType.PCASH) {
                    R.string.no_active_stacking_pirate_buy_more_descritpion
                } else {
                    R.string.no_active_stacking_cosanta_buy_more_descrition
                }, (uiState.minStackingAmount - uiState.balance).toPlainString()
            )
        } else {
            val hours = if (uiState.stackingType == StackingType.PCASH) 8 else 24
            stringResource(R.string.waiting_for_stacking, hours)
        }
        TextImportantWarning(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            title = stringResource(R.string.no_active_stacking),
            text = stringDescription,
            icon = R.drawable.ic_attention_24
        )
    }
}

@Composable
private fun CoinBalanceBlock(
    coin: Coin?,
    balanceStr: String,
    secondaryAmount: String,
    visible: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val balanceWithCoinCode = "$balanceStr ${coin?.code}"
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(height = 26.dp)
        Text(
            text = stringResource(R.string.total_balance),
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.body,
            textAlign = TextAlign.Center,
        )
        VSpacer(height = 14.dp)
        HsImage(
            url = coin?.imageUrl,
            alternativeUrl = coin?.alternativeImageUrl,
            placeholder = coin?.imagePlaceholder,
            modifier = modifier
                .size(56.dp)
                .clip(CircleShape)
        )
        VSpacer(height = 12.dp)
        Text(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        onToggleBalanceVisibility()
                        HudHelper.vibrate(context)
                    }
                ),
            text = if (visible) balanceWithCoinCode else "*****",
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.title2R,
            textAlign = TextAlign.Center,
        )
        VSpacer(height = 6.dp)
        Text(
            text = if (visible) secondaryAmount else "*****",
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.body,
            maxLines = 1,
        )
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
            uiState = StackingCoinUIState(
                balance = BigDecimal(10),
                unpaidStr = "",
                loading = false,
                balanceHidden = false
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
                chartInfoData = null,
                considerAlwaysPositive = true
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
            onCalculatorClicked = {},
            onToggleBalanceVisibility = {}
        )
    }
}