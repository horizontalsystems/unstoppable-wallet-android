package cash.p.terminal.featureStacking.ui.calculatorScreen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.featureStacking.R
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.network.domain.enity.PeriodType
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.InputField
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.ui_compose.theme.GreenL

@Composable
internal fun CalculatorScreen(
    uiState: CalculatorUIState,
    onValueChanged: (String) -> Unit,
    onDoneClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, bottom = 16.dp, top = 29.dp)) {
            Text(
                text = stringResource(R.string.pos_calculator),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "close icon",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDoneClicked
                    ),
                tint = colorResource(R.color.grey)
            )
        }
        val descriptionRes = if (uiState.coin == StackingType.PCASH) {
            R.string.pos_calculator_pirate_description
        } else {
            R.string.pos_calculator_cosanta_description
        }
        Text(
            text = stringResource(descriptionRes),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        InputField(
            value = uiState.amount,
            onValueChange = onValueChanged,
            placeholderText = stringResource(R.string.enter_coin_amount),
            modifier = Modifier.padding(vertical = 6.dp)
        )
        if (uiState.calculateResult.isNotEmpty()) {
            CalculatorTable(uiState = uiState, modifier = Modifier.padding(top = 12.dp))
        }
        Text(
            text = uiState.coinExchange,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        ButtonPrimaryYellow(
            title = stringResource(id = R.string.done),
            onClick = onDoneClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        )
    }
}

@Composable
internal fun CalculatorTable(uiState: CalculatorUIState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.border(
            width = 1.dp,
            color = ComposeAppTheme.colors.steel20,
            shape = RoundedCornerShape(12.dp)
        )
    ) {
        CalculatorItemHeader(
            period = stringResource(R.string.period),
            coin = uiState.coin.value,
            coinSecondary = uiState.coinSecondary
        )
        uiState.calculateResult.forEach { calculatorItem ->

            val period = when (calculatorItem.period) {
                PeriodType.DAY -> stringResource(R.string.day_period)
                PeriodType.WEEK -> stringResource(R.string.week_period)
                PeriodType.MONTH -> stringResource(R.string.month_period)
                PeriodType.YEAR -> stringResource(R.string.year_period)
                PeriodType.UNKNOWN -> return@forEach
            }
            Divider(
                color = ComposeAppTheme.colors.steel20,
                thickness = 1.dp
            )
            CalculatorItemRow(
                period = period,
                coin = calculatorItem.amount,
                coinSecondary = calculatorItem.amountSecondary
            )
        }
    }
}

@Composable
internal fun CalculatorItemHeader(period: String, coin: String, coinSecondary: String) {
    Row(
        modifier = Modifier
            .height(dimensionResource(R.dimen.min_calculator_row_height)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = period,
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.leah,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(
            modifier = Modifier
                .background(ComposeAppTheme.colors.steel20)
                .height(dimensionResource(R.dimen.min_calculator_row_height))
                .width(1.dp)
        )
        Text(
            text = coin,
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.leah,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(
            modifier = Modifier
                .background(ComposeAppTheme.colors.steel20)
                .height(dimensionResource(R.dimen.min_calculator_row_height))
                .width(1.dp)
        )
        Text(
            text = coinSecondary,
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.leah,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
internal fun CalculatorItemRow(
    period: String,
    coin: String,
    coinSecondary: String
) {
    Row(
        modifier = Modifier
            .height(dimensionResource(R.dimen.min_calculator_row_height)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = period,
            style = ComposeAppTheme.typography.captionSB.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = ComposeAppTheme.colors.leah,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(
            modifier = Modifier
                .background(ComposeAppTheme.colors.steel20)
                .height(dimensionResource(R.dimen.min_calculator_row_height))
                .width(1.dp)
        )
        Text(
            text = coin,
            style = ComposeAppTheme.typography.caption,
            color = GreenL,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(
            modifier = Modifier
                .background(ComposeAppTheme.colors.steel20)
                .height(dimensionResource(R.dimen.min_calculator_row_height))
                .width(1.dp)
        )
        Text(
            text = coinSecondary,
            style = ComposeAppTheme.typography.caption,
            color = ComposeAppTheme.colors.leah,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f)
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
private fun CalculatorScreenPreview() {
    ComposeAppTheme {
        CalculatorScreen(
            uiState = CalculatorUIState(
                amount = "100",
                coinExchange = "1 PCASH = 0.105821 USD",
                calculateResult = listOf(
                    CalculatorItem(
                        period = PeriodType.DAY,
                        amount = "+10000",
                        amountSecondary = "+1058.21$"
                    ),
                    CalculatorItem(
                        period = PeriodType.WEEK,
                        amount = "+123000",
                        amountSecondary = "+134058.21$"
                    ),
                    CalculatorItem(
                        period = PeriodType.MONTH,
                        amount = "+13423000",
                        amountSecondary = "+16734058.21$"
                    )
                ),
                coinSecondary = "USD"
            ),
            onDoneClicked = {},
            onValueChanged = {}
        )
    }
}