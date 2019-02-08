package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.reactivex.Single
import java.math.BigDecimal

class EthereumAdapter(coin: Coin, kit: EthereumKit) : EthereumBaseAdapter(coin, kit, 18) {

    override val balance: BigDecimal get() = ethereumKit.balance

    override fun start() {
        ethereumKit.listener = this
        ethereumKit.start()
    }

    override fun stop() {}
    override fun clear() {}

    override fun refresh() {
        ethereumKit.refresh()
    }

    override fun send(address: String, value: BigDecimal, completion: ((Throwable?) -> (Unit))?) {
        ethereumKit.send(address, value, completion)
    }

    override fun fee(value: BigDecimal, address: String?, senderPay: Boolean): BigDecimal {
        val fee = ethereumKit.fee()
        if (senderPay && balance.minus(value).minus(fee) < BigDecimal.ZERO) {
            throw Error.InsufficientAmount(fee)
        }
        return fee
    }

    override fun getTransactionsObservable(hashFrom: String?, limit: Int): Single<List<TransactionRecord>> {
        return ethereumKit.transactions(hashFrom, limit).map {
            it.map { tx -> transactionRecord(tx) }
        }
    }

    companion object {
        fun adapter(coin: Coin, ethereumKit: EthereumKit) = EthereumAdapter(coin, ethereumKit)
    }
}
