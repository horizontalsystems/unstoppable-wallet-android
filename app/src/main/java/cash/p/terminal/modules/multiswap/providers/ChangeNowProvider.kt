package cash.p.terminal.modules.multiswap.providers

import androidx.collection.LruCache
import cash.p.terminal.R
import cash.p.terminal.core.cache.accountScoped
import cash.p.terminal.core.extractBigDecimal
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.modules.multiswap.ISwapFinalQuote
import cash.p.terminal.modules.multiswap.ISwapQuote
import cash.p.terminal.modules.multiswap.SwapDepositTooSmall
import cash.p.terminal.modules.multiswap.SwapFinalQuoteEvm
import cash.p.terminal.modules.multiswap.SwapQuoteOffChain
import cash.p.terminal.modules.multiswap.action.ActionCreate
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.ui.DataFieldRecipientExtended
import cash.p.terminal.network.changenow.data.entity.BackendChangeNowResponseError
import cash.p.terminal.network.changenow.data.entity.request.NewTransactionRequest
import cash.p.terminal.network.changenow.domain.entity.ChangeNowCurrency
import cash.p.terminal.network.changenow.domain.entity.ExchangeAmount
import cash.p.terminal.network.changenow.domain.entity.NewTransactionResponse
import cash.p.terminal.network.changenow.domain.repository.ChangeNowRepository
import cash.p.terminal.network.pirate.domain.useCase.GetChangeNowAssociatedCoinTickerUseCase
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.System
import java.math.BigDecimal

