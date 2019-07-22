package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.eoskit.EosKit
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

abstract class EosBaseAdapter(eos: CoinType.Eos, protected val eosKit: EosKit) : IAdapter {
    val token = eosKit.register(eos.token, eos.symbol)

    override val decimal: Int = 4

    override val feeCoinCode: String? = eos.symbol

    override val confirmationsThreshold: Int = 0

    override fun start() {
        // started via EosKitManager
    }

    override fun stop() {
        // stopped via EosKitManager
    }

    override fun refresh() {
        // refreshed via EosKitManager
    }

    override val lastBlockHeight: Int? get() = eosKit.irreversibleBlockHeight
    override val lastBlockHeightUpdatedFlowable: Flowable<Unit>
        get() = eosKit.irreversibleBlockFlowable.map { Unit }

    override fun send(address: String, value: BigDecimal, feePriority: FeeRatePriority): Single<Unit> {
        return sendSingle(address, "")
    }

    override fun validate(address: String) {
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return PaymentRequestAddress(address)
    }

    override val receiveAddress: String get() = eosKit.account

    override val debugInfo: String = ""

    open fun sendSingle(address: String, amount: String): Single<Unit> {
        return Single.just(Unit)
    }
}
