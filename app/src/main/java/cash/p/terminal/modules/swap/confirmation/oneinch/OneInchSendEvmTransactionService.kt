package cash.p.terminal.modules.swap.confirmation.oneinch

import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.Clearable
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.evmfee.Transaction
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.sendevmtransaction.ISendEvmTransactionService
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionService
import cash.p.terminal.modules.swap.SwapViewItemHelper
import cash.p.terminal.modules.swap.oneinch.OneInchSwapParameters
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.RoundingMode

class OneInchSendEvmTransactionService(
    private val evmKitWrapper: EvmKitWrapper,
    private val feeService: OneInchFeeService,
    private val helper: SwapViewItemHelper
) : ISendEvmTransactionService, Clearable {

    private val evmKit = evmKitWrapper.evmKit

    private val disposable = CompositeDisposable()

    private val stateSubject = PublishSubject.create<SendEvmTransactionService.State>()
    override var state: SendEvmTransactionService.State = SendEvmTransactionService.State.NotReady()
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    override val stateObservable: Flowable<SendEvmTransactionService.State> =
        stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    override var txDataState: SendEvmTransactionService.TxDataState = SendEvmTransactionService.TxDataState(
        null,
        getAdditionalInfo(feeService.parameters),
        null
    )
        private set

    private val sendStateSubject = PublishSubject.create<SendEvmTransactionService.SendState>()
    override var sendState: SendEvmTransactionService.SendState = SendEvmTransactionService.SendState.Idle
        private set(value) {
            field = value
            sendStateSubject.onNext(value)
        }
    override val sendStateObservable: Flowable<SendEvmTransactionService.SendState> =
        sendStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val ownAddress: Address
        get() = evmKit.receiveAddress

    init {
        feeService.transactionStatusObservable
            .subscribeIO { sync(it) }
            .let { disposable.add(it) }
    }

    private fun sync(transactionStatus: DataState<Transaction>) {
        when (transactionStatus) {
            is DataState.Error -> {
                state = SendEvmTransactionService.State.NotReady(errors = listOf(transactionStatus.error))
            }
            DataState.Loading -> {
                state = SendEvmTransactionService.State.NotReady()
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data
                syncTxDataState(transaction)

                state = if (transaction.errors.isNotEmpty()) {
                    SendEvmTransactionService.State.NotReady(transaction.warnings, transaction.errors)
                } else {
                    SendEvmTransactionService.State.Ready(transaction.warnings)
                }
            }
        }
    }

    private fun syncTxDataState(transaction: Transaction) {
        val transactionData = transaction.transactionData
        val decoration = evmKit.decorate(transactionData)
        val additionalInfo = getAdditionalInfo(feeService.parameters)

        txDataState = SendEvmTransactionService.TxDataState(
            transactionData,
            additionalInfo,
            decoration
        )
    }

    private fun getAdditionalInfo(parameters: OneInchSwapParameters): SendEvmData.AdditionalInfo {
        return parameters.let {
            val sellPrice = it.amountTo.divide(it.amountFrom, it.tokenFrom.decimals, RoundingMode.HALF_EVEN).stripTrailingZeros()
            val buyPrice = it.amountFrom.divide(it.amountTo, it.tokenTo.decimals, RoundingMode.HALF_EVEN).stripTrailingZeros()
            val (primaryPrice, secondaryPrice) = helper.prices(sellPrice, buyPrice, it.tokenFrom, it.tokenTo)
            val swapInfo = SendEvmData.OneInchSwapInfo(
                tokenFrom = it.tokenFrom,
                tokenTo = it.tokenTo,
                amountFrom = it.amountFrom,
                estimatedAmountTo = it.amountTo,
                slippage = it.slippage,
                recipient = it.recipient,
                price = primaryPrice
            )
            SendEvmData.AdditionalInfo.OneInchSwap(swapInfo)
        }
    }

    override fun send(logger: AppLogger) {
        if (state !is SendEvmTransactionService.State.Ready) {
            logger.info("state is not Ready: ${state.javaClass.simpleName}")
            return
        }
        val transaction = feeService.transactionStatus.dataOrNull ?: return

        sendState = SendEvmTransactionService.SendState.Sending
        logger.info("sending tx")

        evmKitWrapper.sendSingle(
            transaction.transactionData,
            transaction.gasData.gasPrice,
            transaction.gasData.gasLimit
        )
            .subscribeIO({ fullTransaction ->
                sendState = SendEvmTransactionService.SendState.Sent(fullTransaction.transaction.hash)
                logger.info("success")
            }, { error ->
                sendState = SendEvmTransactionService.SendState.Failed(error)
                logger.warning("failed", error)
            })
            .let { disposable.add(it) }
    }

    override fun methodName(input: ByteArray): String? = null

    override fun clear() {
        disposable.clear()
    }
}
