package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.ActivateCoinManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.Transaction
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ISendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchSwapParameters
import io.horizontalsystems.bankwallet.modules.swap.oneinch.scaleUp
import io.horizontalsystems.ethereumkit.contracts.Bytes32Array
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapMethodDecoration
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger

class OneInchSendEvmTransactionService(
    private val evmKitWrapper: EvmKitWrapper,
    private val feeService: OneInchFeeService,
    private val activateCoinManager: ActivateCoinManager
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
        buildDecorationFromParameters(feeService.parameters)
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
            val swapInfo = SendEvmData.OneInchSwapInfo(
                coinFrom = it.coinFrom,
                coinTo = it.coinTo,
                amountFrom = it.amountFrom,
                estimatedAmountTo = it.amountTo,
                slippage = it.slippage,
                recipient = parameters.recipient
            )
            SendEvmData.AdditionalInfo.OneInchSwap(swapInfo)
        }
    }

    private fun buildDecorationFromParameters(parameters: OneInchSwapParameters): OneInchMethodDecoration {
        val fromToken = getSwapToken(parameters.coinFrom)
        val toToken = getSwapToken(parameters.coinTo)
        val fromAmount = parameters.amountFrom.scaleUp(parameters.coinFrom.decimals)
        val minReturnAmount = parameters.amountTo - parameters.amountTo / BigDecimal("100") * parameters.slippage
        val toAmountMin = minReturnAmount.scaleUp(parameters.coinTo.decimals)

        return parameters.let {
            if (parameters.recipient == null) {
                OneInchUnoswapMethodDecoration(
                    fromToken = fromToken,
                    toToken = toToken,
                    fromAmount = fromAmount,
                    toAmountMin = toAmountMin,
                    toAmount = null,
                    params = Bytes32Array(arrayOf())
                )
            } else
                OneInchSwapMethodDecoration(
                    fromToken = fromToken,
                    toToken = toToken,
                    fromAmount = fromAmount,
                    toAmountMin = toAmountMin,
                    toAmount = null,
                    flags = BigInteger.ZERO,
                    permit = byteArrayOf(),
                    data = byteArrayOf(),
                    recipient = Address(parameters.recipient.hex)
                )
        }
    }

    private fun getSwapToken(coin: PlatformCoin): OneInchMethodDecoration.Token {
        return when (val coinType = coin.coinType) {
            CoinType.Ethereum,
            CoinType.BinanceSmartChain -> OneInchMethodDecoration.Token.EvmCoin
            is CoinType.Erc20 -> OneInchMethodDecoration.Token.Eip20(Address(coinType.address))
            is CoinType.Bep20 -> OneInchMethodDecoration.Token.Eip20(Address(coinType.address))
            else -> throw IllegalStateException("Not supported coin for swap")
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
                activateSwapCoinOut()
                sendState = SendEvmTransactionService.SendState.Sent(fullTransaction.transaction.hash)
                logger.info("success")
            }, { error ->
                sendState = SendEvmTransactionService.SendState.Failed(error)
                logger.warning("failed", error)
            })
            .let { disposable.add(it) }
    }

    private fun activateSwapCoinOut() {
        activateCoinManager.activate(feeService.parameters.coinTo.coinType)
    }

    override fun clear() {
        disposable.clear()
    }
}
