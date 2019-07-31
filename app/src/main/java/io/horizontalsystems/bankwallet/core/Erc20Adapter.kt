package io.horizontalsystems.bankwallet.core

import android.content.Context
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.erc20kit.core.Erc20Kit.SyncState
import io.horizontalsystems.erc20kit.core.TransactionKey
import io.horizontalsystems.erc20kit.models.TransactionInfo
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

class Erc20Adapter(context: Context, wallet: Wallet, kit: EthereumKit, decimal: Int, private val fee: BigDecimal, contractAddress: String, addressParser: AddressParser, feeRateProvider: IFeeRateProvider)
    : EthereumBaseAdapter(wallet, kit, decimal, addressParser, feeRateProvider) {

    private val erc20Kit: Erc20Kit = Erc20Kit.getInstance(context, ethereumKit, contractAddress)

    override val state: AdapterState
        get() = when (erc20Kit.syncState) {
            is SyncState.Synced -> AdapterState.Synced
            is SyncState.NotSynced -> AdapterState.NotSynced
            is SyncState.Syncing -> AdapterState.Syncing(50, null)
        }

    override val stateUpdatedFlowable: Flowable<Unit>
        get() = erc20Kit.syncStateFlowable.map { Unit }

    override val balance: BigDecimal
        get() = balanceInBigDecimal(erc20Kit.balance, decimal)

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = erc20Kit.balanceFlowable.map { Unit }

    override fun getTransactions(from: Pair<String, Int>?, limit: Int): Single<List<TransactionRecord>> {
        return erc20Kit.transactions(from?.let { TransactionKey(it.first.hexStringToByteArray(), it.second) }, limit).map {
            it.map { tx -> transactionRecord(tx) }
        }
    }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = erc20Kit.transactionsFlowable.map { it.map { tx -> transactionRecord(tx) } }

    override fun sendSingle(address: String, amount: String, gasPrice: Long): Single<Unit> {
        return erc20Kit.send(address, amount, gasPrice).map { Unit }
    }

    override fun availableBalance(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        return balance - fee
    }

    override fun fee(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        val feePriority = params[SendModule.AdapterFields.FeeRatePriority] as? FeeRatePriority ?: FeeRatePriority.MEDIUM
        return erc20Kit.fee(feeRateProvider.ethereumGasPrice(feePriority)).movePointLeft(18)
    }

    override fun validate(params: Map<SendModule.AdapterFields, Any?>): List<SendStateError> {
        val errors = mutableListOf<SendStateError>()

        (params[SendModule.AdapterFields.Amount] as? BigDecimal)?.let { amount ->
            val availableBalance = availableBalance(params)
            if (amount > availableBalance) {
                errors.add(SendStateError.InsufficientAmount(availableBalance))
            }
        }

        val ethereumBalance = balanceInBigDecimal(ethereumKit.balance, 18)
        val expectedFee = fee(params)

        if (ethereumBalance < expectedFee) {
            errors.add(SendStateError.InsufficientFeeBalance(expectedFee))
        }

        return errors
    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val mineAddress = ethereumKit.receiveAddress

        val from = TransactionAddress(transaction.from, transaction.from == mineAddress)
        val to = TransactionAddress(transaction.to, transaction.to == mineAddress)

        var amount: BigDecimal

        transaction.value.toBigDecimal().let {
            amount = it.movePointLeft(decimal)
            if (from.mine) {
                amount = -amount
            }
        }

        return TransactionRecord(
                transactionHash = transaction.transactionHash,
                transactionIndex = transaction.transactionIndex ?: 0,
                interTransactionIndex = transaction.interTransactionIndex,
                blockHeight = transaction.blockNumber,
                amount = amount,
                timestamp = transaction.timestamp,
                from = listOf(from),
                to = listOf(to)
        )
    }

    companion object {
        fun clear(context: Context) {
            Erc20Kit.clear(context)
        }
    }

}
