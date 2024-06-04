package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.modules.settings.appearance.PriceChangeInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PriceManager(
    private val storage: ILocalStorage
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    var priceChangeInterval: PriceChangeInterval = storage.priceChangeInterval
        private set

    val priceChangeIntervalFlow: StateFlow<PriceChangeInterval>
        get() = storage.priceChangeIntervalFlow

    init {
        coroutineScope.launch {
            storage.priceChangeIntervalFlow.collect {
                priceChangeInterval = it
            }
        }
    }

}
