package io.horizontalsystems.bankwallet.modules.balance.cex

import kotlinx.coroutines.flow.StateFlow

interface IBalanceCexRepository {
    val itemsFlow: StateFlow<List<BalanceCexItem>>

    fun start()
}
