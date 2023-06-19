package io.horizontalsystems.bankwallet.modules.balance.cex

interface IBalanceCexRepository {
    suspend fun getItems(): List<BalanceCexItem>
}
