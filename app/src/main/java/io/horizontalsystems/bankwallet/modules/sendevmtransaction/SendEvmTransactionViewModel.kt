package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData.AdditionalInfo
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.PriceImpactLevel
import io.horizontalsystems.bankwallet.modules.swap.scaleUp
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.erc20kit.decorations.ApproveEip20Decoration
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.nftkit.decorations.OutgoingEip1155Decoration
import io.horizontalsystems.nftkit.decorations.OutgoingEip721Decoration
import io.horizontalsystems.oneinchkit.decorations.OneInchDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapDecoration
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

class SendEvmTransactionViewModel(
    val service: ISendEvmTransactionService,
    private val coinServiceFactory: EvmCoinServiceFactory,
    private val cautionViewItemFactory: CautionViewItemFactory,
    private val contactsRepo: ContactsRepository,
    private val blockchainType: BlockchainType
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val sendEnabledLiveData = MutableLiveData(false)

    val sendingLiveData = MutableLiveData<Unit>()
    val sendSuccessLiveData = MutableLiveData<ByteArray>()
    val sendFailedLiveData = MutableLiveData<String>()
    val cautionsLiveData = MutableLiveData<List<CautionViewItem>>()

    val viewItemsLiveData = MutableLiveData<List<SectionViewItem>>()

    init {
        service.stateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.sendStateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
        sync(service.sendState)

        viewModelScope.launch {
            contactsRepo.contactsFlow.collect {
                sync(service.state)
            }
        }
        viewModelScope.launch {
            service.start()
        }
    }

    fun send(logger: AppLogger) {
        service.send(logger)
    }

    override fun onCleared() {
        disposable.clear()
        service.clear()
    }

    @Synchronized
    private fun sync(state: SendEvmTransactionService.State) {
        when (state) {
            is SendEvmTransactionService.State.Ready -> {
                val sendEnabled = service.txDataState.additionalInfo?.uniswapInfo?.priceImpact?.level != PriceImpactLevel.Forbidden
                sendEnabledLiveData.postValue(sendEnabled)
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

        var sections: List<SectionViewItem> = dataState.decoration?.let {
            getViewItems(it, additionalInfo)
        } ?: listOf()

        if (sections.isEmpty()) {
            if (additionalInfo?.oneInchSwapInfo != null) {
                sections = getOneInchViewItems(additionalInfo.oneInchSwapInfo!!)
            } else if (dataState.transactionData != null) {
                sections = getUnknownMethodItems(
                    dataState.transactionData,
                    service.methodName(dataState.transactionData.input)
                )
            }
        }

        additionalInfo?.walletConnectInfo?.let {
            sections = sections + getWalletConnectSectionView(it)
        }

        return sections
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
        additionalInfo: AdditionalInfo?
    ): List<SectionViewItem>? =
        when (decoration) {
            is OutgoingDecoration -> getSendBaseCoinItems(
                decoration.to,
                decoration.value
            )

            is OutgoingEip20Decoration -> getEip20TransferViewItems(
                decoration.to,
                decoration.value,
                decoration.contractAddress
            )

            is ApproveEip20Decoration -> getEip20ApproveViewItems(
                decoration.spender,
                decoration.value,
                decoration.contractAddress
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

            is OutgoingEip721Decoration -> getNftTransferItems(
                decoration.to,
                BigInteger.ONE,
                additionalInfo?.sendInfo,
                decoration.tokenId,
            )

            is OutgoingEip1155Decoration -> getNftTransferItems(
                decoration.to,
                decoration.value,
                additionalInfo?.sendInfo,
                decoration.tokenId,
            )

            else -> null
        }

    private fun getNftTransferItems(
        recipient: Address,
        value: BigInteger,
        sendInfo: SendEvmData.SendInfo?,
        tokenId: BigInteger
    ): List<SectionViewItem> {

        val sections = mutableListOf<SectionViewItem>()
        val addressValue = recipient.eip55

        val viewItems = buildList {
            add(
                ViewItem.Subhead(
                    Translator.getString(R.string.Send_Confirmation_YouSend),
                    sendInfo?.nftShortMeta?.nftName ?: tokenId.toString(),
                    R.drawable.ic_arrow_up_right_12
                )
            )

            add(
                getNftAmount(
                    value,
                    sendInfo?.nftShortMeta?.previewImageUrl
                )
            )

            val contact = getContact(addressValue)

            add(
                ViewItem.Address(
                    Translator.getString(R.string.Send_Confirmation_To),
                    addressValue,
                    contact == null,
                    blockchainType
                )
            )

            contact?.let {
                add(
                    ViewItem.ContactItem(it)
                )
            }
        }

        sections.add(SectionViewItem(viewItems))

        return sections
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
            val contact = getContact(addressValue)
            otherViewItems.add(
                ViewItem.Address(
                    Translator.getString(R.string.SwapSettings_RecipientAddressTitle),
                    addressValue,
                    contact == null,
                    blockchainType
                )
            )
            contact?.let {
                otherViewItems.add(
                    ViewItem.ContactItem(it)
                )
            }
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
            val type = when (it.level) {
                PriceImpactLevel.Normal -> ValueType.Warning
                PriceImpactLevel.Warning,
                PriceImpactLevel.Forbidden -> ValueType.Forbidden
                else -> ValueType.Regular
            }

            otherViewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Swap_PriceImpact),
                    it.value,
                    type
                )
            )
        }

        when (amountIn) {
            is SwapDecoration.Amount.Exact -> { // you pay exact
                val amountData = coinServiceIn.amountData(amountIn.value)
                inViewItems.add(getAmount(amountData, ValueType.Outgoing, coinServiceIn.token))
            }

            is SwapDecoration.Amount.Extremum -> { // you pay estimated
                val maxAmountData = coinServiceIn.amountData(amountIn.value)
                if (uniswapInfo?.estimatedIn != null) {
                    val estimatedAmount = getEstimatedSwapAmount(coinServiceIn.amountData(uniswapInfo.estimatedIn))
                    inViewItems.add(
                        ViewItem.Amount(
                            estimatedAmount.fiatAmount,
                            estimatedAmount.coinAmount,
                            ValueType.Outgoing,
                            coinServiceIn.token
                        )
                    )
                    otherViewItems.add(
                        ViewItem.ValueMulti(
                            Translator.getString(R.string.SwapInfo_MaxSpendTitle),
                            maxAmountData.primary.getFormatted(),
                            maxAmountData.secondary?.getFormatted() ?: "---",
                            ValueType.Regular
                        )
                    )

                } else {
                    inViewItems.add(getMaxAmount(maxAmountData, coinServiceIn.token))
                }
            }
        }

        when (amountOut) {
            is SwapDecoration.Amount.Exact -> { // you get exact
                val amountData = coinServiceOut.amountData(amountOut.value)
                outViewItems.add(
                    getAmount(amountData, ValueType.Incoming, coinServiceOut.token)
                )
            }

            is SwapDecoration.Amount.Extremum -> { // you get estimated
                val guaranteedAmountData = coinServiceOut.amountData(amountOut.value)
                if (uniswapInfo?.estimatedOut != null) {
                    getEstimatedSwapAmount(coinServiceOut.amountData(uniswapInfo.estimatedOut)).let {
                        outViewItems.add(
                            ViewItem.Amount(it.fiatAmount, it.coinAmount, ValueType.Incoming, coinServiceOut.token)
                        )
                    }
                    otherViewItems.add(
                        ViewItem.ValueMulti(
                            Translator.getString(R.string.SwapInfo_GuaranteedAmountTitle),
                            guaranteedAmountData.primary.getFormatted(),
                            guaranteedAmountData.secondary?.getFormatted() ?: "---",
                            ValueType.Regular
                        )
                    )
                } else {
                    outViewItems.add(getGuaranteedAmount(guaranteedAmountData, coinServiceOut.token))
                }
            }
        }

        inViewItems.add(
            0, ViewItem.Subhead(
                Translator.getString(R.string.Swap_FromAmountTitle),
                coinServiceIn.token.coin.name,
                R.drawable.ic_arrow_up_right_12
            )
        )
        sections.add(SectionViewItem(inViewItems))

        outViewItems.add(
            0, ViewItem.Subhead(
                Translator.getString(R.string.Swap_ToAmountTitle),
                coinServiceOut.token.coin.name,
                R.drawable.ic_arrow_down_left_12
            )
        )
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
        val coinServiceOut = tokenOut?.let { getCoinService(it) } ?: oneInchInfo?.tokenTo?.let { getCoinService(it) } ?: return null

        val sections = mutableListOf<SectionViewItem>()

        sections.add(
            SectionViewItem(
                listOf(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Swap_FromAmountTitle),
                        coinServiceIn.token.coin.name,
                        R.drawable.ic_arrow_up_right_12
                    ),
                    getAmount(
                        coinServiceIn.amountData(amountIn),
                        ValueType.Outgoing,
                        coinServiceIn.token
                    )
                )
            )
        )

        val outViewItems: MutableList<ViewItem> = mutableListOf(
            ViewItem.Subhead(
                Translator.getString(R.string.Swap_ToAmountTitle),
                coinServiceOut.token.coin.name,
                R.drawable.ic_arrow_down_left_12
            )
        )

        var guaranteed: ViewItem? = null
        if (amountOut is OneInchDecoration.Amount.Extremum) {
            val guaranteedAmountData = coinServiceOut.amountData(amountOut.value)

            if (oneInchInfo?.estimatedAmountTo != null) {
                getEstimatedSwapAmount(coinServiceOut.amountData(oneInchInfo.estimatedAmountTo)).let {
                    outViewItems.add(
                        ViewItem.Amount(it.fiatAmount, it.coinAmount, ValueType.Incoming, coinServiceOut.token)
                    )
                }
                guaranteed = ViewItem.ValueMulti(
                    Translator.getString(R.string.SwapInfo_GuaranteedAmountTitle),
                    guaranteedAmountData.primary.getFormatted(),
                    guaranteedAmountData.secondary?.getFormatted() ?: "---",
                    ValueType.Regular
                )
            } else {
                outViewItems.add(getGuaranteedAmount(guaranteedAmountData, coinServiceOut.token))
            }
        }
        sections.add(SectionViewItem(outViewItems))

        val additionalViewItems = mutableListOf<ViewItem>()
        oneInchInfo?.let { additionalViewItems.addAll(additionalViewItems(it, recipient)) }
        guaranteed?.let { additionalViewItems.add(it) }
        if (additionalViewItems.isNotEmpty()) {
            sections.add(SectionViewItem(additionalViewItems))
        }

        return sections
    }

    private fun additionalViewItems(oneInchSwapInfo: SendEvmData.OneInchSwapInfo, recipient: Address?): List<ViewItem> {
        val viewItems = mutableListOf<ViewItem>()
        oneInchSwapInfo.price?.let {
            viewItems.add(
                ViewItem.Value(
                    Translator.getString(R.string.Swap_Price),
                    it,
                    ValueType.Regular
                )
            )
        }
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
            val contact = getContact(addressValue)
            viewItems.add(
                ViewItem.Address(
                    Translator.getString(R.string.SwapSettings_RecipientAddressTitle),
                    addressValue,
                    contact == null,
                    blockchainType
                )
            )
            contact?.let {
                viewItems.add(
                    ViewItem.ContactItem(it)
                )
            }
        }
        return viewItems
    }

    private fun getOneInchViewItems(
        oneInchSwapInfo: SendEvmData.OneInchSwapInfo
    ): List<SectionViewItem> {
        val coinServiceIn = getCoinService(oneInchSwapInfo.tokenFrom)
        val coinServiceOut = getCoinService(oneInchSwapInfo.tokenTo)

        val sections = mutableListOf<SectionViewItem>()

        sections.add(
            SectionViewItem(
                listOf(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Swap_FromAmountTitle),
                        coinServiceIn.token.coin.name,
                        R.drawable.ic_arrow_up_right_12
                    ),
                    getAmount(
                        coinServiceIn.amountData(oneInchSwapInfo.amountFrom),
                        ValueType.Outgoing,
                        coinServiceIn.token
                    )
                )
            )
        )

        val estimated = getEstimatedSwapAmount(coinServiceOut.amountData(oneInchSwapInfo.estimatedAmountTo)).let {
            ViewItem.Amount(it.fiatAmount, it.coinAmount, ValueType.Incoming, coinServiceOut.token)
        }

        sections.add(
            SectionViewItem(
                listOf(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Swap_ToAmountTitle),
                        coinServiceOut.token.coin.name,
                        R.drawable.ic_arrow_down_left_12
                    ),
                    estimated
                )
            )
        )

        val amountOutMin = oneInchSwapInfo.estimatedAmountTo - oneInchSwapInfo.estimatedAmountTo / BigDecimal("100") * oneInchSwapInfo.slippage
        val guaranteedAmountData = coinServiceOut.amountData(amountOutMin.scaleUp(oneInchSwapInfo.tokenTo.decimals))

        val guaranteed = ViewItem.ValueMulti(
            Translator.getString(R.string.SwapInfo_GuaranteedAmountTitle),
            guaranteedAmountData.primary.getFormatted(),
            guaranteedAmountData.secondary?.getFormatted() ?: "n/a",
            ValueType.Regular
        )

        val recipient = try {
            oneInchSwapInfo.recipient?.let { Address(it.hex) }
        } catch (exception: Exception) {
            null
        }

        val additionalViewItems = additionalViewItems(oneInchSwapInfo, recipient) + guaranteed
        sections.add(SectionViewItem(additionalViewItems))

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
        contractAddress: Address
    ): List<SectionViewItem>? {
        val coinService = coinServiceFactory.getCoinService(contractAddress) ?: return null

        val viewItems = mutableListOf(
            ViewItem.Subhead(
                Translator.getString(R.string.Send_Confirmation_YouSend),
                coinService.token.coin.name,
                R.drawable.ic_arrow_up_right_12
            ),
            getAmount(
                coinService.amountData(value),
                ValueType.Outgoing,
                coinService.token
            )
        )
        val addressValue = to.eip55
        val contact = getContact(addressValue)
        viewItems.add(
            ViewItem.Address(
                Translator.getString(R.string.Send_Confirmation_To),
                addressValue,
                contact == null,
                blockchainType
            )
        )
        contact?.let {
            viewItems.add(
                ViewItem.ContactItem(it)
            )
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getContact(addressValue: String): Contact? {
        return contactsRepo.getContactsFiltered(blockchainType, addressQuery = addressValue).firstOrNull()
    }

    private fun getEip20ApproveViewItems(
        spender: Address,
        value: BigInteger,
        contractAddress: Address
    ): List<SectionViewItem>? {
        val coinService = coinServiceFactory.getCoinService(contractAddress) ?: return null

        val viewItems = buildList {
            if (value.compareTo(BigInteger.ZERO) == 0) {
                add(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Approve_YouRevoke),
                        coinService.token.coin.name,
                    )
                )
                add(ViewItem.TokenItem(coinService.token))
            } else {
                add(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Approve_YouApprove),
                        coinService.token.coin.name,
                    )
                )
                add(
                    getAmount(
                        coinService.amountData(value),
                        ValueType.Regular,
                        coinService.token
                    )
                )
            }

            val addressValue = spender.eip55
            val contact = getContact(addressValue)
            add(
                ViewItem.Address(
                    Translator.getString(R.string.Approve_Spender),
                    addressValue,
                    contact == null,
                    blockchainType
                )
            )
            contact?.let {
                add(
                    ViewItem.ContactItem(it)
                )
            }
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getWalletConnectSectionView(
        info: SendEvmData.WalletConnectInfo
    ) = SectionViewItem(
        buildList {
            info.dAppName?.let {
                add(
                    ViewItem.Value(
                        Translator.getString(R.string.WalletConnect_SignMessageRequest_dApp),
                        it,
                        ValueType.Regular
                    )
                )
            }
            info.chain?.let {
                add(
                    ViewItem.Value(
                        it.name,
                        it.address,
                        ValueType.Regular
                    )
                )
            }
        }
    )

    private fun getUnknownMethodItems(
        transactionData: TransactionData,
        methodName: String?,
    ): List<SectionViewItem> {
        val viewItems = buildList {
            add(
                getAmount(
                    coinServiceFactory.baseCoinService.amountData(transactionData.value),
                    ValueType.Outgoing,
                    coinServiceFactory.baseCoinService.token
                )
            )
            val toValue = transactionData.to.eip55
            val contact = getContact(toValue)
            add(
                ViewItem.Address(
                    Translator.getString(R.string.Send_Confirmation_To),
                    toValue,
                    contact == null,
                    blockchainType
                )
            )
            contact?.let {
                add(
                    ViewItem.ContactItem(it)
                )
            }

            methodName?.let {
                add(ViewItem.Value(Translator.getString(R.string.Send_Confirmation_Method), it, ValueType.Regular))
            }

            add(ViewItem.Input(transactionData.input.toHexString()))
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getSendBaseCoinItems(to: Address, value: BigInteger): List<SectionViewItem> {
        val toValue = to.eip55
        val baseCoinService = coinServiceFactory.baseCoinService

        val viewItems = buildList {
            add(
                ViewItem.Subhead(
                    Translator.getString(R.string.Send_Confirmation_YouSend),
                    baseCoinService.token.coin.name,
                    R.drawable.ic_arrow_up_right_12
                )
            )
            add(
                getAmount(
                    baseCoinService.amountData(value),
                    ValueType.Outgoing,
                    baseCoinService.token
                )
            )
            val contact = getContact(toValue)
            add(
                ViewItem.Address(
                    Translator.getString(R.string.Send_Confirmation_To),
                    toValue,
                    contact == null,
                    blockchainType
                )
            )
            contact?.let {
                add(
                    ViewItem.ContactItem(it)
                )
            }
        }

        return listOf(
            SectionViewItem(
                viewItems
            )
        )
    }

    private fun getCoinService(token: SwapDecoration.Token) = when (token) {
        SwapDecoration.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is SwapDecoration.Token.Eip20Coin -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getCoinService(token: OneInchDecoration.Token) = when (token) {
        OneInchDecoration.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is OneInchDecoration.Token.Eip20Coin -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getCoinService(token: Token) =
        coinServiceFactory.getCoinService(token)

    private fun getNftAmount(value: BigInteger, previewImageUrl: String?): ViewItem.NftAmount =
        ViewItem.NftAmount(previewImageUrl, "$value NFT", ValueType.Regular)

    private fun getAmount(amountData: SendModule.AmountData, valueType: ValueType, token: Token) =
        ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            amountData.primary.getFormatted(),
            valueType,
            token
        )

    private fun getEstimatedSwapAmount(amountData: SendModule.AmountData) = AmountValues(
        "${amountData.primary.getFormatted()} ${Translator.getString(R.string.Swap_AmountEstimated)}",
        amountData.secondary?.getFormatted() ?: "n/a"
    )

    private fun getGuaranteedAmount(amountData: SendModule.AmountData, token: Token) =
        ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            "${amountData.primary.getFormatted()} ${Translator.getString(R.string.Swap_AmountMin)}",
            ValueType.Incoming,
            token
        )

    private fun getMaxAmount(amountData: SendModule.AmountData, token: Token) =
        ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            "${amountData.primary.getFormatted()} ${Translator.getString(R.string.Swap_AmountMax)}",
            ValueType.Outgoing,
            token
        )

    private fun convertError(error: Throwable) =
        when (val convertedError = error.convertedError) {
            is SendEvmTransactionService.TransactionError.InsufficientBalance -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_InsufficientBalance,
                    coinServiceFactory.baseCoinService.coinValue(convertedError.requiredBalance)
                        .getFormattedFull()
                )
            }
            is EvmError.InsufficientBalanceWithFee -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                    coinServiceFactory.baseCoinService.token.coin.code
                )
            }
            is EvmError.ExecutionReverted -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_ExecutionReverted,
                    convertedError.message ?: ""
                )
            }
            is EvmError.CannotEstimateSwap -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_CannotEstimate,
                    coinServiceFactory.baseCoinService.token.coin.code
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
    class Subhead(val title: String, val value: String, val iconRes: Int? = null) : ViewItem()
    class Value(
        val title: String,
        val value: String,
        val type: ValueType,
    ) : ViewItem()

    class ValueMulti(
        val title: String,
        val primaryValue: String,
        val secondaryValue: String,
        val type: ValueType,
    ) : ViewItem()

    class AmountMulti(
        val amounts: List<AmountValues>,
        val type: ValueType,
        val token: Token
    ) : ViewItem()

    class Amount(
        val fiatAmount: String?,
        val coinAmount: String,
        val type: ValueType,
        val token: Token
    ) : ViewItem()

    class NftAmount(
        val iconUrl: String?,
        val amount: String,
        val type: ValueType,
    ) : ViewItem()

    class Address(val title: String, val value: String, val showAdd: Boolean, val blockchainType: BlockchainType) : ViewItem()
    class Input(val value: String) : ViewItem()
    class TokenItem(val token: Token) : ViewItem()
    class ContactItem(val contact: Contact) : ViewItem()
}

data class AmountValues(val coinAmount: String, val fiatAmount: String?)

enum class ValueType {
    Regular, Disabled, Outgoing, Incoming, Warning, Forbidden
}
