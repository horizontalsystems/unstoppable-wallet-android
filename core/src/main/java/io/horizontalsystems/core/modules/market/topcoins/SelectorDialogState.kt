package io.horizontalsystems.core.modules.market.topcoins

import io.horizontalsystems.core.modules.market.SortingField
import io.horizontalsystems.core.ui.compose.Select

sealed class SelectorDialogState {
    object Closed : SelectorDialogState()
    class Opened(val select: Select<SortingField>) : SelectorDialogState()
}
