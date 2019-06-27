package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

abstract class EthereumBaseAdapter(override val wallet: Wallet, protected val ethereumKit: EthereumKit, final override val decimal: Int, private val addressParser: AddressParser, protected val feeRateProvider: IFeeRateProvider) : IAdapter {

    override val feeCoinCode: String? = "ETH"

    override val confirmationsThreshold: Int = 12

    override fun start() {
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override fun refresh() {
        // refreshed via EthereumKitManager
    }

    override val lastBlockHeight: Int? get() = ethereumKit.lastBlockHeight?.toInt()

    override val lastBlockHeightUpdatedFlowable: Flowable<Unit>
        get() = ethereumKit.lastBlockHeightFlowable.map { Unit }

    override fun send(address: String, value: BigDecimal, feePriority: FeeRatePriority): Single<Unit> {
        val poweredDecimal = value.scaleByPowerOfTen(decimal)
        val noScaleDecimal = poweredDecimal.setScale(0, RoundingMode.HALF_DOWN)

        return sendSingle(address, noScaleDecimal.toPlainString(), feeRateProvider.ethereumGasPrice(feePriority))
    }

    override fun validate(address: String) {
        ethereumKit.validateAddress(address)
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        val paymentData = addressParser.parse(address)
        return PaymentRequestAddress(paymentData.address, paymentData.amount?.toBigDecimal())
    }

    override val receiveAddress: String get() = ethereumKit.receiveAddress

    override val debugInfo: String = ethereumKit.debugInfo()

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            val converted = it.movePointLeft(decimal)
            return converted.stripTrailingZeros()
        } ?: return BigDecimal.ZERO
    }

    open fun sendSingle(address: String, amount: String, gasPrice: Long): Single<Unit> {
        return Single.just(Unit)
    }

}
