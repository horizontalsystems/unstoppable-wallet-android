package cash.p.terminal.modules.balance.cex

import cash.p.terminal.core.managers.CexAssetManager
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.ICexProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class BalanceCexRepositoryWrapper(private val cexAssetManager: CexAssetManager) {
    val itemsFlow = MutableStateFlow<List<CexAsset>?>(null)

    private var cexProvider: ICexProvider? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var collectCexRepoItemsJob: Job? = null

    fun start() = Unit

    fun stop() {
        coroutineScope.cancel()
    }

    fun refresh() {
        collectCexRepoItemsJob?.cancel()
        val provider = cexProvider ?: return

        collectCexRepoItemsJob = coroutineScope.launch {
            try {
                cexAssetManager.saveAllForAccount(provider.getAssets(), provider.account)
                itemsFlow.update {
                    cexAssetManager.getAllForAccount(provider.account)
                        .filter { it.freeBalance > BigDecimal.ZERO || it.lockedBalance > BigDecimal.ZERO }
                }
            } catch (t: Throwable) {

            }
        }
    }

    fun setCexProvider(cexProvider: ICexProvider?) {
        this.cexProvider = cexProvider

        itemsFlow.update {
            cexProvider?.let {
                cexAssetManager.getAllForAccount(it.account)
                    .filter { it.freeBalance > BigDecimal.ZERO || it.lockedBalance > BigDecimal.ZERO }
            }
        }

        refresh()
    }
}