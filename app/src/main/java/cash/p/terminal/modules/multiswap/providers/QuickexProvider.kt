package cash.p.terminal.modules.multiswap.providers

import androidx.collection.LruCache
import cash.p.terminal.R
import cash.p.terminal.core.cache.accountScoped
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.modules.multiswap.ISwapFinalQuote
import cash.p.terminal.modules.multiswap.ISwapQuote
import cash.p.terminal.modules.multiswap.SwapDepositTooSmall
import cash.p.terminal.modules.multiswap.SwapFinalQuoteEvm
import cash.p.terminal.modules.multiswap.SwapQuoteOffChain
import cash.p.terminal.modules.multiswap.action.ActionCreate
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.ui.DataFieldRecipientExtended
import cash.p.terminal.network.quickex.data.entity.BackendQuickexResponseError
import cash.p.terminal.network.quickex.data.entity.request.InstrumentRequest
import cash.p.terminal.network.quickex.data.entity.request.NewTransactionQuickexRequest
import cash.p.terminal.network.quickex.domain.entity.NewTransactionQuickexResponse
import cash.p.terminal.network.quickex.domain.entity.QuickexInstrument
import cash.p.terminal.network.quickex.domain.repository.QuickexRepository
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.useCases.WalletUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class QuickexProvider(
    override val walletUseCase: WalletUseCase,
    private val quickexRepository: QuickexRepository,
    accountManager: IAccountManager,
    private val providerSupport: OffChainSwapProviderSupport,
) : OffChainSwapProvider {
    override val id = "quickex"
    override val title = "Quickex"
    override val icon = R.drawable.ic_quickex

    override val mevProtectionAvailable: Boolean = false
    private val currencies = mutableListOf<QuickexInstrument>()
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
        val request: NewTransactionQuickexRequest,
        val response: NewTransactionQuickexResponse,
        val timestamp: Long,
    )

    private companion object {
        const val CACHE_MIN_AMOUNT_DURATION = 1000L * 60
        const val CACHE_FINAL_QUOTE_DURATION = 1000L * 60 * 5
    }

    override suspend fun start() {
        currencies.clear()
        currencies.addAll(quickexRepository.getAvailablePairs())
    }

    override suspend fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        if (tokenTo.isZcashNonTransparent) return false
        return supports(tokenFrom) && supports(tokenTo)
    }


    override suspend fun supports(token: Token): Boolean {
        if (token.isZcashShielded) return false

        return withContext(coroutineScope.coroutineContext) {
            currencies.find {
                isSameToken(it, token) ||
                        isSameContract(it, token)
            } != null
        }
    }

    private fun isSameToken(quickexInstrument: QuickexInstrument, token: Token): Boolean {
        val tokenTicker = getQuickexTicker(token) ?: return false
        val tokenNetwork = getQuickexNetwork(token) ?: return false
        return quickexInstrument.currencyTitle.equals(tokenTicker, true) &&
                quickexInstrument.networkTitle.equals(tokenNetwork, true)
    }

    private fun isSameContract(quickexInstrument: QuickexInstrument, token: Token): Boolean {
        val contractAddress = token.contractAddress()
        if (contractAddress.isEmpty()) return false

        return quickexInstrument.contractAddress == contractAddress
    }

    private fun getQuickexTicker(token: Token): String? =
        currencies.find {
            isSameContract(it, token)
        }?.currencyTitle ?: token.coin.code

    /***
     * Return badge (network) if exists, or ticker if native
     */
    private fun getQuickexNetwork(token: Token): String? {
        currencies.find {
            isSameContract(it, token)
        }?.networkTitle?.let { return it }

        val ticker = getQuickexTicker(token)

        if (token.type is TokenType.Native && ticker != null) {
            val blockchainUid = token.blockchainType.uid.lowercase()
            val networkOverride = when (blockchainUid) {
                "base" if ticker.equals("ETH", true) -> "BASE"
                "arbitrum-one" if ticker.equals("ETH", true) -> "ARBITRUM"
                "optimistic-ethereum" if ticker.equals("ETH", true) -> "OPTIMISM"
                "binance-smart-chain" if ticker.equals("BNB", true) -> "BEP20"
                "polygon-pos" if ticker.equals("POL", true) -> "POLYGON"
                "avalanche" if ticker.equals("AVAX", true) -> "AVAX C-Chain"
                "polkadot" if ticker.equals("DOT", true) -> "Asset Hub(Polkadot)"
                "kusama" if ticker.equals("KSM", true) -> "KUSAMA"
                "kaspa" if ticker.equals("KAS", true) -> "KASPA"
                "nem" if ticker.equals("XEM", true) -> "NEM"
                "bittensor" if ticker.equals("TAO", true) -> "Bittensor"
                "celestia" if ticker.equals("TIA", true) -> "CELESTIA"
                "lukso-token-2" if ticker.equals("LYX", true) -> "LUKSO"
                "hydra" if ticker.equals("HYDRA", true) -> "Hydragon"
                "oasis-network" if ticker.equals("ROSE", true) -> "OASIS"
                else -> null
            }

            if (networkOverride != null) return networkOverride
        }

        return when (token.type) {
            is TokenType.Native,
            is TokenType.Derived,
            is TokenType.AddressTyped,
            is TokenType.AddressSpecTyped -> ticker

            else -> token.badge ?: ticker
        }
    }

    private suspend fun getExchangeAmountOrThrow(
        tickerFrom: String,
        networkFrom: String,
        tickerTo: String,
        networkTo: String,
        amountIn: BigDecimal
    ): BigDecimal? {
        return try {
            quickexRepository.getRates(
                fromCurrency = tickerFrom,
                fromNetwork = networkFrom,
                toCurrency = tickerTo,
                toNetwork = networkTo,
                claimedDepositAmount = amountIn
            ).amountToGet
        } catch (e: BackendQuickexResponseError) {
            //extract decimal from message
            if (e.status == BackendQuickexResponseError.DEPOSIT_TOO_SMALL) {
                val amount = e.data?.details?.expected?.toBigDecimalOrNull() ?: throw e
                throw SwapDepositTooSmall(amount)
            } else {
                throw e
            }
        } catch (e: Exception) {
            throw IllegalStateException("QuickexProvider: error fetching amount", e)
        }
    }

    private fun getCacheKey(
        tickerFrom: String,
        networkFrom: String,
        tickerTo: String,
        networkTo: String
    ) = "$tickerFrom-$networkFrom-$tickerTo-$networkTo"

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote = withContext(coroutineScope.coroutineContext) {
        mutex.withLock {
            val (tickerIn, tickerOut) = awaitAll(
                async { getQuickexTicker(tokenIn) },
                async { getQuickexTicker(tokenOut) }
            )

            val (networkIn, networkOut) = awaitAll(
                async { getQuickexNetwork(tokenIn) },
                async { getQuickexNetwork(tokenOut) }
            )

            val tickerFrom =
                requireNotNull(tickerIn) { "QuickexProvider: ticker for $tokenIn is not found" }
            val tickerTo =
                requireNotNull(tickerOut) { "QuickexProvider: ticker for $tokenOut is not found" }
            val networkFrom =
                requireNotNull(networkIn) { "QuickexProvider: network for $tokenIn is not found" }
            val networkTo =
                requireNotNull(networkOut) { "QuickexProvider: network for $tokenOut is not found" }

            val key = getCacheKey(
                tickerFrom = tickerFrom,
                networkFrom = networkFrom,
                tickerTo = tickerTo,
                networkTo = networkTo
            )
            val cachedValue = minAmount[key]
            val cachedTimestamp = minAmountTimestamp[key] ?: 0L
            if (cachedValue != null &&
                (System.currentTimeMillis() - cachedTimestamp < CACHE_MIN_AMOUNT_DURATION) &&
                cachedValue > amountIn
            ) {
                throw SwapDepositTooSmall(cachedValue)
            }

            val amountOut = try {
                getExchangeAmountOrThrow(
                    tickerFrom = tickerFrom,
                    networkFrom = networkFrom,
                    tickerTo = tickerTo,
                    networkTo = networkTo,
                    amountIn = amountIn
                ) ?: throw IllegalStateException("QuickexProvider: amount is not found")
            } catch (e: SwapDepositTooSmall) {
                minAmount.put(key, e.minValue)
                minAmountTimestamp.put(key, System.currentTimeMillis())
                throw e
            } catch (e: Exception) {
                throw IllegalStateException("QuickexProvider: amount is not found")
            }

            val actionRequired = getCreateTokenActionRequired(tokenIn, tokenOut)

            SwapQuoteOffChain(
                amountOut = amountOut,
                priceImpact = null,
                fields = emptyList(),
                settings = emptyList(),
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn,
                actionRequired = actionRequired
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
            val transaction: NewTransactionQuickexResponse = try {
                val (tickerIn, tickerOut) = awaitAll(
                    async { getQuickexTicker(tokenIn) },
                    async { getQuickexTicker(tokenOut) }
                )

                val (networkIn, networkOut) = awaitAll(
                    async { getQuickexNetwork(tokenIn) },
                    async { getQuickexNetwork(tokenOut) }
                )

                val tickerFrom =
                    requireNotNull(tickerIn) { "QuickexProvider: ticker for $tokenIn is not found" }
                val tickerTo =
                    requireNotNull(tickerOut) { "QuickexProvider: ticker for $tokenOut is not found" }
                val networkFrom =
                    requireNotNull(networkIn) { "QuickexProvider: network for $tokenIn is not found" }
                val networkTo =
                    requireNotNull(networkOut) { "QuickexProvider: network for $tokenOut is not found" }

                val request = NewTransactionQuickexRequest(
                    instrumentFrom = InstrumentRequest(
                        currencyTitle = tickerFrom,
                        networkTitle = networkFrom
                    ),
                    instrumentTo = InstrumentRequest(
                        currencyTitle = tickerTo,
                        networkTitle = networkTo
                    ),
                    destinationAddress = walletUseCase.getReceiveAddress(tokenOut),
                    refundAddress = providerSupport.getRefundAddress(tokenIn),
                    claimedDepositAmount = amountIn.toPlainString()
                )
                val cached = finalQuote
                if (cached != null &&
                    cached.request == request &&
                    System.currentTimeMillis() - cached.timestamp < CACHE_FINAL_QUOTE_DURATION
                ) {
                    cached.response
                } else {
                    quickexRepository.createTransaction(
                        newTransactionRequest = request
                    ).also {
                        finalQuote = CachedFinalQuote(request, it, System.currentTimeMillis())
                    }
                }
            } catch (e: Exception) {
                throw IllegalStateException("QuickexProvider: error fetchFinalQuote", e)
            }

            val fields = buildList {
                add(
                    DataFieldRecipientExtended(
                        address = Address(transaction.depositAddress.depositAddress),
                        blockchainType = tokenIn.blockchainType
                    )
                )
            }

            val swapProviderTransaction = providerSupport.buildSwapProviderTransaction(
                provider = SwapProvider.QUICKEX,
                transactionId = transaction.orderId.toString(),
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn,
                amountOut = transaction.amountToGet,
            )

            SwapFinalQuoteEvm(
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn,
                amountOut = transaction.amountToGet,
                amountOutMin = transaction.amountToGet,
                sendTransactionData = providerSupport.buildTransactionData(
                    tokenIn = tokenIn,
                    amountIn = amountIn,
                    depositAddress = transaction.depositAddress.depositAddress,
                    memo = transaction.depositAddress.depositAddressMemo,
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
