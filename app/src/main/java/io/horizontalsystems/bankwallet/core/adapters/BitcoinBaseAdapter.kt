package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.transactions.TransactionLockInfo
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.TransactionStatus
import io.horizontalsystems.hodler.HodlerOutputData
import io.horizontalsystems.hodler.HodlerPlugin
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

abstract class BitcoinBaseAdapter(open val kit: AbstractKit, open val settings: BlockchainSetting?)
    : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter {

    abstract val satoshisInBitcoin: BigDecimal

    //
    // Adapter implementation
    //

    override val confirmationsThreshold: Int = defaultConfirmationsThreshold

    override val lastBlockInfo: LastBlockInfo?
        get() = kit.lastBlockInfo?.let { LastBlockInfo(it.height, it.timestamp) }

    override val receiveAddress: String
        get() = kit.receiveAddress()

    override fun getReceiveAddressType(wallet: Wallet): String? = null

    protected val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = lastBlockUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val stateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = transactionRecordsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val debugInfo: String = ""

    override val balance: BigDecimal
        get() = satoshiToBTC(kit.balance.spendable)

    override val balanceLocked: BigDecimal?
        get() = if (kit.balance.unspendable > 0L) satoshiToBTC(kit.balance.unspendable) else null

    override var state: AdapterState = AdapterState.Syncing(0, null)
        set(value) {
            field = value
            adapterStateUpdatedSubject.onNext(Unit)
        }

    override fun start() {
        kit.start()
    }

    override fun stop() {
        kit.stop()
    }

    override fun refresh() {
        kit.refresh()
    }

    fun send(amount: BigDecimal, address: String, feeRate: Long, pluginData: Map<Byte, IPluginData>?): Single<Unit> {
        return Single.create { emitter ->
            try {
                kit.send(address, (amount * satoshisInBitcoin).toLong(), feeRate = feeRate.toInt(), pluginData = pluginData
                        ?: mapOf())
                emitter.onSuccess(Unit)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    fun availableBalance(feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal {
        return try {
            val maximumSpendableValue = kit.maximumSpendableValue(address, feeRate.toInt(), pluginData
                    ?: mapOf())
            satoshiToBTC(maximumSpendableValue, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun minimumSendAmount(address: String?): BigDecimal {
        return satoshiToBTC(kit.minimumSpendableValue(address).toLong(), RoundingMode.CEILING)
    }

    fun maximumSendAmount(pluginData: Map<Byte, IPluginData>): BigDecimal? {
        return kit.maximumSpendLimit(pluginData)?.let { maximumSpendLimit ->
            satoshiToBTC(maximumSpendLimit, RoundingMode.CEILING)
        }
    }

    fun fee(amount: BigDecimal, feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal {
        return try {
            val satoshiAmount = (amount * satoshisInBitcoin).toLong()
            val fee = kit.fee(satoshiAmount, address, senderPay = true, feeRate = feeRate.toInt(), pluginData = pluginData
                    ?: mapOf())
            satoshiToBTC(fee, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun validate(address: String, pluginData: Map<Byte, IPluginData>?) {
        kit.validateAddress(address, pluginData ?: mapOf())
    }

    fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        var myInputsTotalValue = 0L
        var allInputsMine = true
        transaction.inputs.forEach { input ->
            if (input.mine) {
                myInputsTotalValue += input.value ?: 0
            } else {
                allInputsMine = false
            }
        }

        var myOutputsTotalValue = 0L
        var myChangeOutputsTotalValue = 0L
        transaction.outputs.filter { it.value > 0 }.forEach { output ->
            if (output.mine) {
                myOutputsTotalValue += output.value

                if (output.changeOutput) {
                    myChangeOutputsTotalValue += output.value
                }
            }
        }

        var amount = myOutputsTotalValue - myInputsTotalValue

        if (allInputsMine) {
            amount += transaction.fee ?: 0
        }

        val type = when {
            amount > 0 -> TransactionType.Incoming
            amount < 0 -> TransactionType.Outgoing
            else -> {
                amount = myOutputsTotalValue - myChangeOutputsTotalValue
                TransactionType.SentToSelf
            }
        }

        var transactionLockInfo: TransactionLockInfo? = null
        val lockedOutput = transaction.outputs.firstOrNull { it.pluginId == HodlerPlugin.id }
        if (lockedOutput != null) {
            val hodlerOutputData = lockedOutput.pluginData as? HodlerOutputData
            hodlerOutputData?.approxUnlockTime?.let { approxUnlockTime ->
                val lockedValueBTC = satoshiToBTC(lockedOutput.value)
                transactionLockInfo = TransactionLockInfo(Date(approxUnlockTime * 1000), hodlerOutputData.addressString, lockedValueBTC)
            }
        }

        var from: String? = null
        var to: String? = null
        if (type == TransactionType.Incoming) {
            from = transaction.inputs.firstOrNull { !it.mine }?.address
        } else if (type == TransactionType.Outgoing) {
            to = transaction.outputs.firstOrNull { it.value > 0 && !it.mine }?.address
        }

        return TransactionRecord(
                uid = transaction.uid,
                transactionHash = transaction.transactionHash,
                transactionIndex = transaction.transactionIndex,
                interTransactionIndex = 0,
                blockHeight = transaction.blockHeight?.toLong(),
                amount = satoshiToBTC(amount),
                fee = satoshiToBTC(transaction.fee),
                timestamp = transaction.timestamp,
                from = from,
                to = to,
                type = type,
                failed = transaction.status == TransactionStatus.INVALID,
                lockInfo = transactionLockInfo,
                conflictingTxHash = transaction.conflictingTxHash
        )
    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    private fun satoshiToBTC(value: Long, roundingMode: RoundingMode = RoundingMode.HALF_EVEN): BigDecimal {
        return BigDecimal(value).divide(satoshisInBitcoin, decimal, roundingMode)
    }

    private fun satoshiToBTC(value: Long?): BigDecimal? {
        return satoshiToBTC(value ?: return null)
    }

    companion object {
        const val defaultConfirmationsThreshold = 3
        const val decimal = 8

        fun getBip(derivation: AccountType.Derivation?): Bip = when (derivation) {
            AccountType.Derivation.bip49 -> Bip.BIP49
            AccountType.Derivation.bip84 -> Bip.BIP84
            else -> Bip.BIP44
        }

        fun getSyncMode(mode: SyncMode?): BitcoinCore.SyncMode {
            return when (mode) {
                SyncMode.Slow -> BitcoinCore.SyncMode.Full()
                SyncMode.New -> BitcoinCore.SyncMode.NewWallet()
                SyncMode.Fast -> BitcoinCore.SyncMode.Api()
                null -> throw AdapterErrorWrongParameters("SyncMode is null")
            }
        }
    }

}
