package cash.p.terminal.modules.send

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cash.p.terminal.modules.address.AmountUnique
import cash.p.terminal.ui.compose.Keyboard
import cash.p.terminal.ui.compose.components.SuggestionsBar
import cash.p.terminal.ui.compose.observeKeyboardState
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun BoxScope.SendSuggestionsBar(
    availableBalance: BigDecimal,
    coinDecimal: Int,
    coinAmount: BigDecimal?,
    onAmountChange: (BigDecimal?) -> Unit,
    onPercentageAmountUnique: (AmountUnique?) -> Unit,
) {
    val keyboardState by observeKeyboardState()

    if (keyboardState == Keyboard.Opened) {
        SuggestionsBar(
            modifier = Modifier
                .imePadding()
                .align(Alignment.BottomCenter),
            onSelect = { percent ->
                val amount = availableBalance
                    .times(BigDecimal(percent / 100.0))
                    .setScale(coinDecimal, RoundingMode.DOWN)
                    .stripTrailingZeros()
                onPercentageAmountUnique(AmountUnique(amount))
                onAmountChange(amount)
            },
            onDelete = {
                onPercentageAmountUnique(AmountUnique(BigDecimal.ZERO))
                onAmountChange(null)
            },
            selectEnabled = availableBalance > BigDecimal.ZERO,
            deleteEnabled = coinAmount != null,
        )
    }
}
