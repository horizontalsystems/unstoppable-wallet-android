package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.core.WrongParameters
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorError
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode

abstract class BitcoinBaseAdapter(open val kit: AbstractKit, private val addressParser: AddressParser) : IAdapter {

    open val receiveScriptType = ScriptType.P2PKH
    open val changeScriptType = ScriptType.P2PKH
    abstract val satoshisInBitcoin: BigDecimal

    //
    // Adapter implementation
    //

    override val feeCoinCode: String? = null
    override val decimal = 8

    override val confirmationsThreshold: Int = 6
    override val lastBlockHeight: Int?
        get() = kit.lastBlockInfo?.height

    override val receiveAddress: String
        get() = kit.receiveAddress(receiveScriptType)

    protected val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val lastBlockHeightUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val lastBlockHeightUpdatedFlowable: Flowable<Unit>
        get() = lastBlockHeightUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val stateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = transactionRecordsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val debugInfo: String = ""

    override val balance: BigDecimal
        get() = BigDecimal.valueOf(kit.balance).divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN)

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

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        val paymentData = addressParser.parse(address)
        var addressError: AddressError.InvalidPaymentAddress? = null
        try {
            validate(paymentData.address)
        } catch (e: Exception) {
            addressError = AddressError.InvalidPaymentAddress()
        }
        return PaymentRequestAddress(paymentData.address, paymentData.amount?.toBigDecimal(), error = addressError)
    }

    override fun send(params: Map<SendModule.AdapterFields, Any?>): Single<Unit> {
        val coinValue = params[SendModule.AdapterFields.CoinValue] as? CoinValue
                ?: throw WrongParameters()
        val feeRate = params[SendModule.AdapterFields.FeeRate] as? Long
                ?: throw WrongParameters()
        val address = params[SendModule.AdapterFields.Address] as? String
                ?: throw WrongParameters()

        return Single.create { emitter ->
            try {
                kit.send(address, (coinValue.value * satoshisInBitcoin).toLong(), feeRate = feeRate.toInt(), changeScriptType = changeScriptType)
                emitter.onSuccess(Unit)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun fee(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        val amount = params[SendModule.AdapterFields.CoinAmountInBigDecimal] as? BigDecimal
                ?: throw WrongParameters()
        val feeRate = params[SendModule.AdapterFields.FeeRate] as? Long
                ?: throw WrongParameters()
        val address = params[SendModule.AdapterFields.Address] as? String

        return try {
            val satoshiAmount = (amount * satoshisInBitcoin).toLong()
            val fee = kit.fee(satoshiAmount, address, true, feeRate = feeRate.toInt(), changeScriptType = changeScriptType)
            BigDecimal.valueOf(fee).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: UnspentOutputSelectorError.InsufficientUnspentOutputs) {
            BigDecimal.valueOf(e.fee).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    override fun availableBalance(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        val mutableParamsMap = params.toMutableMap()
        mutableParamsMap[SendModule.AdapterFields.CoinAmountInBigDecimal] = balance
        return BigDecimal.ZERO.max(balance.subtract(fee(mutableParamsMap)))
    }

    override fun validate(params: Map<SendModule.AdapterFields, Any?>): List<SendStateError> {
        val coinAmount = params[SendModule.AdapterFields.CoinAmountInBigDecimal] as? BigDecimal
                ?: throw WrongParameters()

        val errors = mutableListOf<SendStateError>()
        val availableBalance = availableBalance(params)
        if (coinAmount > availableBalance) {
            errors.add(SendStateError.InsufficientAmount(availableBalance))
        }
        return errors
    }

    override fun validate(address: String) {
        kit.validateAddress(address)
    }

    fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        return TransactionRecord(
                transactionHash = transaction.transactionHash,
                transactionIndex = transaction.transactionIndex,
                interTransactionIndex = 0,
                blockHeight = transaction.blockHeight?.toLong(),
                amount = transaction.amount.toBigDecimal().divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN),
                timestamp = transaction.timestamp,
                from = emptyList(),
                to = transaction.to.map { TransactionAddress(it.address, it.mine) }
        )
    }
}
