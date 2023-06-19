package io.horizontalsystems.bankwallet.modules.balance.cex

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CexType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class BalanceCexRepositoryWrapper {
    val itemsFlow = MutableStateFlow<List<BalanceCexItem>?>(null)

    private var concreteRepository: IBalanceCexRepository? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var collectCexRepoItemsJob: Job? = null

    fun start() = Unit

    fun stop() {
        coroutineScope.cancel()
    }

    fun refresh() {
        collectCexRepoItemsJob?.cancel()
        collectCexRepoItemsJob = coroutineScope.launch {
            try {
                itemsFlow.update {
                    concreteRepository?.getItems()
                }
            } catch (t: Throwable) {

            }
        }
    }

    fun setActiveAccount(account: Account?) {
        val cexType = (account?.type as? AccountType.Cex)?.cexType
        concreteRepository = when (cexType) {
            is CexType.Coinzix -> CoinzixBalanceCexRepository(cexType.authToken, cexType.secret)
            is CexType.Binance -> BinanceBalanceCexRepository(cexType.apiKey, cexType.secretKey)
            null -> null
        }

        refresh()
    }
}