package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

abstract class EthereumBaseAdapter(
        protected val ethereumKit: EthereumKit,
        val decimal: Int)
    : IAdapter, ISendEthereumAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter {

    override fun getReceiveAddressType(wallet: Wallet): String? = null

    override val debugInfo: String = ethereumKit.debugInfo()

    // ITransactionsAdapter

    override val confirmationsThreshold: Int = 12

    override val lastBlockInfo: LastBlockInfo?
        get() = ethereumKit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = ethereumKit.lastBlockHeightFlowable.map { Unit }

    // ISendEthereumAdapter

    override fun send(amount: BigDecimal, address: String, gasPrice: Long, gasLimit: Long): Single<Unit> {
        val poweredDecimal = amount.scaleByPowerOfTen(decimal)
        val noScaleDecimal = poweredDecimal.setScale(0, RoundingMode.HALF_DOWN)

        return sendSingle(address, noScaleDecimal.toPlainString(), gasPrice, gasLimit)
    }

    override fun validate(address: String) {
        ethereumKit.validateAddress(address)
    }

    // IReceiveAdapter

    override val receiveAddress: String get() = ethereumKit.receiveAddress

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            val converted = it.movePointLeft(decimal)
            return converted.stripTrailingZeros()
        } ?: return BigDecimal.ZERO
    }

    open fun sendSingle(address: String, amount: String, gasPrice: Long, gasLimit: Long): Single<Unit> {
        return Single.just(Unit)
    }

    override fun estimateGasLimit(toAddress: String, value: BigDecimal, gasPrice: Long?): Single<Long> {
        return Single.just(ethereumKit.gasLimit)
    }

}
