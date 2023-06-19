package cash.p.terminal.modules.balance.cex

import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.managers.ActiveAccountState
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.CexType
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
            collectCexRepoItemsJob = coroutineScope.launch {
                try {
                    itemsFlow.update {
                        repo.getItems()
                    }
                } catch (t: Throwable) {

                }
            }
        } ?: run {
            itemsFlow.update { null }
        }
    }
}