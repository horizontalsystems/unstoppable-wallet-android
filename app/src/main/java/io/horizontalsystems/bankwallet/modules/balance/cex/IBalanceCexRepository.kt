package cash.p.terminal.modules.balance.cex

import kotlinx.coroutines.flow.StateFlow

interface IBalanceCexRepository {
    val itemsFlow: StateFlow<List<BalanceCexItem>?>

    fun start()
    fun stop()
}
