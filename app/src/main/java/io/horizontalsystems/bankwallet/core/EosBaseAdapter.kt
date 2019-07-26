package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.AddressError
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.eoskit.EosKit
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

abstract class EosBaseAdapter(eos: CoinType.Eos, protected val eosKit: EosKit) : IAdapter {
    val token = eosKit.register(eos.token, eos.symbol)

    private val irreversibleThreshold = 330

    override val decimal: Int = 4

    override val feeCoinCode: String? = eos.symbol

    override val confirmationsThreshold: Int = irreversibleThreshold

    override fun start() {
        // started via EosKitManager
    }

    override fun stop() {
        // stopped via EosKitManager
    }

    override fun refresh() {
        // refreshed via EosKitManager
    }

    override val lastBlockHeight: Int?
        get() = eosKit.irreversibleBlockHeight?.let { it + confirmationsThreshold }

    override val lastBlockHeightUpdatedFlowable: Flowable<Unit>
        get() = eosKit.irreversibleBlockFlowable.map { Unit }

    override fun send(address: String, value: BigDecimal, feePriority: FeeRatePriority): Single<Unit> {
        return sendSingle(address, "")
    }

    override fun validate(address: String) {
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        var addressError: AddressError.InvalidPaymentAddress? = null
        try {
            validate(address)
        } catch (e: Exception) {
            addressError = AddressError.InvalidPaymentAddress()
        }
        return PaymentRequestAddress(address, null, error = addressError)
    }

    override val receiveAddress: String get() = eosKit.account

    override val debugInfo: String = ""

    open fun sendSingle(address: String, amount: String): Single<Unit> {
        return Single.just(Unit)
    }
}
