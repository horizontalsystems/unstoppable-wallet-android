package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable

class MarketSearchViewModel(private val service: MarketSearchService, private val clearables: List<Clearable>) : ViewModel() {

    var query: String by service::query

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

}
