package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.ViewModel

class MarketCategoriesViewModel(private val service: MarketCategoriesService) : ViewModel() {

    val categories by service::categories

}
