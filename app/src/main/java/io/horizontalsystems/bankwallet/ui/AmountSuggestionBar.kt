package io.horizontalsystems.bankwallet.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import io.horizontalsystems.bankwallet.modules.multiswap.SuggestionsBar
import io.horizontalsystems.bankwallet.ui.compose.Keyboard
import io.horizontalsystems.bankwallet.ui.compose.observeKeyboardState
import java.math.BigDecimal

@Composable
fun BoxScope.AmountSuggestionBar(
    availableBalance: BigDecimal?,
    amount: BigDecimal?,
    onEnterAmountPercentage: (Int) -> Unit,
    onDelete: () -> Unit,
    inputHasFocus: Boolean,
) {
    val focusManager = LocalFocusManager.current
    val keyboardState by observeKeyboardState()
    if (inputHasFocus && keyboardState == Keyboard.Opened) {
        val hasNonZeroBalance = availableBalance != null && availableBalance > BigDecimal.ZERO

        SuggestionsBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onDelete = onDelete,
            onSelect = {
                focusManager.clearFocus()
                onEnterAmountPercentage.invoke(it)
            },
            selectEnabled = hasNonZeroBalance,
            deleteEnabled = amount != null,
        )
    }
}
