package cash.p.terminal.modules.market.topplatforms

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
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
