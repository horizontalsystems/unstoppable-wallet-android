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
) {
    val itemsFlow = MutableStateFlow<List<BalanceCexItem>?>(null)

    private var concreteRepository: IBalanceCexRepository? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var collectCexRepoItemsJob: Job? = null

    fun start() {
        coroutineScope.launch {
            accountManager.activeAccountStateFlow.collect {
                handleActiveAccount(it)
            }
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun refresh() {
        concreteRepository?.let {
            getItems(it)
        }
    }

    private fun handleActiveAccount(activeAccount: ActiveAccountState) {
        collectCexRepoItemsJob?.cancel()

        val accountType = (activeAccount as? ActiveAccountState.ActiveAccount)
            ?.account
            ?.type

        val cexType = (accountType as? AccountType.Cex)?.cexType
        concreteRepository = when (cexType) {
            is CexType.Coinzix -> CoinzixBalanceCexRepository(cexType.authToken, cexType.secret)
            is CexType.Binance -> BinanceBalanceCexRepository(cexType.apiKey, cexType.secretKey)
            null -> null
        }

        concreteRepository?.let { repo ->
            getItems(repo)
        } ?: run {
            itemsFlow.update { null }
        }
    }

    private fun getItems(repo: IBalanceCexRepository) {
        collectCexRepoItemsJob = coroutineScope.launch {
            try {
                itemsFlow.update {
                    repo.getItems()
                }
            } catch (t: Throwable) {

            }
        }
    }
}