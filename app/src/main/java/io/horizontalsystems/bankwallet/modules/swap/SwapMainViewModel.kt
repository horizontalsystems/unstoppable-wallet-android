package io.horizontalsystems.bankwallet.modules.swap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService.AmountType
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.evmfee.GasDataError
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.AmountTypeItem
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ExactType
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.PriceImpactLevel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ProviderTradeData
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ProviderViewItem
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapActionState
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapButtons
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapData
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapResultState
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceState
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchKitHelper
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchTradeService
import io.horizontalsystems.bankwallet.modules.swap.uniswap.IUniswapTradeService
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapV2TradeService
import io.horizontalsystems.bankwallet.modules.swap.uniswapv3.UniswapV3TradeService
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.horizontalsystems.uniswapkit.models.DexType
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class SwapMainViewModel(
    private val formatter: SwapViewItemHelper,
    val service: SwapMainService,
    private val switchService: AmountTypeSwitchService,
    private val fromTokenService: SwapTokenService,
    private val toTokenService: SwapTokenService,
    private val allowanceService: SwapAllowanceService,
    private val pendingAllowanceService: SwapPendingAllowanceService,
    private val errorShareService: ErrorShareService,
    private val timerService: TimerService,
    private val currencyManager: CurrencyManager,
    private val adapterManager: IAdapterManager,
) : ViewModel() {
    private val maxValidDecimals = 8

    private val disposable = CompositeDisposable()
    private val tradeDisposable = CompositeDisposable()

    private val dex: SwapMainModule.Dex
        get() = service.dex

    val revokeEvmData: SendEvmData?
        get() = allowanceService.revokeEvmData()

    private val providerViewItems: List<ProviderViewItem>
        get() = service.availableProviders.map {
            ProviderViewItem(
                provider = it,
                selected = it == dex.provider
            )
        }

    private var exactType: ExactType = ExactType.ExactFrom
    private var balanceFrom: BigDecimal? = null
    private var availableBalance: String? = null
    private var amountTypeSelect = buildAmountTypeSelect()

    private var amountTypeSelectEnabled = switchService.toggleAvailable

    private var tokenFromState = fromTokenService.state
    private var tokenToState = toTokenService.state

    private val evmKit: EthereumKit by lazy { App.evmBlockchainManager.getEvmKitManager(dex.blockchainType).evmKitWrapper?.evmKit!! }
    private val oneIncKitHelper by lazy { OneInchKitHelper(evmKit) }
    private val uniswapKit by lazy { UniswapKit.getInstance(evmKit) }
    private val uniswapV3Kit by lazy { UniswapV3Kit.getInstance(evmKit, DexType.Uniswap) }
    private val pancakeSwapV3Kit by lazy { UniswapV3Kit.getInstance(evmKit, DexType.PancakeSwap) }
    private var tradeService: SwapMainModule.ISwapTradeService = getTradeService(dex.provider)
    private var tradeView: SwapMainModule.TradeViewX? = null
    private var tradePriceExpiration: Float? = null

    private var amountFrom: BigDecimal? = null
    private var amountTo: BigDecimal? = null
    private var allErrors: List<Throwable> = emptyList()
    private var error: String? = null
    private var hasNonZeroBalance: Boolean? = null
    private var swapData: SwapData? = null
    private var buttons = SwapButtons(SwapActionState.Hidden, SwapActionState.Hidden, SwapActionState.Hidden)
    private var refocusKey = UUID.randomUUID().leastSignificantBits

    var swapState by mutableStateOf(
        SwapMainModule.SwapState(
            dex = dex,
            providerViewItems = providerViewItems,
            availableBalance = availableBalance,
            amountTypeSelect = amountTypeSelect,
            amountTypeSelectEnabled = amountTypeSelectEnabled,
            fromState = tokenFromState,
            toState = tokenToState,
            tradeView = tradeView,
            tradePriceExpiration = tradePriceExpiration,
            error = error,
            buttons = buttons,
            hasNonZeroBalance = hasNonZeroBalance,
            recipient = tradeService.recipient,
            slippage = tradeService.slippage,
            ttl = tradeService.ttl,
            refocusKey = refocusKey
        )
    )
        private set

    val approveData: SwapMainModule.ApproveData?
        get() = balanceFrom?.let { amount ->
            allowanceService.approveData(dex, amount)
        }

    val proceedParams: SwapData?
        get() = swapData

    init {
        fromTokenService.stateFlow.collectWith(viewModelScope) {
            tokenFromState = it
            syncUiState()
        }

        toTokenService.stateFlow.collectWith(viewModelScope) {
            tokenToState = it
            syncUiState()
        }

        service.providerUpdatedFlow.collectWith(viewModelScope) { provider ->
            allowanceService.set(getSpenderAddress(provider))
            tradeService = getTradeService(provider)
            toTokenService.setAmountEnabled(provider.supportsExactOut)
            syncUiState()
        }

        switchService.amountTypeObservable
            .subscribeIO {
                amountTypeSelect = buildAmountTypeSelect()
                syncUiState()
            }.let {
                disposable.add(it)
            }

        switchService.toggleAvailableObservable
            .subscribeIO {
                amountTypeSelectEnabled = it
                syncUiState()
            }.let {
                disposable.add(it)
            }

        allowanceService.stateFlow
            .collectWith(viewModelScope) {
                syncSwapDataState()
            }

        pendingAllowanceService.stateObservable
            .subscribeIO {
                syncSwapDataState()
            }.let {
                disposable.add(it)
            }


        allowanceService.set(getSpenderAddress(dex.provider))
        fromTokenService.token?.let {
            allowanceService.set(it)
            pendingAllowanceService.set(it)
        }

        toTokenService.setAmountEnabled(dex.provider.supportsExactOut)
        fromTokenService.start()
        toTokenService.start()
        setBalance()
        subscribeToTradeService()
        timerService.start()
        allowanceService.start()
        syncButtonsState()
    }

    private fun getTradeService(provider: ISwapProvider): SwapMainModule.ISwapTradeService = when (provider) {
        SwapMainModule.OneInchProvider -> OneInchTradeService(oneIncKitHelper)
        SwapMainModule.UniswapV3Provider -> UniswapV3TradeService(uniswapV3Kit)
        SwapMainModule.PancakeSwapV3Provider -> UniswapV3TradeService(pancakeSwapV3Kit)
        else -> UniswapV2TradeService(uniswapKit)
    }

    private fun getSpenderAddress(provider: ISwapProvider) = when (provider) {
        SwapMainModule.OneInchProvider -> oneIncKitHelper.smartContractAddress
        SwapMainModule.UniswapV3Provider -> uniswapV3Kit.routerAddress
        SwapMainModule.PancakeSwapV3Provider -> pancakeSwapV3Kit.routerAddress
        else -> uniswapKit.routerAddress
    }

    private fun syncUiState() {
        swapState = SwapMainModule.SwapState(
            dex = dex,
            providerViewItems = providerViewItems,
            availableBalance = availableBalance,
            amountTypeSelect = amountTypeSelect,
            amountTypeSelectEnabled = amountTypeSelectEnabled,
            fromState = tokenFromState,
            toState = tokenToState,
            tradeView = tradeView,
            tradePriceExpiration = tradePriceExpiration,
            error = error,
            buttons = buttons,
            hasNonZeroBalance = hasNonZeroBalance,
            recipient = tradeService.recipient,
            slippage = tradeService.slippage,
            ttl = tradeService.ttl,
            refocusKey = refocusKey
        )
    }

    private fun subscribeToTradeService() {
        tradeService.stateFlow.collectWith(viewModelScope) {
            syncSwapDataState()
        }

        timerService.reSyncFlow.collectWith(viewModelScope) {
            resyncSwapData()
        }

        timerService.timeoutProgressFlow.collectWith(viewModelScope) {
            tradePriceExpiration = it
            syncUiState()
        }

    }

    private fun syncSwapDataState() {
        val errors = mutableListOf<Throwable>()
        swapData = null
        setLoading(tradeService.state)

        when (val state = tradeService.state) {
            SwapResultState.Loading -> {
                tradeView = tradeView?.copy(expired = true)
            }

            is SwapResultState.NotReady -> {
                tradeView = null
                errors.addAll(state.errors)
            }

            is SwapResultState.Ready -> {
                swapData = state.swapData
                when (val swapData = state.swapData) {
                    is SwapData.OneInchData -> {
                        tradeView = oneInchTradeViewItem(swapData.data, fromTokenService.token, toTokenService.token)
                        amountTo = swapData.data.amountTo
                        toTokenService.onChangeAmount(swapData.data.amountTo.toString(), true)
                    }

                    is SwapData.UniswapData -> {
                        tradeView = uniswapTradeViewItem(swapData, fromTokenService.token, toTokenService.token)
                        if (exactType == ExactType.ExactFrom) {
                            amountTo = swapData.data.amountOut
                            toTokenService.onChangeAmount(swapData.data.amountOut.toString(), true)
                        } else {
                            amountFrom = swapData.data.amountIn
                            fromTokenService.onChangeAmount(swapData.data.amountIn.toString(), true)
                        }
                    }
                }
            }
        }

        when (val state = allowanceService.state) {
            SwapAllowanceService.State.Loading -> {}

            is SwapAllowanceService.State.Ready -> {
                amountFrom?.let { amountFrom ->
                    if (amountFrom > state.allowance.value) {
                        if (revokeRequired()) {
                            errors.add(SwapError.RevokeAllowanceRequired)
                        } else {
                            errors.add(SwapError.InsufficientAllowance)
                        }
                    }
                }
            }

            is SwapAllowanceService.State.NotReady -> {
                errors.add(state.error)
            }

            null -> {}
        }

        amountFrom?.let { amountFrom ->
            val balance = balanceFrom
            if (balance == null || balance < amountFrom) {
                errors.add(SwapError.InsufficientBalanceFrom)
            }
        }

        if (pendingAllowanceService.state.loading()) {
            tradeView = tradeView?.copy(expired = true)
        }

        allErrors = errors
        errorShareService.updateErrors(errors)

        val filtered = allErrors.filter { it !is GasDataError && it !is SwapError }
        error = filtered.firstOrNull()?.let { convert(it) }

        syncUiState()
        syncButtonsState()
    }

    private fun setLoading(state: SwapResultState) {
        val loading = state == SwapResultState.Loading
        fromTokenService.setLoading(loading)
        toTokenService.setLoading(loading)
    }

    private fun resyncSwapData() {
        tradeService.fetchSwapData(fromTokenService.token, toTokenService.token, amountFrom, amountTo, exactType)
    }

    private fun syncButtonsState() {
        val revokeAction = getRevokeActionState()
        val approveAction = getApproveActionState(revokeAction)
        val proceedAction = getProceedActionState(revokeAction)
        buttons = SwapButtons(revokeAction, approveAction, proceedAction)
        syncUiState()
    }

    private fun getProceedActionState(revokeAction: SwapActionState) = when {
        balanceFrom == null -> {
            SwapActionState.Disabled(Translator.getString(R.string.Swap_ErrorBalanceNotAvailable))
        }

        revokeAction !is SwapActionState.Hidden -> {
            SwapActionState.Hidden
        }

        tradeService.state is SwapResultState.Ready -> {
            when {
                allErrors.any { it == SwapError.InsufficientBalanceFrom } -> {
                    SwapActionState.Disabled(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
                }

                pendingAllowanceService.state == SwapPendingAllowanceState.Approving -> {
                    SwapActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
                }

                else -> {
                    if (allErrors.isEmpty()) {
                        SwapActionState.Enabled(Translator.getString(R.string.Swap_Proceed))
                    } else {
                        SwapActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
                    }
                }
            }
        }

        else -> {
            SwapActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
        }
    }

    private fun getRevokeActionState() = when {
        pendingAllowanceService.state == SwapPendingAllowanceState.Revoking -> {
            SwapActionState.Disabled(Translator.getString(R.string.Swap_Revoking))
        }

        allErrors.isNotEmpty() && allErrors.all { it == SwapError.RevokeAllowanceRequired } -> {
            SwapActionState.Enabled(Translator.getString(R.string.Swap_Revoke))
        }

        else -> {
            SwapActionState.Hidden
        }
    }

    private fun getApproveActionState(revokeAction: SwapActionState) = when {
        revokeAction !is SwapActionState.Hidden -> {
            SwapActionState.Hidden
        }

        pendingAllowanceService.state == SwapPendingAllowanceState.Approving -> {
            SwapActionState.Disabled(Translator.getString(R.string.Swap_Approving), loading = true)
        }

        tradeService.state is SwapResultState.NotReady || allErrors.any { it == SwapError.InsufficientBalanceFrom } -> {
            SwapActionState.Hidden
        }

        allErrors.any { it == SwapError.InsufficientAllowance } -> {
            SwapActionState.Enabled(Translator.getString(R.string.Swap_Approve))
        }

        pendingAllowanceService.state == SwapPendingAllowanceState.Approved -> {
            SwapActionState.Disabled(Translator.getString(R.string.Swap_Approve))
        }

        else -> {
            SwapActionState.Hidden
        }
    }

    private fun buildAmountTypeSelect() = Select(
        selected = switchService.amountType.item,
        options = listOf(AmountTypeItem.Coin, AmountTypeItem.Currency(currencyManager.baseCurrency.code))
    )

    private fun balance(coin: Token): BigDecimal? =
        (adapterManager.getAdapterForToken(coin) as? IBalanceAdapter)?.balanceData?.available

    private fun syncBalance(balance: BigDecimal?) {
        balanceFrom = balance
        val token = fromTokenService.token
        val formattedBalance: String?
        val hasNonZeroBalance: Boolean?
        when {
            token == null -> {
                formattedBalance = Translator.getString(R.string.NotAvailable)
                hasNonZeroBalance = null
            }

            balance == null -> {
                formattedBalance = null
                hasNonZeroBalance = null
            }

            else -> {
                formattedBalance = formatter.coinAmount(balance, token.coin.code)
                hasNonZeroBalance = balance > BigDecimal.ZERO
            }
        }
        availableBalance = formattedBalance
        this.hasNonZeroBalance = hasNonZeroBalance
        syncUiState()
    }

    private fun setBalance() {
        fromTokenService.token?.let {
            syncBalance(balance(it))
        }
    }

    private fun oneInchTradeViewItem(params: SwapMainModule.OneInchSwapParameters, tokenFrom: Token?, tokenTo: Token?) = try {
        val sellPrice = params.amountTo.divide(params.amountFrom, params.tokenFrom.decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val buyPrice = params.amountFrom.divide(params.amountTo, params.tokenTo.decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val (primaryPrice, secondaryPrice) = formatter.prices(sellPrice, buyPrice, tokenFrom, tokenTo)
        SwapMainModule.TradeViewX(ProviderTradeData.OneInchTradeViewItem(primaryPrice, secondaryPrice))
    } catch (exception: ArithmeticException) {
        null
    }

    private fun uniswapTradeViewItem(swapData: SwapData.UniswapData, tokenFrom: Token?, tokenTo: Token?): SwapMainModule.TradeViewX {
        val (primaryPrice, secondaryPrice) = swapData.data.executionPrice?.let {
            val sellPrice = it
            val buyPrice = BigDecimal.ONE.divide(sellPrice, sellPrice.scale(), RoundingMode.HALF_EVEN)
            formatter.prices(sellPrice, buyPrice, tokenFrom, tokenTo)
        } ?: Pair(null, null)

        return SwapMainModule.TradeViewX(
            ProviderTradeData.UniswapTradeViewItem(
                primaryPrice = primaryPrice,
                secondaryPrice = secondaryPrice,
                priceImpact = formatter.priceImpactViewItem(swapData, PriceImpactLevel.Normal),
            )
        )
    }

    private val AmountType.item: AmountTypeItem
        get() = when (this) {
            AmountType.Coin -> AmountTypeItem.Coin
            AmountType.Currency -> AmountTypeItem.Currency(currencyManager.baseCurrency.code)
        }

    private fun revokeRequired(): Boolean {
        val tokenFrom = fromTokenService.token ?: return false
        val allowance = approveData?.allowance ?: return false

        return allowance.compareTo(BigDecimal.ZERO) != 0 && isUsdt(tokenFrom)
    }

    private fun isUsdt(token: Token): Boolean {
        val tokenType = token.type

        return token.blockchainType is BlockchainType.Ethereum
                && tokenType is TokenType.Eip20
                && tokenType.address.lowercase() == "0xdac17f958d2ee523a2206206994597c13d831ec7"
    }

    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
        return when {
            amount1 == null && amount2 == null -> true
            amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
            else -> false
        }
    }

    private fun convert(error: Throwable): String =
        when (val convertedError = error.convertedError) {
            is JsonRpc.ResponseError.RpcError -> {
                convertedError.error.message
            }

            is EvmError.InsufficientLiquidity -> {
                Translator.getString(R.string.EthereumTransaction_Error_InsufficientLiquidity)
            }

            else -> {
                convertedError.message ?: convertedError.javaClass.simpleName
            }
        }

    override fun onCleared() {
        disposable.dispose()
        tradeDisposable.dispose()
        tradeService.stop()
        allowanceService.onCleared()
        pendingAllowanceService.onCleared()
        fromTokenService.stop()
        toTokenService.stop()
    }

    fun onToggleAmountType() {
        switchService.toggle()
    }

    fun onSelectFromCoin(token: Token) {
        fromTokenService.onSelectCoin(token)
        syncBalance(balance(token))
        if (exactType == ExactType.ExactTo) {
            fromTokenService.onChangeAmount(null, true)
        }
        if (token == toTokenService.token) {
            toTokenService.setToken(null)
            toTokenService.onChangeAmount(null, true)
        }
        resyncSwapData()
        allowanceService.set(token)
        pendingAllowanceService.set(token)
    }

    fun onSelectToCoin(token: Token) {
        toTokenService.onSelectCoin(token)
        if (exactType == ExactType.ExactFrom) {
            toTokenService.onChangeAmount(null, true)
        }
        if (token == fromTokenService.token) {
            fromTokenService.setToken(null)
            fromTokenService.onChangeAmount(null, true)
        }
        resyncSwapData()
    }

    fun onFromAmountChange(amount: String?) {
        exactType = ExactType.ExactFrom
        val coinAmount = fromTokenService.getCoinAmount(amount)
        if (amountsEqual(amountFrom, coinAmount)) return
        amountFrom = coinAmount
        amountTo = null
        fromTokenService.onChangeAmount(amount)
        toTokenService.onChangeAmount(null, true)
        resyncSwapData()
    }

    fun onToAmountChange(amount: String?) {
        exactType = ExactType.ExactTo
        val coinAmount = toTokenService.getCoinAmount(amount)
        if (amountsEqual(amountTo, coinAmount)) return
        amountTo = coinAmount
        amountFrom = null
        toTokenService.onChangeAmount(amount)
        fromTokenService.onChangeAmount(null, true)
        resyncSwapData()
    }

    fun onTapSwitch() {
        val fromToken = fromTokenService.token
        val toToken = toTokenService.token

        fromTokenService.setToken(toToken)
        toTokenService.setToken(fromToken)

        resyncSwapData()
        setBalance()
        allowanceService.set(toToken)
        pendingAllowanceService.set(toToken)
    }

    fun setProvider(provider: ISwapProvider) {
        tradeService.stop()
        service.setProvider(provider)
        subscribeToTradeService()

        timerService.stop()
        timerService.start()

        refocusKey = UUID.randomUUID().leastSignificantBits
        syncUiState()
    }

    fun onSetAmountInBalancePercent(percent: Int) {
        val coinDecimals = fromTokenService.token?.decimals ?: maxValidDecimals
        val percentRatio = BigDecimal.valueOf(percent.toDouble() / 100)
        val coinAmount = balanceFrom?.multiply(percentRatio)?.setScale(coinDecimals, RoundingMode.FLOOR) ?: return

        val amount = fromTokenService.getCoinAmount(coinAmount)
        onFromAmountChange(amount.toPlainString())
    }

    fun didApprove() {
        pendingAllowanceService.syncAllowance()
    }

    fun getSendEvmData(swapData: SwapData.UniswapData): SendEvmData? {
        val uniswapTradeService = tradeService as? IUniswapTradeService ?: return null
        val tradeOptions = uniswapTradeService.tradeOptions
        val transactionData = try {
            uniswapTradeService.transactionData(swapData.data)
        } catch (e: Exception) {
            return null
        }

        val (primaryPrice, _) = swapData.data.executionPrice?.let {
            val sellPrice = it
            val buyPrice = BigDecimal.ONE.divide(sellPrice, sellPrice.scale(), RoundingMode.HALF_EVEN)
            formatter.prices(sellPrice, buyPrice, fromTokenService.token, toTokenService.token)
        } ?: Pair(null, null)

        val swapInfo = SendEvmData.UniswapInfo(
            estimatedIn = amountFrom ?: BigDecimal.ZERO,
            estimatedOut = amountTo ?: BigDecimal.ZERO,
            slippage = formatter.slippage(tradeOptions.allowedSlippage),
            deadline = formatter.deadline(tradeOptions.ttl),
            recipientDomain = tradeOptions.recipient?.title,
            price = primaryPrice,
            priceImpact = formatter.priceImpactViewItem(swapData)
        )

        return SendEvmData(
            transactionData,
            SendEvmData.AdditionalInfo.Uniswap(swapInfo),
        )
    }

    fun onUpdateSwapSettings(recipient: Address?, slippage: BigDecimal?, ttl: Long?) {
        tradeService.updateSwapSettings(recipient, slippage, ttl)
        syncSwapDataState()
    }

}
