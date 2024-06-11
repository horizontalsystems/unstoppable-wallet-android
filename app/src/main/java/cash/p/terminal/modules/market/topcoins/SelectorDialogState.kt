package cash.p.terminal.modules.market.topcoins

import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.ui.compose.Select

sealed class SelectorDialogState {
    object Closed : SelectorDialogState()
    class Opened(val select: Select<SortingField>) : SelectorDialogState()
}
