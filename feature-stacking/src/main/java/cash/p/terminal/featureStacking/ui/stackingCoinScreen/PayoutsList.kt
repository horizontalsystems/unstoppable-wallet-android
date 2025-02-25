package cash.p.terminal.featureStacking.ui.stackingCoinScreen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.featureStacking.R
import cash.p.terminal.network.pirate.domain.enity.PayoutType
import cash.p.terminal.featureStacking.ui.entities.PayoutViewItem
import cash.p.terminal.ui_compose.components.HeaderStick
import cash.p.terminal.ui_compose.entities.SectionItemPosition
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

internal fun LazyListScope.payoutList(
    payoutItemsMap: Map<String, List<PayoutViewItem>>
) {
    if(payoutItemsMap.isEmpty()) {
        return
    }
    item {
        Text(
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            text = stringResource(id = R.string.payouts_history),
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp)
        )
    }
    payoutItemsMap.forEach { (dateHeader, transactions) ->
        item {
            HeaderStick(text = dateHeader)
        }

        val itemsCount = transactions.size
        val singleElement = itemsCount == 1

        itemsIndexed(
            items = transactions,
            key = { _, item ->
                item.id
            }
        ) { index, item ->
            val position: SectionItemPosition = when {
                singleElement -> SectionItemPosition.Single
                index == 0 -> SectionItemPosition.First
                index == itemsCount - 1 -> SectionItemPosition.Last
                else -> SectionItemPosition.Middle
            }

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PayoutCell(item, position)
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    item {
        Spacer(modifier = Modifier.height(20.dp))
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
private fun PayoutListPreview() {
    ComposeAppTheme {
        LazyColumn {
            payoutList(
                payoutItemsMap = mapOf(
                    "DECEMBER 15, 2023" to listOf(
                        PayoutViewItem(
                            id = 1,
                            date = "DECEMBER 15, 2023",
                            time = "10:00",
                            payoutType = PayoutType.INCOME,
                            amount = 123.45.toBigDecimal(),
                            amountSecondary = "$123.4"
                        ),
                        PayoutViewItem(
                            id = 2,
                            date = "DECEMBER 15, 2023",
                            time = "09:55",
                            payoutType = PayoutType.INCOME,
                            amount = 123.45.toBigDecimal(),
                            amountSecondary = "$123.4"
                        )
                    ),
                    "DECEMBER 10, 2023" to listOf(
                        PayoutViewItem(
                            id = 3,
                            date = "DECEMBER 10, 2023",
                            time = "09:55",
                            payoutType = PayoutType.INCOME,
                            amount = 123.45.toBigDecimal(),
                            amountSecondary = "$123.4"
                        ),
                        PayoutViewItem(
                            id = 4,
                            date = "DECEMBER 10, 2023",
                            time = "06:55",
                            payoutType = PayoutType.WITHDRAWAL,
                            amount = 123.45.toBigDecimal(),
                            amountSecondary = "$123.4"
                        )
                    )
                )
            )
        }
    }
}
