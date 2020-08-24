package io.horizontalsystems.bankwallet.modules.swap.repository

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.swap.DataState
import io.horizontalsystems.erc20kit.core.AllowanceManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.math.BigDecimal

class AllowanceRepository(
        private val ethereumKit: EthereumKit,
        private val address: Address,
        private val spenderAddress: Address
) {
    private val allowanceManagerMap = HashMap<Coin, AllowanceManager>()

    fun allowance(coin: Coin): Flowable<DataState<BigDecimal>> {
        return Flowable.create({ emitter ->
            try {
                emitter.onNext(DataState.Loading)

                val allowanceManager = getAllowanceManager(coin)

                val allowance = allowanceManager.allowance(spenderAddress).blockingGet()
                val allowanceDecimal = allowance.toBigDecimal().movePointLeft(coin.decimal).stripTrailingZeros()

                emitter.onNext(DataState.Success(allowanceDecimal))
            } catch (error: Throwable) {
                emitter.onNext(DataState.Error(error))
            }
            emitter.onComplete()
        }, BackpressureStrategy.BUFFER)
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
