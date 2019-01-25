package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputSelector
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class BitcoinAdapter(val words: List<String>, network: BitcoinKit.NetworkType, newWallet: Boolean, walletId: String?) : IAdapter, BitcoinKit.Listener {

    private var bitcoinKit = BitcoinKit(words, network, newWallet = newWallet, walletId = walletId)
    private val satoshisInBitcoin = Math.pow(10.0, 8.0)

    private val progressSubject: BehaviorSubject<Double> = BehaviorSubject.createDefault(0.0)

    private val balanceSubject: BehaviorSubject<Double> = BehaviorSubject.createDefault(balance)
    private val stateSubject: BehaviorSubject<AdapterState> = BehaviorSubject.createDefault(AdapterState.Syncing(progressSubject))

    override val balance: Double
        get() = bitcoinKit.balance / satoshisInBitcoin

    override val balanceObservable: Flowable<Double> = balanceSubject.toFlowable(BackpressureStrategy.DROP)
    override val stateObservable: Flowable<AdapterState> = stateSubject.toFlowable(BackpressureStrategy.DROP)

    override val confirmationsThreshold: Int = 6
    override val lastBlockHeight: Int get() = bitcoinKit.lastBlockHeight
    override val lastBlockHeightUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    override val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val debugInfo: String = ""

    override val receiveAddress: String get() = bitcoinKit.receiveAddress()

    override fun start() {
        bitcoinKit.listener = this
        bitcoinKit.start()
    }

    override fun refresh() {
        bitcoinKit.refresh()
    }

    override fun clear() {
        bitcoinKit.clear()
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        val paymentData = bitcoinKit.parsePaymentAddress(address)
        return PaymentRequestAddress(paymentData.address, paymentData.amount)
    }

    override fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))?) {
        try {
            bitcoinKit.send(address, (value * satoshisInBitcoin).toInt())
            completion?.invoke(null)
        } catch (ex: Exception) {
            completion?.invoke(ex)
        }
    }

    override fun fee(value: Double, address: String?, senderPay: Boolean): Double {
        try {
            val amount = (value * satoshisInBitcoin).toInt()
            val fee = bitcoinKit.fee(amount, address, senderPay)
            return fee / satoshisInBitcoin
        } catch (e: UnspentOutputSelector.Error.InsufficientUnspentOutputs) {
            val fee = e.fee / satoshisInBitcoin
            throw Error.InsufficientAmount(fee)
        } catch (e: UnspentOutputSelector.Error.EmptyUnspentOutputs) {
            val fee = 0.0
            throw Error.InsufficientAmount(fee)
        }
    }

    override fun validate(address: String) {
        bitcoinKit.validateAddress(address)
    }

    override fun getTransactionsObservable(hashFrom: String?, limit: Int): Single<List<TransactionRecord>> {
        return bitcoinKit.transactions(hashFrom, limit).map { it.map { transactionRecord(it) } }
    }

    //
    // BitcoinKit Listener implementations
    //
    override fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long) {
        balanceSubject.onNext(balance / satoshisInBitcoin)
    }

    override fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, blockInfo: BlockInfo) {
        lastBlockHeightUpdatedSignal.onNext(Unit)
    }

    override fun onKitStateUpdate(bitcoinKit: BitcoinKit, state: BitcoinKit.KitState) {
        when (state) {
            is BitcoinKit.KitState.Synced -> {
                if (stateSubject.value !is AdapterState.Synced) {
                    stateSubject.onNext(AdapterState.Synced)
                }
            }
            is BitcoinKit.KitState.NotSynced -> {
                stateSubject.onNext(AdapterState.NotSynced)
            }
            is BitcoinKit.KitState.Syncing -> {
                progressSubject.onNext(state.progress)

                if (stateSubject.value !is AdapterState.Syncing) {
                    stateSubject.onNext(AdapterState.Syncing(progressSubject))
                }
            }
        }
    }

    override fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>) {
        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }
        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)

    }

    private fun transactionRecord(transaction: TransactionInfo) =
            TransactionRecord(
                    transaction.transactionHash,
                    transaction.blockHeight?.toLong() ?: 0,
                    transaction.amount / satoshisInBitcoin,
                    transaction.timestamp,
                    transaction.from.map {
                        val address = TransactionAddress()
                        address.address = it.address
                        address.mine = it.mine
                        address
                    },
                    transaction.to.map {
                        val address = TransactionAddress()
                        address.address = it.address
                        address.mine = it.mine
                        address
                    }
            )

    companion object {

        fun createBitcoin(words: List<String>, testMode: Boolean, newWallet: Boolean, walletId: String?): BitcoinAdapter {
            val network = if (testMode) BitcoinKit.NetworkType.TestNet else BitcoinKit.NetworkType.MainNet
            return BitcoinAdapter(words, network, newWallet, walletId)
        }

        fun createBitcoinCash(words: List<String>, testMode: Boolean, newWallet: Boolean, walletId: String?): BitcoinAdapter {
            val network = if (testMode) BitcoinKit.NetworkType.TestNetBitCash else BitcoinKit.NetworkType.MainNetBitCash
            return BitcoinAdapter(words, network, newWallet, walletId)
        }
    }

}
