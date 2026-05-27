package io.horizontalsystems.bankwallet.modules.market.platform

import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.ui.compose.Select

object MarketPlatformModule {

    data class Menu(
        val sortingFieldSelect: Select<SortingField>,
        val marketFieldSelect: Select<MarketField>
    )

}
