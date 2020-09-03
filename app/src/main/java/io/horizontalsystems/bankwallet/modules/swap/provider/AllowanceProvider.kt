package io.horizontalsystems.bankwallet.modules.swap.provider

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.erc20kit.core.AllowanceManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Single
import java.math.BigDecimal

class AllowanceProvider(
        private val ethereumKit: EthereumKit,
        private val address: Address
) {
    private val allowanceManagerMap = HashMap<Coin, AllowanceManager>()

    fun getAllowance(coin: Coin, spenderAddress: Address): Single<BigDecimal> =
            Single.create { emitter ->
                try {
                    val allowanceManager = getAllowanceManager(coin)
                    val allowance = allowanceManager.allowance(spenderAddress).blockingGet()
                    val allowanceDecimal = allowance.toBigDecimal().movePointLeft(coin.decimal).stripTrailingZeros()

                    emitter.onSuccess(allowanceDecimal)
                } catch (error: Throwable) {
                    emitter.onError(error)
                }
            }

    @Synchronized
    private fun getAllowanceManager(coin: Coin): AllowanceManager =
            if (allowanceManagerMap.containsKey(coin)) {
                allowanceManagerMap.getValue(coin)
            } else {
                val contractAddressString = (coin.type as CoinType.Erc20).address
                val contractAddress = Address(contractAddressString)
                AllowanceManager(ethereumKit, contractAddress, address).also {
                    allowanceManagerMap[coin] = it
                }
            }
}
