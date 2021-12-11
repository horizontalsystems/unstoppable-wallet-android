package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapTradeService
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoAddressMapper
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.erc20kit.decorations.ApproveMethodDecoration
import io.horizontalsystems.erc20kit.decorations.TransferMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.RecognizedMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapMethodDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration
import io.reactivex.disposables.CompositeDisposable
import java.math.BigInteger

class SendEvmTransactionViewModel(
    private val service: ISendEvmTransactionService,
    private val coinServiceFactory: EvmCoinServiceFactory
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val sendEnabledLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>()

    val sendingLiveData = MutableLiveData<Unit>()
    val sendSuccessLiveData = MutableLiveData<ByteArray>()
    val sendFailedLiveData = MutableLiveData<String>()

    val viewItemsLiveData = MutableLiveData<List<SectionViewItem>>()
    val transactionTitleLiveData = MutableLiveData<String>()

    init {
        service.stateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.txDataStateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.sendStateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
        sync(service.txDataState)
        sync(service.sendState)
    }

    fun send(logger: AppLogger) {
        service.send(logger)
    }

    private fun sync(state: SendEvmTransactionService.State) =
        when (state) {
            SendEvmTransactionService.State.Ready -> {
                sendEnabledLiveData.postValue(true)
                errorLiveData.postValue(null)
            }
            is SendEvmTransactionService.State.NotReady -> {
                sendEnabledLiveData.postValue(false)
                errorLiveData.postValue(state.errors.firstOrNull()?.let { convertError(it) })
            }
        }

    private fun sync(txDataState: DataState<SendEvmTransactionService.TxDataState>) {
        val decoration = txDataState.dataOrNull?.decoration
        val transactionData = txDataState.dataOrNull?.transactionData

        transactionTitleLiveData.postValue(getTransactionTitle(decoration, transactionData))

        var viewItems = when {
            decoration is SwapMethodDecoration || decoration is OneInchMethodDecoration -> {
                getSwapViewItems(decoration, txDataState.dataOrNull?.additionalInfo)
            }
            decoration != null && transactionData != null -> {
                getViewItems(decoration, transactionData, txDataState.dataOrNull?.additionalInfo)
            }
            decoration == null && transactionData != null -> {
                getSendEvmCoinViewItems(transactionData, txDataState.dataOrNull?.additionalInfo)
            }
            else -> null
        }

        if (viewItems == null && transactionData != null) {
            viewItems = getFallbackViewItems(transactionData)
        }

        viewItems?.let {
            viewItemsLiveData.postValue(it)
        }
    }

    private fun getTransactionTitle(
        decoration: ContractMethodDecoration?,
        transactionData: TransactionData?
    ) =
        if (decoration == null && transactionData?.input?.isEmpty() == true) {
            Translator.getString(R.string.WalletConnect_SendRequest_Title)
        } else {
            when (decoration) {
                is TransferMethodDecoration -> Translator.getString(R.string.WalletConnect_SendRequest_Title)
                is ApproveMethodDecoration -> Translator.getString(R.string.WalletConnect_ApproveRequest_Title)
                is SwapMethodDecoration,
                is OneInchMethodDecoration -> Translator.getString(R.string.WalletConnect_SwapRequest_Title)
                else -> Translator.getString(R.string.WalletConnect_UnknownRequest_Title)
            }
        }

    private fun sync(sendState: SendEvmTransactionService.SendState) =
        when (sendState) {
            SendEvmTransactionService.SendState.Idle -> Unit
            SendEvmTransactionService.SendState.Sending -> {
                sendEnabledLiveData.postValue(false)
                sendingLiveData.postValue(Unit)
            }
            is SendEvmTransactionService.SendState.Sent -> sendSuccessLiveData.postValue(sendState.transactionHash)
            is SendEvmTransactionService.SendState.Failed -> sendFailedLiveData.postValue(
                convertError(sendState.error)
            )
        }

    private fun getViewItems(
        decoration: ContractMethodDecoration,
        transactionData: TransactionData,
        additionalInfo: SendEvmData.AdditionalInfo?
    ): List<SectionViewItem>? =
        when (decoration) {
            is TransferMethodDecoration -> getEip20TransferViewItems(
                decoration.to,
                decoration.value,
                transactionData.to,
                transactionData.nonce,
                additionalInfo
            )
            is ApproveMethodDecoration -> getEip20ApproveViewItems(
                decoration.spender,
                decoration.value,
                transactionData.to,
                transactionData.nonce
            )
            is RecognizedMethodDecoration -> getRecognizedMethodItems(
                transactionData,
                decoration.method,
                decoration.arguments
            )
            is UnknownMethodDecoration -> getUnknownMethodItems(transactionData)
            else -> null
        }

    private fun getSwapViewItems(
        decoration: ContractMethodDecoration,
        additionalInfo: SendEvmData.AdditionalInfo?
    ): List<SectionViewItem>? =
        when (decoration) {
            is SwapMethodDecoration -> getUniswapViewItems(
                decoration.trade,
                decoration.tokenIn,
                decoration.tokenOut,
                decoration.to,
                additionalInfo
            )
            is OneInchMethodDecoration -> getOneInchSwapViewItems(decoration, additionalInfo)
            else -> null
        }

    private fun getOneInchSwapViewItems(
        decoration: OneInchMethodDecoration,
        additionalInfo: SendEvmData.AdditionalInfo?
    ): List<SectionViewItem>? {
        var fromToken: OneInchMethodDecoration.Token? = null
        var toToken: OneInchMethodDecoration.Token? = null
        var fromAmount = BigInteger.ZERO
        var toAmountMin = BigInteger.ZERO
        var recipient: Address? = null

        when (decoration) {
            is OneInchUnoswapMethodDecoration -> {
                fromToken = decoration.fromToken
                toToken = decoration.toToken
                fromAmount = decoration.fromAmount
                toAmountMin = decoration.toAmountMin
            }
            is OneInchSwapMethodDecoration -> {
                fromToken = decoration.fromToken
                toToken = decoration.toToken
                fromAmount = decoration.fromAmount
                toAmountMin = decoration.toAmountMin
                recipient = decoration.recipient
            }
        }

        val info = additionalInfo?.oneInchSwapInfo
        val sections = mutableListOf<SectionViewItem>()

        val fromCoinService = fromToken?.let { getCoinService(it) } ?: return null
        val toCoinService = if (toToken == null)
            info?.coinTo?.let { getCoinService(it) }
        else
            getCoinService(toToken)

        if (toCoinService == null)
            return null

        sections.add(
            SectionViewItem(
                listOf(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Swap_FromAmountTitle),
                        fromCoinService.platformCoin.name
                    ),
                    getAmount(fromCoinService.amountData(fromAmount), ValueType.Outgoing)
                )
            )
        )

        sections.add(
            SectionViewItem(
                listOf(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Swap_ToAmountTitle),
                        toCoinService.platformCoin.name
                    ),
                    getEstimatedAmount(
                        info?.let { toCoinService.amountData(it.estimatedAmountTo) },
                        ValueType.Incoming
                    ),
                    getGuaranteedAmount(toCoinService.amountData(toAmountMin))
                )
            )
        )

        val otherViewItems = mutableListOf<ViewItem>()
        info?.slippage?.let {
            otherViewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.SwapSettings_SlippageTitle),
                    it,
                    ValueType.Regular
                )
            )
        }

        if (recipient != null && recipient != service.ownAddress) {
            val addressValue = recipient.eip55
            val addressTitle =
                info?.recipientDomain ?: TransactionInfoAddressMapper.map(addressValue)
            otherViewItems.add(
                ViewItem.Address(
                    Translator.getString(R.string.SwapSettings_RecipientAddressTitle),
                    addressTitle,
                    addressValue
                )
            )
        }

        if (otherViewItems.isNotEmpty()) {
            sections.add(SectionViewItem(otherViewItems))
        }

        return sections
    }

    private fun getEip20TransferViewItems(
        to: Address,
        value: BigInteger,
        contractAddress: Address,
        nonce: Long?,
        additionalInfo: SendEvmData.AdditionalInfo?
    ): List<SectionViewItem>? {
        val coinService = coinServiceFactory.getCoinService(contractAddress) ?: return null

        val viewItems = mutableListOf(
            ViewItem.Subhead(
                Translator.getString(R.string.Send_Confirmation_YouSend),
                coinService.platformCoin.name
            ),
            getAmount(coinService.amountData(value), ValueType.Outgoing)
        )
        val addressValue = to.eip55
        val addressTitle =
            additionalInfo?.sendInfo?.domain ?: TransactionInfoAddressMapper.map(addressValue)
        viewItems.add(
            ViewItem.Address(
                Translator.getString(R.string.Send_Confirmation_To),
                addressTitle,
                value = addressValue
            )
        )
        nonce?.let {
            viewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Send_Confirmation_Nonce),
                    "$it",
                    ValueType.Regular
                ),
            )
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getEip20ApproveViewItems(
        spender: Address,
        value: BigInteger,
        contractAddress: Address,
        nonce: Long?
    ): List<SectionViewItem>? {
        val coinService = coinServiceFactory.getCoinService(contractAddress) ?: return null

        val addressValue = spender.eip55
        val addressTitle = TransactionInfoAddressMapper.map(addressValue)

        val viewItems = mutableListOf(
            ViewItem.Subhead(
                Translator.getString(R.string.Approve_YouApprove),
                coinService.platformCoin.name
            ),
            getAmount(coinService.amountData(value), ValueType.Regular),
            ViewItem.Address(
                Translator.getString(R.string.Approve_Spender),
                addressTitle,
                addressValue
            )
        )
        nonce?.let {
            viewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Send_Confirmation_Nonce),
                    "$it",
                    ValueType.Regular
                ),
            )
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getUniswapViewItems(
        trade: SwapMethodDecoration.Trade,
        tokenIn: SwapMethodDecoration.Token,
        tokenOut: SwapMethodDecoration.Token,
        to: Address,
        additionalInfo: SendEvmData.AdditionalInfo?
    ): List<SectionViewItem>? {

        val coinServiceIn = getCoinService(tokenIn) ?: return null
        val coinServiceOut = getCoinService(tokenOut) ?: return null

        val info = additionalInfo?.uniswapInfo
        val sections = mutableListOf<SectionViewItem>()

        when (trade) {
            is SwapMethodDecoration.Trade.ExactIn -> {
                sections.add(
                    SectionViewItem(
                        listOf(
                            ViewItem.Subhead(
                                Translator.getString(R.string.Swap_FromAmountTitle),
                                coinServiceIn.platformCoin.name
                            ),
                            getAmount(coinServiceIn.amountData(trade.amountIn), ValueType.Outgoing),
                        )
                    )
                )
                sections.add(
                    SectionViewItem(
                        listOf(
                            ViewItem.Subhead(
                                Translator.getString(R.string.Swap_ToAmountTitle),
                                coinServiceOut.platformCoin.name
                            ),
                            getEstimatedAmount(
                                info?.let { coinServiceOut.amountData(it.estimatedOut) },
                                ValueType.Incoming
                            ),
                            getGuaranteedAmount(coinServiceOut.amountData(trade.amountOutMin))
                        )
                    )
                )
            }
            is SwapMethodDecoration.Trade.ExactOut -> {
                sections.add(
                    SectionViewItem(
                        listOf(
                            ViewItem.Subhead(
                                Translator.getString(R.string.Swap_FromAmountTitle),
                                coinServiceIn.platformCoin.name
                            ),
                            getEstimatedAmount(
                                info?.let { coinServiceOut.amountData(it.estimatedIn) },
                                ValueType.Outgoing
                            ),
                            getMaxAmount(coinServiceIn.amountData(trade.amountInMax))
                        )
                    )
                )
                sections.add(
                    SectionViewItem(
                        listOf(
                            ViewItem.Subhead(
                                Translator.getString(R.string.Swap_ToAmountTitle),
                                coinServiceOut.platformCoin.name
                            ),
                            getGuaranteedAmount(coinServiceOut.amountData(trade.amountOut))
                        )
                    )
                )
            }
        }

        val otherViewItems = mutableListOf<ViewItem>()
        info?.slippage?.let {
            otherViewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.SwapSettings_SlippageTitle),
                    it,
                    ValueType.Regular
                )
            )
        }
        info?.deadline?.let {
            otherViewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.SwapSettings_DeadlineTitle),
                    it,
                    ValueType.Regular
                )
            )
        }
        if (to != service.ownAddress) {
            val addressValue = to.eip55
            val addressTitle = info?.recipientDomain
                ?: TransactionInfoAddressMapper.map(addressValue)
            otherViewItems.add(
                ViewItem.Address(
                    Translator.getString(R.string.SwapSettings_RecipientAddressTitle),
                    addressTitle,
                    addressValue
                )
            )
        }
        info?.price?.let {
            otherViewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Swap_Price),
                    it,
                    ValueType.Regular
                )
            )
        }
        info?.priceImpact?.let {
            val color = when (it.level) {
                UniswapTradeService.PriceImpactLevel.Warning -> R.color.jacob
                UniswapTradeService.PriceImpactLevel.Forbidden -> R.color.lucian
                else -> null
            }

            otherViewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Swap_PriceImpact),
                    it.value,
                    ValueType.Regular,
                    color
                )
            )
        }
        if (otherViewItems.isNotEmpty()) {
            sections.add(SectionViewItem(otherViewItems))
        }

        if (info?.priceImpactWarning == true) {
            sections.add(
                SectionViewItem(
                    listOf(
                        ViewItem.Warning(
                            Translator.getString(R.string.Swap_PriceImpact),
                            Translator.getString(R.string.Swap_PriceImpactTooHigh),
                            R.drawable.ic_attention_20
                        )
                    )
                )
            )
        }

        return sections
    }

    private fun getRecognizedMethodItems(
        transactionData: TransactionData,
        method: String,
        arguments: List<Any>
    ): List<SectionViewItem>? {
        val addressValue = transactionData.to.eip55

        val viewItems = mutableListOf(
            getAmount(
                coinServiceFactory.baseCoinService.amountData(transactionData.value),
                ValueType.Outgoing
            ),
            ViewItem.Address(
                Translator.getString(R.string.Send_Confirmation_To),
                addressValue,
                addressValue
            ),
            ViewItem.Subhead(Translator.getString(R.string.Send_Confirmation_Method), method),
            ViewItem.Input(transactionData.input.toHexString())
        )

        return listOf(SectionViewItem(viewItems))
    }

    private fun getUnknownMethodItems(transactionData: TransactionData): List<SectionViewItem>? {
        val addressValue = transactionData.to.eip55

        val viewItems = mutableListOf(
            getAmount(
                coinServiceFactory.baseCoinService.amountData(transactionData.value),
                ValueType.Outgoing
            ),
            ViewItem.Address(
                Translator.getString(R.string.Send_Confirmation_To),
                addressValue,
                addressValue
            ),
            ViewItem.Input(transactionData.input.toHexString())
        )

        return listOf(SectionViewItem(viewItems))
    }

    private fun getCoinService(token: SwapMethodDecoration.Token) = when (token) {
        SwapMethodDecoration.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is SwapMethodDecoration.Token.Eip20Coin -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getCoinService(token: OneInchMethodDecoration.Token) = when (token) {
        OneInchMethodDecoration.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is OneInchMethodDecoration.Token.Eip20 -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getCoinService(coin: PlatformCoin) = when (val coinType = coin.coinType) {
        CoinType.Ethereum, CoinType.BinanceSmartChain -> coinServiceFactory.baseCoinService
        is CoinType.Erc20 -> coinServiceFactory.getCoinService(coinType.address)
        is CoinType.Bep20 -> coinServiceFactory.getCoinService(coinType.address)
        else -> null
    }

    private fun getSendEvmCoinViewItems(
        transactionData: TransactionData,
        additionalInfo: SendEvmData.AdditionalInfo?
    ): List<SectionViewItem> {
        val viewItems = mutableListOf(
            ViewItem.Subhead(
                Translator.getString(R.string.Send_Confirmation_YouSend),
                coinServiceFactory.baseCoinService.platformCoin.name
            ),
            getAmount(
                coinServiceFactory.baseCoinService.amountData(transactionData.value),
                ValueType.Outgoing
            ),
        )
        val addressValue = transactionData.to.eip55
        val addressTitle =
            additionalInfo?.sendInfo?.domain ?: TransactionInfoAddressMapper.map(addressValue)
        viewItems.add(
            ViewItem.Address(
                Translator.getString(R.string.Send_Confirmation_To),
                addressTitle,
                value = addressValue
            )
        )
        transactionData.nonce?.let {
            viewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Send_Confirmation_Nonce),
                    "$it",
                    ValueType.Regular
                ),
            )
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getFallbackViewItems(transactionData: TransactionData): List<SectionViewItem> {
        val addressValue = transactionData.to.eip55
        val viewItems = mutableListOf(
            getAmount(
                coinServiceFactory.baseCoinService.amountData(transactionData.value),
                ValueType.Outgoing
            ),
            ViewItem.Address(
                Translator.getString(R.string.Send_Confirmation_To),
                addressValue,
                addressValue
            ),
            ViewItem.Input(transactionData.input.toHexString())
        )
        transactionData.nonce?.let {
            viewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Send_Confirmation_Nonce),
                    "$it",
                    ValueType.Regular
                ),
            )
        }
        return listOf(SectionViewItem(viewItems))
    }

    private fun getAmount(amountData: SendModule.AmountData, valueType: ValueType) =
        ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            ColoredValue(
                amountData.primary.getFormatted(),
                getAmountColor(valueType)
            )
        )

    private fun getGuaranteedAmount(amountData: SendModule.AmountData): ViewItem.Amount {
        return ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            ColoredValue(
                "${amountData.primary.getFormatted()} ${Translator.getString(R.string.Swap_AmountMin)}",
                getAmountColor(ValueType.Regular)
            )
        )
    }

    private fun getMaxAmount(amountData: SendModule.AmountData): ViewItem.Amount {
        return ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            ColoredValue(
                "${amountData.primary.getFormatted()} ${Translator.getString(R.string.Swap_AmountMax)}",
                getAmountColor(ValueType.Regular)
            )
        )
    }

    private fun getEstimatedAmount(
        amountData: SendModule.AmountData?,
        type: ValueType
    ): ViewItem {
        val coinAmount = amountData?.primary?.getFormatted()

        val coinAmountString = if (coinAmount != null) {
            "$coinAmount" + " " + Translator.getString(R.string.Swap_AmountEstimated)
        } else {
            "---"
        }
        val amountColor = getAmountColor(if (coinAmount != null) type else ValueType.Disabled)

        return ViewItem.Amount(
            amountData?.secondary?.getFormatted() ?: "---",
            ColoredValue(coinAmountString, amountColor)
        )
    }

    private fun convertError(error: Throwable) =
        when (val convertedError = error.convertedError) {
            is SendEvmTransactionService.TransactionError.InsufficientBalance -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_InsufficientBalance,
                    coinServiceFactory.baseCoinService.coinValue(convertedError.requiredBalance)
                        .getFormatted()
                )
            }
            is EvmError.InsufficientBalanceWithFee,
            is EvmError.ExecutionReverted -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                    coinServiceFactory.baseCoinService.platformCoin.code
                )
            }
            is EvmError.CannotEstimateSwap -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_CannotEstimate,
                    coinServiceFactory.baseCoinService.platformCoin.code
                )
            }
            is EvmError.LowerThanBaseGasLimit -> Translator.getString(R.string.EthereumTransaction_Error_LowerThanBaseGasLimit)
            is EvmError.InsufficientLiquidity -> Translator.getString(R.string.EthereumTransaction_Error_InsufficientLiquidity)
            else -> convertedError.message ?: convertedError.javaClass.simpleName
        }

    private fun getAmountColor(type: ValueType): Int {
        return when (type) {
            ValueType.Regular -> R.color.bran
            ValueType.Disabled -> R.color.grey
            ValueType.Outgoing -> R.color.jacob
            ValueType.Incoming -> R.color.remus
        }
    }
}

data class SectionViewItem(
    val viewItems: List<ViewItem>
)

sealed class ViewItem {
    class Subhead(val title: String, val value: String) : ViewItem()
    class Value(
        val title: String,
        val value: String,
        val type: ValueType,
        @ColorRes val color: Int? = null
    ) : ViewItem()

    class Amount(val fiatAmount: String?, val coinAmount: ColoredValue) : ViewItem()
    class Address(val title: String, val valueTitle: String, val value: String) : ViewItem()
    class Input(val value: String) : ViewItem()
    class Warning(val title: String, val description: String, @DrawableRes val icon: Int) :
        ViewItem()
}

enum class ValueType {
    Regular, Disabled, Outgoing, Incoming
}
