package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Coin

class PoolRepo {

    val activePools: List<Pool>
        get() = activePoolCoinCodes.mapNotNull { pools[it] }

    val allPools: List<Pool>
        get() = pools.values.toList()

    private var pools = mutableMapOf<Coin, Pool>()
    private var activePoolCoinCodes = listOf<Coin>()

    fun activatePools(coinCodes: List<Coin>) {

        // remove pools for unused coins
        pools.map { it.key }.forEach { poolCoin ->
            if (!coinCodes.contains(poolCoin)) {
                pools.remove(poolCoin)
            }
        }

        coinCodes.forEach { coinCode ->
            if (!pools.containsKey(coinCode)) {
                pools[coinCode] = Pool(Pool.State(coinCode))
            }
        }

        this.activePoolCoinCodes = coinCodes
    }

    fun getPool(coin: Coin): Pool? {
        return pools[coin]
    }

    fun isPoolActiveByCoinCode(coin: Coin): Boolean {
        return activePoolCoinCodes.contains(coin)
    }

}
