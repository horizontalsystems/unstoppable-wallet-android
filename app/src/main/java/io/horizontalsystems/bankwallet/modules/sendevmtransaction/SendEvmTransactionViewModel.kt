package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.oneinch.scaleUp
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapTradeService
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.erc20kit.decorations.ApproveEip20Decoration
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.oneinchkit.decorations.OneInchDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapDecoration
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.math.BigInteger

class SendEvmTransactionViewModel(
    private val service: ISendEvmTransactionService,
    private val coinServiceFactory: EvmCoinServiceFactory,
    private val cautionViewItemFactory: CautionViewItemFactory,
    private val evmLabelManager: EvmLabelManager
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val sendEnabledLiveData = MutableLiveData(false)

    val sendingLiveData = MutableLiveData<Unit>()
    val sendSuccessLiveData = MutableLiveData<ByteArray>()
    val sendFailedLiveData = MutableLiveData<String>()
    val cautionsLiveData = MutableLiveData<List<CautionViewItem>>()

    val viewItemsLiveData = MutableLiveData<List<SectionViewItem>>()
    val transactionTitleLiveData = MutableLiveData<String>()

    init {
        service.stateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.sendStateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
        sync(service.sendState)
    }

    fun send(logger: AppLogger) {
        service.send(logger)
    }

    private fun sync(state: SendEvmTransactionService.State) {
        Log.e("SendEvmTransactionViewModel", "sync: ${state}")
        when (state) {
            is SendEvmTransactionService.State.Ready -> {
                sendEnabledLiveData.postValue(true)
                cautionsLiveData.postValue(cautionViewItemFactory.cautionViewItems(state.warnings, errors = listOf()))
            }
            is SendEvmTransactionService.State.NotReady -> {
                sendEnabledLiveData.postValue(false)
                cautionsLiveData.postValue(cautionViewItemFactory.cautionViewItems(state.warnings, state.errors))
            }
        }

        viewItemsLiveData.postValue(getItems(service.txDataState))
    }

    private fun getItems(dataState: SendEvmTransactionService.TxDataState): List<SectionViewItem> {
        val additionalInfo = dataState.additionalInfo

        if (dataState.decoration != null) {
            val sections = getViewItems(dataState.decoration, dataState.transactionData, additionalInfo)
            if (sections != null) return sections
        }

        if (additionalInfo != null) {
            val oneInchSwapInfo = additionalInfo.oneInchSwapInfo
            if (oneInchSwapInfo != null) {
                return getOneInchViewItems(oneInchSwapInfo)
            }
        }

        if (dataState.transactionData != null) {
            return getUnknownMethodItems(dataState.transactionData, service.methodName(dataState.transactionData.input))
        }

        return listOf()
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
        decoration: TransactionDecoration,
        transactionData: TransactionData?,
        additionalInfo: SendEvmData.AdditionalInfo?
    ): List<SectionViewItem>? =
        when (decoration) {
            is OutgoingDecoration -> getSendBaseCoinItems(
                decoration.to,
                decoration.value,
                additionalInfo?.sendInfo
            )

            is OutgoingEip20Decoration -> getEip20TransferViewItems(
                decoration.to,
                decoration.value,
                decoration.contractAddress,
                transactionData?.nonce,
                additionalInfo?.sendInfo
            )

            is ApproveEip20Decoration -> getEip20ApproveViewItems(
                decoration.spender,
                decoration.value,
                decoration.contractAddress,
                transactionData?.nonce
            )

            is SwapDecoration -> getUniswapViewItems(
                decoration.amountIn,
                decoration.amountOut,
                decoration.tokenIn,
                decoration.tokenOut,
                decoration.recipient,
                additionalInfo?.uniswapInfo
            )

            is OneInchSwapDecoration -> getOneInchSwapViewItems(
                decoration.tokenIn,
                decoration.tokenOut,
                decoration.amountIn,
                decoration.amountOut,
                decoration.recipient,
                additionalInfo?.oneInchSwapInfo
            )

            is OneInchUnoswapDecoration -> getOneInchSwapViewItems(
                decoration.tokenIn,
                decoration.tokenOut,
                decoration.amountIn,
                decoration.amountOut,
                oneInchInfo = additionalInfo?.oneInchSwapInfo
            )

            is OneInchDecoration -> additionalInfo?.oneInchSwapInfo?.let { getOneInchViewItems(it) }

            else -> null
        }

    private fun getUniswapViewItems(
        amountIn: SwapDecoration.Amount,
        amountOut: SwapDecoration.Amount,
        tokenIn: SwapDecoration.Token,
        tokenOut: SwapDecoration.Token,
        recipient: Address?,
        uniswapInfo: SendEvmData.UniswapInfo?
    ): List<SectionViewItem>? {

        val coinServiceIn = getCoinService(tokenIn) ?: return null
        val coinServiceOut = getCoinService(tokenOut) ?: return null

        val sections = mutableListOf<SectionViewItem>()
        val inViewItems = mutableListOf<ViewItem>()
        val outViewItems = mutableListOf<ViewItem>()

        when (amountIn) {
            is SwapDecoration.Amount.Exact -> {
                val amountData = coinServiceIn.amountData(amountIn.value)
                inViewItems.add(getAmount(amountData, ValueType.Regular))
            }

            is SwapDecoration.Amount.Extremum -> {
                val estimatedIn = uniswapInfo?.estimatedIn

                if (estimatedIn != null) {
                    inViewItems.add(estimatedSwapAmount(coinServiceIn.amountData(estimatedIn), ValueType.Incoming))
                }

                inViewItems.add(getMaxAmount(coinServiceIn.amountData(amountIn.value)))
            }
        }

        when (amountOut) {
            is SwapDecoration.Amount.Exact -> {
                val amountData = coinServiceOut.amountData(amountOut.value)
                outViewItems.add(getAmount(amountData, ValueType.Regular))
            }

            is SwapDecoration.Amount.Extremum -> {
                val estimatedOut = uniswapInfo?.estimatedOut

                if (estimatedOut != null) {
                    outViewItems.add(estimatedSwapAmount(coinServiceOut.amountData(estimatedOut), ValueType.Outgoing))
                }

                outViewItems.add(getAmount(coinServiceOut.amountData(amountIn.value), ValueType.Outgoing))
            }
        }

        val otherViewItems = mutableListOf<ViewItem>()
        uniswapInfo?.slippage?.let {
            otherViewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.SwapSettings_SlippageTitle),
                    it,
                    ValueType.Regular
                )
            )
        }
        uniswapInfo?.deadline?.let {
            otherViewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.SwapSettings_DeadlineTitle),
                    it,
                    ValueType.Regular
                )
            )
        }
        if (recipient != null) {
            val addressValue = recipient.eip55
            val addressTitle = uniswapInfo?.recipientDomain ?: evmLabelManager.mapped(addressValue)
            otherViewItems.add(
                ViewItem.Address(
                    Translator.getString(R.string.SwapSettings_RecipientAddressTitle),
                    addressTitle,
                    addressValue
                )
            )
        }
        uniswapInfo?.price?.let {
            otherViewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Swap_Price),
                    it,
                    ValueType.Regular
                )
            )
        }
        uniswapInfo?.priceImpact?.let {
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

        inViewItems.add(ViewItem.Subhead(Translator.getString(R.string.Swap_FromAmountTitle), coinServiceIn.platformCoin.name))
        sections.add(SectionViewItem(inViewItems))

        outViewItems.add(ViewItem.Subhead(Translator.getString(R.string.Swap_ToAmountTitle), coinServiceOut.platformCoin.name))
        sections.add(SectionViewItem(outViewItems))

        if (otherViewItems.isNotEmpty()) {
            sections.add(SectionViewItem(otherViewItems))
        }

        return sections
    }

    private fun getOneInchSwapViewItems(
        tokenIn: OneInchDecoration.Token,
        tokenOut: OneInchDecoration.Token?,
        amountIn: BigInteger,
        amountOut: OneInchDecoration.Amount,
        recipient: Address? = null,
        oneInchInfo: SendEvmData.OneInchSwapInfo?
    ): List<SectionViewItem>? {
        val coinServiceIn = getCoinService(tokenIn) ?: return null
        val coinServiceOut = tokenOut?.let { getCoinService(it) } ?: oneInchInfo?.coinTo?.let { getCoinService(it) } ?: return null

        val sections = mutableListOf<SectionViewItem>()

        sections.add(
            SectionViewItem(
                listOf(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Swap_FromAmountTitle),
                        coinServiceIn.platformCoin.name
                    ),
                    getAmount(coinServiceIn.amountData(amountIn), ValueType.Outgoing)
                )
            )
        )

        val outViewItems: MutableList<ViewItem> = mutableListOf(
            ViewItem.Subhead(
                Translator.getString(R.string.Swap_ToAmountTitle),
                coinServiceOut.platformCoin.name
            )
        )

        if (amountOut is OneInchDecoration.Amount.Extremum) {
            val estimatedOut = oneInchInfo?.estimatedAmountTo

            if (estimatedOut != null) {
                outViewItems.add(estimatedSwapAmount(coinServiceOut.amountData(estimatedOut), ValueType.Incoming))
            }

            outViewItems.add(getGuaranteedAmount(coinServiceOut.amountData(amountOut.value), ValueType.Regular))
        }

        sections.add(
            SectionViewItem(outViewItems)
        )

        val additionalSection = oneInchInfo?.let { additionalSectionViewItem(it, recipient) }
        if (additionalSection != null) {
            sections.add(additionalSection)
        }

        return sections
    }

    private fun additionalSectionViewItem(oneInchSwapInfo: SendEvmData.OneInchSwapInfo, recipient: Address?): SectionViewItem? {
        val viewItems = mutableListOf<ViewItem>()
        getFormattedSlippage(oneInchSwapInfo.slippage)?.let { formattedSlippage ->
            viewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.SwapSettings_SlippageTitle),
                    formattedSlippage,
                    ValueType.Regular
                )
            )
        }

        if (recipient != null) {
            val addressValue = recipient.eip55
            val addressTitle =
                oneInchSwapInfo.recipient?.domain ?: evmLabelManager.mapped(addressValue)
            viewItems.add(
                ViewItem.Address(
                    Translator.getString(R.string.SwapSettings_RecipientAddressTitle),
                    addressTitle,
                    addressValue
                )
            )
        }

        return if (viewItems.isNotEmpty()) {
            SectionViewItem(viewItems)
        } else {
            null
        }
    }

    private fun getOneInchViewItems(
        oneInchSwapInfo: SendEvmData.OneInchSwapInfo
    ): List<SectionViewItem> {
        val coinServiceIn = getCoinService(oneInchSwapInfo.coinFrom)
        val coinServiceOut = getCoinService(oneInchSwapInfo.coinTo)

        val sections = mutableListOf<SectionViewItem>()

        sections.add(
            SectionViewItem(
                listOf(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Swap_FromAmountTitle),
                        coinServiceIn.platformCoin.name
                    ),
                    getAmount(coinServiceIn.amountData(oneInchSwapInfo.amountFrom), ValueType.Outgoing)
                )
            )
        )

        val amountOutMin = oneInchSwapInfo.estimatedAmountTo - oneInchSwapInfo.estimatedAmountTo / BigDecimal("100") * oneInchSwapInfo.slippage

        sections.add(
            SectionViewItem(
                listOf(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Swap_ToAmountTitle),
                        coinServiceOut.platformCoin.name
                    ),
                    estimatedSwapAmount(coinServiceOut.amountData(oneInchSwapInfo.estimatedAmountTo), ValueType.Incoming),
                    getGuaranteedAmount(coinServiceOut.amountData(amountOutMin.scaleUp(oneInchSwapInfo.coinTo.decimals)), ValueType.Regular)
                )
            )
        )

        val recipient = try {
            oneInchSwapInfo.recipient?.let { Address(it.hex) }
        } catch (exception: Exception) {
            null
        }

        val additionalSection = additionalSectionViewItem(oneInchSwapInfo, recipient)
        if (additionalSection != null) {
            sections.add(additionalSection)
        }

        return sections
    }

    private fun getFormattedSlippage(slippage: BigDecimal): String? {
        return if (slippage.compareTo(OneInchSwapSettingsModule.defaultSlippage) == 0) {
            null
        } else {
            "$slippage%"
        }
    }

    private fun getEip20TransferViewItems(
        to: Address,
        value: BigInteger,
        contractAddress: Address,
        nonce: Long?,
        sendInfo: SendEvmData.SendInfo?
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
            sendInfo?.domain ?: evmLabelManager.mapped(addressValue)
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
        val addressTitle = evmLabelManager.mapped(addressValue)

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

    private fun getUnknownMethodItems(transactionData: TransactionData, methodName: String?): List<SectionViewItem> {
        val toValue = transactionData.to.eip55

        val viewItems = mutableListOf(
            getAmount(
                coinServiceFactory.baseCoinService.amountData(transactionData.value),
                ValueType.Outgoing
            ),
            ViewItem.Address(
                Translator.getString(R.string.Send_Confirmation_To),
                evmLabelManager.mapped(toValue),
                toValue
            )
        )

        if (transactionData.nonce != null) {
            viewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Send_Confirmation_Nonce),
                    "${transactionData.nonce}",
                    ValueType.Regular
                ),
            )
        }

        methodName?.let {
            viewItems.add(ViewItem.Value(Translator.getString(R.string.Send_Confirmation_Method), it, ValueType.Regular))
        }

        viewItems.add(ViewItem.Input(transactionData.input.toHexString()))

        // TODO: Add dAppName from additionalInfo

        return listOf(SectionViewItem(viewItems))
    }

    private fun getSendBaseCoinItems(to: Address, value: BigInteger, sendInfo: SendEvmData.SendInfo?): List<SectionViewItem> {
        val toValue = to.eip55
        val baseCoinService = coinServiceFactory.baseCoinService

        return listOf(SectionViewItem(
            listOf(
                ViewItem.Subhead(Translator.getString(R.string.Send_Confirmation_YouSend), baseCoinService.platformCoin.coin.name),
                getAmount(baseCoinService.amountData(value), ValueType.Outgoing),
                ViewItem.Address(Translator.getString(R.string.Send_Confirmation_To), sendInfo?.domain ?: evmLabelManager.mapped(toValue), toValue)
            )
        ))
    }

    private fun getCoinService(token: SwapDecoration.Token) = when (token) {
        SwapDecoration.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is SwapDecoration.Token.Eip20Coin -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getCoinService(token: OneInchDecoration.Token) = when (token) {
        OneInchDecoration.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is OneInchDecoration.Token.Eip20Coin -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getCoinService(coin: PlatformCoin) =
        coinServiceFactory.getCoinService(coin)

    private fun getAmount(amountData: SendModule.AmountData, valueType: ValueType) =
        ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            amountData.primary.getFormatted(),
            valueType
        )

    private fun estimatedSwapAmount(amountData: SendModule.AmountData, type: ValueType): ViewItem {
        val title = amountData.secondary?.getFormatted() ?: "n/a"
        val value = amountData.primary.getFormatted()

        return ViewItem.Value(title, "$value ${Translator.getString(R.string.Swap_AmountEstimated)}", type)
    }

    private fun getGuaranteedAmount(amountData: SendModule.AmountData, type: ValueType = ValueType.Incoming): ViewItem.Amount {
        return ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            "${amountData.primary.getFormatted()} ${Translator.getString(R.string.Swap_AmountMin)}",
            type
        )
    }

    private fun getMaxAmount(amountData: SendModule.AmountData): ViewItem.Amount {
        return ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            "${amountData.primary.getFormatted()} ${Translator.getString(R.string.Swap_AmountMax)}",
            ValueType.Regular
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

    class Amount(val fiatAmount: String?, val coinAmount: String, val type: ValueType) : ViewItem()
    class Address(val title: String, val valueTitle: String, val value: String) : ViewItem()
    class Input(val value: String) : ViewItem()
    class Warning(val title: String, val description: String, @DrawableRes val icon: Int) :
        ViewItem()
}

enum class ValueType {
    Regular, Disabled, Outgoing, Incoming
}
