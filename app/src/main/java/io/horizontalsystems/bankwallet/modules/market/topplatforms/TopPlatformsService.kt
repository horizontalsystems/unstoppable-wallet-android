package io.horizontalsystems.bankwallet.modules.market.topplatforms

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TopPlatformsService(
    private val repository: TopPlatformsRepository,
    private val currencyManager: CurrencyManager,
) {

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    suspend fun getTopPlatforms(
        sortingField: SortingField,
        timeDuration: TimeDuration,
        forceRefresh: Boolean
    ): List<TopPlatformItem> = withContext(Dispatchers.IO) {
        repository.get(sortingField, timeDuration, forceRefresh)
    }

}
