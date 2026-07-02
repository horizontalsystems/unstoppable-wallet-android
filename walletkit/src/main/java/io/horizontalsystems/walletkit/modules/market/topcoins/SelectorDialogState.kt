package io.horizontalsystems.walletkit.modules.market.topcoins

import io.horizontalsystems.walletkit.modules.market.SortingField
import io.horizontalsystems.walletkit.ui.compose.Select

sealed class SelectorDialogState {
    object Closed : SelectorDialogState()
    class Opened(val select: Select<SortingField>) : SelectorDialogState()
}
