package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.reactivex.Single
import org.web3j.crypto.Keys
import java.math.BigDecimal

class Erc20Adapter(coin: Coin, kit: EthereumKit, override val contractAddress: String, decimal: Int)
    : EthereumBaseAdapter(coin, kit, decimal), EthereumKit.ListenerERC20 {

    init {
        ethereumKit.register(this)
    }

    override val feeCoinCode: String? = "ETH"

    override val balance get() = ethereumKit.balanceERC20(contractAddress)

    override fun start() {}
    override fun clear() {}

    override fun stop() {
        ethereumKit.unregister(contractAddress)
    }

    override fun refresh() {
        ethereumKit.refresh()
    }


    override fun send(address: String, value: BigDecimal, completion: ((Throwable?) -> Unit)?) {
        ethereumKit.sendERC20(address, contractAddress, value.toDouble(), completion)
    }

    override fun fee(value: BigDecimal, address: String?): BigDecimal {
        return ethereumKit.feeERC20().toBigDecimal()
    }

    override fun availableBalance(address: String?): BigDecimal {
        return balance
    }

    override fun validate(amount: BigDecimal, address: String?): List<SendStateError> {
        val errors = mutableListOf<SendStateError>()
        if (amount > availableBalance(address)) {
            errors.add(SendStateError.InsufficientAmount)
        }
        if (ethereumKit.balance < fee(amount, address)) {
            errors.add(SendStateError.InsufficientFeeBalance)
        }
        return errors
    }

    override fun getTransactionsObservable(hashFrom: String?, limit: Int): Single<List<TransactionRecord>> {
        return ethereumKit.transactionsERC20(contractAddress, hashFrom, limit).map {
            it.map { tx -> transactionRecord(tx) }
        }
    }

    companion object {
        fun adapter(coin: Coin, ethereumKit: EthereumKit, contractAddress: String, decimal: Int): Erc20Adapter {
            return Erc20Adapter(coin, ethereumKit, Keys.toChecksumAddress(contractAddress), decimal)
        }
    }
}
