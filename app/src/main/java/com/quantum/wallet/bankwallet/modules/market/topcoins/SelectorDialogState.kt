package com.quantum.wallet.bankwallet.modules.market.topcoins

import com.quantum.wallet.bankwallet.modules.market.SortingField
import com.quantum.wallet.bankwallet.ui.compose.Select

sealed class SelectorDialogState {
    object Closed : SelectorDialogState()
    class Opened(val select: Select<SortingField>) : SelectorDialogState()
}
