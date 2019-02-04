package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.reactivex.Single
import java.math.BigDecimal

class Erc20Adapter(kit: EthereumKit, override val contractAddress: String, decimal: Int)
    : EthereumBaseAdapter(kit, decimal), EthereumKit.ListenerERC20 {

    init {
        ethereumKit.register(this)
    }

    override val balance get() = ethereumKit.balanceERC20(contractAddress).toBigDecimal()

    override fun start() {}
    override fun clear() {}

    override fun stop() {
        ethereumKit.unregister(contractAddress)
    }

    override fun refresh() {
        ethereumKit.refresh(contractAddress)
    }


    override fun send(address: String, value: BigDecimal, completion: ((Throwable?) -> Unit)?) {
        ethereumKit.sendERC20(address, contractAddress, value.toDouble(), completion)
    }

    override fun fee(value: BigDecimal, address: String?, senderPay: Boolean): BigDecimal {
        val fee = ethereumKit.feeERC20().toBigDecimal()
        if (senderPay && balance.minus(value).minus(fee) < BigDecimal.ZERO) {
            throw Error.InsufficientAmount(fee)
        }

        return fee
    }

    override fun getTransactionsObservable(hashFrom: String?, limit: Int): Single<List<TransactionRecord>> {
        return ethereumKit.transactionsERC20(contractAddress, hashFrom, limit).map {
            it.map { tx -> transactionRecord(tx) }
        }
    }

    companion object {
        fun adapter(ethereumKit: EthereumKit, contractAddress: String, decimal: Int): Erc20Adapter {
            return Erc20Adapter(ethereumKit, contractAddress, decimal)
        }
    }
}
