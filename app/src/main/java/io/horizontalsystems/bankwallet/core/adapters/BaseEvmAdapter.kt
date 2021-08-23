package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger

abstract class BaseEvmAdapter(
        final override val evmKit: EthereumKit,
        val decimal: Int,
        val coinManager: ICoinManager
) : IAdapter, ISendEthereumAdapter, IBalanceAdapter, IReceiveAdapter {

    override val debugInfo: String
        get() = evmKit.debugInfo()

    // ITransactionsAdapter

    val lastBlockInfo: LastBlockInfo?
        get() = evmKit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = evmKit.lastBlockHeightFlowable.map { Unit }

    // ISendEthereumAdapter

    override fun fee(gasPrice: Long, gasLimit: Long): BigDecimal {
        val value = BigDecimal(gasPrice) * BigDecimal(gasLimit)
        return convertToEther(value)
    }

    override fun send(amount: BigDecimal, address: String, gasPrice: Long, gasLimit: Long, logger: AppLogger): Single<Unit> {
        return try {
            sendInternal(Address(address), scaleUp(amount), gasPrice, gasLimit, logger)
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    override fun estimateGasLimit(toAddress: String?, value: BigDecimal, gasPrice: Long?): Single<Long> {
        return try {
            estimateGasLimitInternal(toAddress?.let { Address(it) }, scaleUp(value), gasPrice)
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    @Throws
    override fun validate(address: String) {
        AddressValidator.validate(address)
    }

    protected fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    protected fun scaleUp(amount: BigDecimal, decimals: Int = decimal): BigInteger {
        return amount.movePointRight(decimals).toBigInteger()
    }

    protected fun convertToWei(amount: BigDecimal): BigInteger {
        return scaleUp(amount, EvmAdapter.decimal)
    }

    private fun convertToEther(amount: BigDecimal): BigDecimal {
        return scaleDown(amount, EvmAdapter.decimal)
    }
    // IReceiveAdapter

    override val receiveAddress: String
        get() = evmKit.receiveAddress.eip55

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    protected abstract fun sendInternal(address: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long, logger: AppLogger): Single<Unit>

    protected abstract fun estimateGasLimitInternal(toAddress: Address?, value: BigInteger, gasPrice: Long?): Single<Long>

    companion object {
        const val confirmationsThreshold: Int = 12
    }

}
