package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.reactivex.Single
import java.math.BigDecimal

class EthereumAdapter(coin: Coin, kit: EthereumKit) : EthereumBaseAdapter(coin, kit, 18) {

    init {
        ethereumKit.listener = this
    }

    override val balanceString: String?
        get() = ethereumKit.balance

    override fun start() {
        ethereumKit.start()
    }

    override fun stop() {}
    override fun clear() {}

    override fun refresh() {
        ethereumKit.start()
    }

    override fun sendSingle(address: String, amount: String): Single<Unit> {
        return ethereumKit.send(address, amount).map { Unit }
    }

    override fun fee(value: BigDecimal, address: String?): BigDecimal {
        return ethereumKit.fee()
    }

    override fun availableBalance(address: String?): BigDecimal {
        return BigDecimal.ZERO.max(balance - fee(balance, address))
    }

    override fun validate(amount: BigDecimal, address: String?): List<SendStateError> {
        val errors = mutableListOf<SendStateError>()
        if (amount > availableBalance(address)) {
            errors.add(SendStateError.InsufficientAmount)
        }
        return errors
    }

    override fun getTransactionsObservable(hashFrom: String?, limit: Int): Single<List<TransactionRecord>> {
        return ethereumKit.transactions(hashFrom, limit).map {
            it.map { tx -> transactionRecord(tx) }
        }
    }

    override fun onSyncStateUpdate() {
        val newState = convertState(ethereumKit.syncState)
        if (state != newState) {
            state = newState
        }
    }

    companion object {
        fun adapter(coin: Coin, ethereumKit: EthereumKit) = EthereumAdapter(coin, ethereumKit)
    }
}
