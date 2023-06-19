package cash.p.terminal.modules.balance.cex

interface IBalanceCexRepository {
    suspend fun getItems(): List<BalanceCexItem>
}
