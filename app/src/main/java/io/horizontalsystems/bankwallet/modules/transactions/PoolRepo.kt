package io.horizontalsystems.bankwallet.modules.transactions

class PoolRepo {

    val activePools: List<Pool>
        get() = activePoolCoinCodes.mapNotNull { pools[it] }

    val allPools: List<Pool>
        get() = pools.values.toList()

    private var pools = mutableMapOf<CoinCode, Pool>()
    private var activePoolCoinCodes = listOf<CoinCode>()

    fun activatePools(coinCodes: List<CoinCode>) {
        coinCodes.forEach { coinCode ->
            if (!pools.containsKey(coinCode)) {
                pools[coinCode] = Pool(Pool.State(coinCode))
            }
        }

        this.activePoolCoinCodes = coinCodes
    }

    fun getPool(coinCode: CoinCode): Pool? {
        return pools[coinCode]
    }

    fun isPoolActiveByCoinCode(coinCode: CoinCode): Boolean {
        return activePoolCoinCodes.contains(coinCode)
    }

}