class ChangeNowProvider(
    override val walletUseCase: WalletUseCase,
    private val changeNowRepository: ChangeNowRepository,
    private val getChangeNowAssociatedCoinTickerUseCase: GetChangeNowAssociatedCoinTickerUseCase,
    accountManager: IAccountManager,
    private val providerSupport: OffChainSwapProviderSupport,
) : OffChainSwapProvider {
    override val id = "changenow"
    override val title = "ChangeNow"
    override val icon = R.drawable.ic_change_now
    override val riskType = ProviderRiskType.PreCheck

    override val mevProtectionAvailable: Boolean = false
    private val currencies = mutableListOf<ChangeNowCurrency>()
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

    private val minAmount: LruCache<String, BigDecimal> = LruCache(10)
    private var minAmountTimestamp = LruCache<String, Long>(10)

    // SwapConfirmViewModel calls final quote too many times, so cache results
    private var finalQuote: CachedFinalQuote? by accountManager.accountScoped()
    private val mutex = Mutex()

    private data class CachedFinalQuote(
        val request: NewTransactionRequest,
        val response: NewTransactionResponse,
        val timestamp: Long,
    )

    private companion object {
        const val CACHE_MIN_AMOUNT_DURATION = 1000L * 60
        const val CACHE_FINAL_QUOTE_DURATION = 1000L * 60 * 5
    }

    override suspend fun start() {
        currencies.clear()
        currencies.addAll(changeNowRepository.getAvailableCurrencies().sortedBy { it.ticker })
    }

    override suspend fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        if (tokenTo.isZcashNonTransparent) return false
        return supports(tokenFrom) && supports(tokenTo)
    }

    override suspend fun supports(token: Token): Boolean {
        if (token.isZcashShielded) return false

        return withContext(coroutineScope.coroutineContext) {
            try {
                getChangeNowTicker(token)?.let {
                    isChangeNowTickerActive(it)
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun getChangeNowTicker(token: Token): String? =
        getChangeNowAssociatedCoinTickerUseCase(
            token.coin.uid,
            token.blockchainType.uid
        )

    private fun isChangeNowTickerActive(ticker: String): Boolean = currencies.find {
        it.ticker == ticker
    } != null

    private suspend fun getExchangeAmountOrThrow(
        tickerFrom: String,
        tickerTo: String,
        amountIn: BigDecimal
    ): ExchangeAmount {
        return try {
            changeNowRepository.getExchangeAmount(
                tickerFrom = tickerFrom,
                tickerTo = tickerTo,
                amount = amountIn
            )
        } catch (e: BackendChangeNowResponseError) {
            //extract decimal from message
            if (e.error == BackendChangeNowResponseError.Companion.DEPOSIT_TOO_SMALL) {
                val amount = e.message.extractBigDecimal() ?: throw e
                throw SwapDepositTooSmall(amount)
            } else {
                throw e
            }
        } catch (e: Exception) {
            throw IllegalStateException("ChangeNowProvider: error fetching amount", e)
        }
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote = withContext(coroutineScope.coroutineContext) {
        mutex.withLock {
            val (tickerIn, tickerOut) = awaitAll(
                async { getChangeNowTicker(tokenIn) },
                async { getChangeNowTicker(tokenOut) }
            )

            val tickerFrom = tickerIn
                ?: throw IllegalStateException("ChangeNowProvider: ticker for $tokenIn is not found")
            val tickerTo = tickerOut
                ?: throw IllegalStateException("ChangeNowProvider: ticker for $tokenOut is not found")
            val key = "$tickerFrom $tickerTo"
            val cachedValue = minAmount[key]
            val cachedTimestamp = minAmountTimestamp[key] ?: 0L
            if (cachedValue != null && System.currentTimeMillis() - cachedTimestamp < CACHE_MIN_AMOUNT_DURATION) {
                cachedValue
            } else {
                changeNowRepository.getMinAmount(
                    tickerFrom = tickerFrom,
                    tickerTo = tickerTo
                ).also {
                    minAmount.put(key, it)
                    minAmountTimestamp.put(key, System.currentTimeMillis())
                }
            }.also {
                if (it > amountIn) {
                    throw SwapDepositTooSmall(it)
                }
            }

            val exchangeAmount = getExchangeAmountOrThrow(tickerFrom, tickerTo, amountIn)
            val amountOut = exchangeAmount.estimatedAmount
                ?: throw IllegalStateException("ChangeNowProvider: amount is not found")

            val actionRequired = getCreateTokenActionRequired(tokenIn, tokenOut)

            SwapQuoteOffChain(
                amountOut = amountOut,
                priceImpact = null,
                fields = emptyList(),
                settings = emptyList(),
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn,
                actionRequired = actionRequired,
                estimationTime = parseMinutesRangeToSeconds(exchangeAmount.transactionSpeedForecast),
            )
        }
    }

    override fun getCreateTokenActionRequired(
        tokenIn: Token,
        tokenOut: Token
    ): ActionCreate? = providerSupport.getCreateTokenActionRequired(tokenIn, tokenOut)

    override suspend fun getWarningMessage(tokenIn: Token, tokenOut: Token): TranslatableString? =
        withContext(Dispatchers.IO) {
            providerSupport.getWarningMessage(tokenIn)
        }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: ISwapQuote
    ): ISwapFinalQuote = withContext(coroutineScope.coroutineContext) {
        mutex.withLock {
            val transaction: NewTransactionResponse = try {
                val (tickerIn, tickerOut) = awaitAll(
                    async { getChangeNowTicker(tokenIn) },
                    async { getChangeNowTicker(tokenOut) }
                )

                val tickerFrom =
                    requireNotNull(tickerIn) { "ChangeNowProvider: ticker for $tokenIn is not found" }
                val tickerTo =
                    requireNotNull(tickerOut) { "ChangeNowProvider: ticker for $tokenOut is not found" }

                val request = NewTransactionRequest(
                    from = tickerFrom,
                    to = tickerTo,
                    amount = amountIn.toPlainString(),
                    address = walletUseCase.getReceiveAddress(tokenOut),
                    refundAddress = providerSupport.getRefundAddress(tokenIn)
                )
                val cached = finalQuote
                if (cached != null &&
                    cached.request == request &&
                    System.currentTimeMillis() - cached.timestamp < CACHE_FINAL_QUOTE_DURATION
                ) {
                    cached.response
                } else {
                    changeNowRepository.createTransaction(
                        newTransactionRequest = request
                    ).also {
                        finalQuote = CachedFinalQuote(request, it, System.currentTimeMillis())
                    }
                }
            } catch (e: BackendChangeNowResponseError) {
                //extract decimal from message
                if (e.error == BackendChangeNowResponseError.Companion.OUT_OF_RANGE) {
                    val amount = e.message.extractBigDecimal() ?: throw e
                    return@withContext SwapFinalQuoteEvm(
                        tokenIn = tokenIn,
                        tokenOut = tokenOut,
                        amountIn = amountIn,
                        amountOut = BigDecimal.ZERO,
                        amountOutMin = amount,
                        // Placeholder transaction data because out of range error
                        sendTransactionData = SendTransactionData.Btc(
                            amount = amountIn,
                            address = "",
                            changeToFirstInput = false,
                            utxoFilters = UtxoFilters(),
                            memo = "",
                            recommendedGasRate = null,
                            minimumSendAmount = null,
                            feesMap = emptyMap()
                        ),
                        priceImpact = null,
                        fields = emptyList(),
                    )
                } else {
                    throw e
                }
            } catch (e: Exception) {
                throw IllegalStateException("ChangeNowProvider: error fetchFinalQuote", e)
            }

            val fields = buildList {
                add(
                    DataFieldRecipientExtended(
                        address = Address(transaction.payinAddress),
                        blockchainType = tokenIn.blockchainType
                    )
                )
            }

            val swapProviderTransaction = providerSupport.buildSwapProviderTransaction(
                provider = SwapProvider.CHANGENOW,
                transactionId = transaction.id,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn,
                amountOut = transaction.amount,
            )

            SwapFinalQuoteEvm(
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn,
                amountOut = transaction.amount,
                amountOutMin = transaction.amount,
                sendTransactionData = providerSupport.buildTransactionData(
                    tokenIn = tokenIn,
                    amountIn = amountIn,
                    depositAddress = transaction.payinAddress,
                    memo = transaction.mandatoryMemo,
                ),
                priceImpact = null,
                fields = fields,
                swapProviderTransaction = swapProviderTransaction,
            )
        }
    }

    override fun onTransactionCompleted(
        transaction: SwapProviderTransaction,
        result: SendTransactionResult,
    ) = providerSupport.onTransactionCompleted(transaction, result)
}

/**
 * Converts ChangeNow's `transactionSpeedForecast` (a minutes value, e.g. "30" or a range "10-60")
 * into seconds. A single number yields that number; a range yields its upper bound (conservative).
 * Returns null when the input has no digits to parse.
 */
internal fun parseMinutesRangeToSeconds(text: String?): Long? {
    val maxMinutes = text?.let {
        Regex("""\d+""").findAll(it).maxOfOrNull { match -> match.value.toLong() }
    }
    return maxMinutes?.let { it * 60 }
}
