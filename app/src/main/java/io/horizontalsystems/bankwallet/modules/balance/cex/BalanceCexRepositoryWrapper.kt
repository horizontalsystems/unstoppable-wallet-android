package io.horizontalsystems.bankwallet.modules.balance.cex

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CexType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class BalanceCexRepositoryWrapper(
    private val accountManager: IAccountManager,
) : IBalanceCexRepository {
    override val itemsFlow = MutableStateFlow<List<BalanceCexItem>?>(null)

    private var concreteRepository: IBalanceCexRepository? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var collectCexRepoItemsJob: Job? = null

    override fun start() {
        coroutineScope.launch {
            accountManager.activeAccountStateFlow.collect {
                handleActiveAccount(it)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    private fun handleActiveAccount(activeAccount: ActiveAccountState) {
        collectCexRepoItemsJob?.cancel()
        concreteRepository?.stop()

        val accountType = (activeAccount as? ActiveAccountState.ActiveAccount)
            ?.account
            ?.type

        val cexType = (accountType as? AccountType.Cex)?.cexType
        concreteRepository = when (cexType) {
            is CexType.Coinzix -> CoinzixBalanceCexRepository()
            is CexType.Binance -> BinanceBalanceCexRepository()
            null -> null
        }

        concreteRepository?.let { repo ->
            collectCexRepoItemsJob = coroutineScope.launch {
                repo.itemsFlow.collect { repoItems ->
                    itemsFlow.update {
                        repoItems
                    }
                }
            }

            repo.start()
        } ?: run {
            itemsFlow.update { null }
        }
    }
}