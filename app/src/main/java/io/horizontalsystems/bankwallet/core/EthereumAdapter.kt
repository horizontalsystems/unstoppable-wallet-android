package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.reactivex.Single
import java.math.BigDecimal

class EthereumAdapter(
        coin: Coin,
        kit: EthereumKit,
        private val feeRateProvider: IFeeRateProvider) : EthereumBaseAdapter(coin, kit, 18) {

    init {
        ethereumKit.listener = this
    }

    override val balanceString: String?
        get() = ethereumKit.balance

    override val balance: BigDecimal
        get() = balanceInBigDecimal(balanceString, decimal)

    override fun start() {
        ethereumKit.start()
    }

    override fun stop() {}
    override fun clear() {}

    override fun refresh() {
        ethereumKit.start()
    }

    override fun sendSingle(address: String, amount: String, feePriority: FeeRatePriority): Single<Unit> {
        return ethereumKit.send(address, amount, feeRateProvider.ethereumGasPrice(feePriority)).map { Unit }
    }

    override fun fee(value: BigDecimal, address: String?, feePriority: FeeRatePriority): BigDecimal {
        return ethereumKit.fee(feeRateProvider.ethereumGasPrice(feePriority))
    }

    override fun availableBalance(address: String?, feePriority: FeeRatePriority): BigDecimal {
        return BigDecimal.ZERO.max(balance - fee(balance, address, feePriority))
    }

    override fun validate(amount: BigDecimal, address: String?, feePriority: FeeRatePriority): List<SendStateError> {
        val errors = mutableListOf<SendStateError>()
        if (amount > availableBalance(address, feePriority)) {
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

}
