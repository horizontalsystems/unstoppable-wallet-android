package cash.p.terminal.modules.balance.cex

import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.providers.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class BalanceCexRepositoryWrapper(private val marketKit: MarketKitWrapper) {
    val itemsFlow = MutableStateFlow<List<CexAsset>?>(null)

    private var cexProvider: ICexProvider? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var collectCexRepoItemsJob: Job? = null
    private val coins = marketKit.allCoins().map { it.uid to it }.toMap()

    fun start() = Unit

    fun stop() {
        coroutineScope.cancel()
    }

    fun refresh() {
        collectCexRepoItemsJob?.cancel()
        collectCexRepoItemsJob = coroutineScope.launch {
            try {
                val cexAssetRaws = cexProvider?.getAssets()
                val cexAssets = cexAssetRaws?.map {
                    buildCexAsset(it)
                }

                itemsFlow.update {
                    cexAssets
                }
            } catch (t: Throwable) {

            }
        }
    }

    private fun buildCexAsset(cexAssetRaw: CexAssetRaw): CexAsset {
        return CexAsset(
            id = cexAssetRaw.id,
            name = cexAssetRaw.name,
            freeBalance = cexAssetRaw.freeBalance,
            lockedBalance = cexAssetRaw.lockedBalance,
            depositEnabled = cexAssetRaw.depositEnabled,
            withdrawEnabled = cexAssetRaw.withdrawEnabled,
            networks = cexAssetRaw.networks.map { buildCexNetwork(it) },
            coin = coins[cexAssetRaw.coinUid],
            decimals = cexAssetRaw.decimals
        )
    }

    private fun buildCexNetwork(cexNetworkRaw: CexNetworkRaw): CexNetwork {
        return CexNetwork(
            network = cexNetworkRaw.network,
            name = cexNetworkRaw.name,
            isDefault = cexNetworkRaw.isDefault,
            depositEnabled = cexNetworkRaw.depositEnabled,
            withdrawEnabled = cexNetworkRaw.withdrawEnabled,
            blockchain = null,
        )
    }

    fun setCexProvider(cexProvider: ICexProvider?) {
        this.cexProvider = cexProvider

        refresh()
    }
}