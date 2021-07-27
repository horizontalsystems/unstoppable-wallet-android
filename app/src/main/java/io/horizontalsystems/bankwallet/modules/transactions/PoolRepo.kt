package io.horizontalsystems.bankwallet.modules.transactions


class PoolRepo {

    val activePools: List<Pool>
        get() = activePoolWallets.mapNotNull { pools[it] }

    val allPools: List<Pool>
        get() = pools.values.toList()

    private var pools = mutableMapOf<TransactionWallet, Pool>()
    private var activePoolWallets = listOf<TransactionWallet>()

    fun activatePools(wallets: List<TransactionWallet>) {
        wallets.forEach { wallet ->
            if (!pools.containsKey(wallet)) {
                pools[wallet] = Pool(Pool.State(wallet))
            }
        }

        this.activePoolWallets = wallets
    }

    fun deactivateAllPools() {
        pools.clear()
    }

    fun getPool(wallet: TransactionWallet): Pool? {
        return pools[wallet]
    }

    fun isPoolActiveByWallet(wallet: TransactionWallet): Boolean {
        return activePoolWallets.contains(wallet)
    }

}
