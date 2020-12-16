package io.horizontalsystems.bankwallet.modules.market.top

import androidx.lifecycle.ViewModel

class MarketTopViewModel : ViewModel() {

    val sortingFields: List<String> = listOf("Highest Cap", "Lowest Cap", "Highest Volume", "Lowest Volume")
    var sortingField: CharSequence = "Highest Cap"
    val sortingPeriod: CharSequence = "24h"

}
