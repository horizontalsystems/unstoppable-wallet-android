package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.managers.ActivateCoinManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ISendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.swap.oneinch.scaleUp
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger

class OneInchSendEvmTransactionService(
        private val evmKit: EthereumKit,
        private val transactionFeeService: OneInchTransactionFeeService,
        private val activateCoinManager: ActivateCoinManager
) : ISendEvmTransactionService, Clearable {

    private val disposable = CompositeDisposable()

    private val stateSubject = PublishSubject.create<SendEvmTransactionService.State>()
    override var state: SendEvmTransactionService.State = SendEvmTransactionService.State.NotReady()
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    override val stateObservable: Flowable<SendEvmTransactionService.State> = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val txDataStateSubject = PublishSubject.create<DataState<SendEvmTransactionService.TxDataState>>()
    override var txDataState: DataState<SendEvmTransactionService.TxDataState> = DataState.Success(SendEvmTransactionService.TxDataState(null, getAdditionalInfo(transactionFeeService.parameters), getSwapDecoration(transactionFeeService.parameters)))
        private set(value) {
            field = value
            txDataStateSubject.onNext(value)
        }
    override val txDataStateObservable: Flowable<DataState<SendEvmTransactionService.TxDataState>> = txDataStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val sendStateSubject = PublishSubject.create<SendEvmTransactionService.SendState>()
    override var sendState: SendEvmTransactionService.SendState = SendEvmTransactionService.SendState.Idle
        private set(value) {
            field = value
            sendStateSubject.onNext(value)
        }
    override val sendStateObservable: Flowable<SendEvmTransactionService.SendState> = sendStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val ownAddress: Address
        get() = evmKit.receiveAddress

    private val evmBalance: BigInteger
        get() = evmKit.accountState?.balance ?: BigInteger.ZERO

    init {
        transactionFeeService.transactionStatusObservable
                .subscribeIO { syncState() }
                .let { disposable.add(it) }

        transactionFeeService.gasPriceType = EvmTransactionService.GasPriceType.Recommended

    }

    private fun syncState() {
        when (val status = transactionFeeService.transactionStatus) {
            is DataState.Error -> {
                state = SendEvmTransactionService.State.NotReady(listOf(status.error))
            }
            DataState.Loading -> {
                state = SendEvmTransactionService.State.NotReady()
            }
            is DataState.Success -> {
                val transaction = status.data
                state = if (transaction.totalAmount > evmBalance) {
                    SendEvmTransactionService.State.NotReady(listOf(SendEvmTransactionService.TransactionError.InsufficientBalance(transaction.totalAmount)))
                } else {
                    SendEvmTransactionService.State.Ready
                }
            }
        }

        syncTxDataState()
    }

    private fun syncTxDataState() {
        txDataState = when (val status = transactionFeeService.transactionStatus) {
            is DataState.Error -> {
                DataState.Error(status.error)
            }
            DataState.Loading -> {
                DataState.Loading
            }
            is DataState.Success -> {
                val transactionData = status.data.data //TODO rename last data to 'transactionData'

                //TODO get decoration using evmKit
                val decoration = getSwapDecoration(transactionFeeService.parameters) /*evmKit.decorate(transactionData)*/
                val additionalInfo = getAdditionalInfo(transactionFeeService.parameters)

                DataState.Success(SendEvmTransactionService.TxDataState(transactionData, additionalInfo, decoration))
            }
        }
    }

    private fun getAdditionalInfo(parameters: OneInchSwapParameters): SendEvmData.AdditionalInfo {
        fun getFormattedSlippage(slippage: BigDecimal): String? {
            return if (slippage.compareTo(OneInchSwapSettingsModule.defaultSlippage) == 0) {
                null
            } else {
                "$slippage%"
            }
        }

        return parameters.let {
            val swapInfo = SendEvmData.SwapInfo(
                    estimatedIn = it.amountFrom,
                    estimatedOut = it.amountTo,
                    slippage = getFormattedSlippage(it.slippage),
                    recipientDomain = it.recipient
            )
            SendEvmData.AdditionalInfo.Swap(swapInfo)
        }
    }

    private fun getSwapDecoration(parameters: OneInchSwapParameters): TransactionDecoration.Swap {
        fun getSwapToken(coin: Coin): TransactionDecoration.Swap.Token {
            return when (val coinType = coin.type) {
                CoinType.Ethereum,
                CoinType.BinanceSmartChain -> TransactionDecoration.Swap.Token.EvmCoin
                is CoinType.Erc20 -> TransactionDecoration.Swap.Token.Eip20Coin(Address(coinType.address))
                is CoinType.Bep20 -> TransactionDecoration.Swap.Token.Eip20Coin(Address(coinType.address))
                else -> throw IllegalStateException("Not supported coin for swap")
            }
        }

        return parameters.let {
            val amountIn = it.amountFrom.scaleUp(it.coinFrom.decimal)

            val amountOutMinDecimal = it.amountTo - it.amountTo / BigDecimal("100") * it.slippage
            val trade = TransactionDecoration.Swap.Trade.ExactIn(amountIn, amountOutMinDecimal.scaleUp(it.coinFrom.decimal))

            TransactionDecoration.Swap(
                    trade,
                    getSwapToken(it.coinFrom),
                    getSwapToken(it.coinTo),
                    it.recipient?.let { recipient -> Address(recipient) } ?: ownAddress,
                    BigInteger.ZERO)
        }
    }


    override fun send(logger: AppLogger) {
        if (state != SendEvmTransactionService.State.Ready) {
            logger.info("state is not Ready: ${state.javaClass.simpleName}")
            return
        }
        val transaction = transactionFeeService.transactionStatus.dataOrNull ?: return

        sendState = SendEvmTransactionService.SendState.Sending
        logger.info("sending tx")

        evmKit.send(transaction.data, transaction.gasData.gasPrice, transaction.gasData.gasLimit)
                .subscribeIO({ fullTransaction ->
                    activateSwapCoinOut()
                    sendState = SendEvmTransactionService.SendState.Sent(fullTransaction.transaction.hash)
                    logger.info("success txHash: ${fullTransaction.transaction.hash.toHexString()}")
                }, { error ->
                    sendState = SendEvmTransactionService.SendState.Failed(error)
                    logger.warning("failed", error)
                })
                .let { disposable.add(it) }
    }

    private fun activateSwapCoinOut() {
        activateCoinManager.activate(transactionFeeService.parameters.coinTo.type)
    }

    override fun clear() {
        disposable.clear()
    }
}
