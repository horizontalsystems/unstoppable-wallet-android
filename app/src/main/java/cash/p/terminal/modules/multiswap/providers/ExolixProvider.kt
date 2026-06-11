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
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.ui.DataFieldRecipientExtended
import cash.p.terminal.network.exolix.data.entity.BackendExolixResponseError
import cash.p.terminal.network.exolix.data.entity.request.NewTransactionExolixRequest
import cash.p.terminal.network.exolix.domain.entity.ExolixNetwork
import cash.p.terminal.network.exolix.domain.entity.ExolixTransaction
import cash.p.terminal.network.exolix.domain.repository.ExolixRepository
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

class ExolixProvider(
    override val walletUseCase: WalletUseCase,
    private val exolixRepository: ExolixRepository,
    accountManager: IAccountManager,
    private val dispatcherProvider: DispatcherProvider,
    private val providerSupport: OffChainSwapProviderSupport,
) : OffChainSwapProvider {
    override val id = "exolix"
    override val title = "Exolix"
    override val icon = R.drawable.ic_exolix

    override val mevProtectionAvailable: Boolean = false
    private val currencyNetworksCache = mutableMapOf<String, List<ExolixNetwork>>()
    private val assetCache = mutableMapOf<String, ExolixAsset?>()
    private val assetMutex = Mutex()

    private val minAmount: LruCache<String, BigDecimal> = LruCache(10)
    private var minAmountTimestamp = LruCache<String, Long>(10)

    // SwapConfirmViewModel calls final quote too many times, so cache results
    private var finalQuote: CachedFinalQuote? by accountManager.accountScoped()
    private val mutex = Mutex()

    private data class CachedFinalQuote(
        val request: NewTransactionExolixRequest,
        val response: ExolixTransaction,
        val timestamp: Long,
    )

    private data class ExolixAsset(
        val coin: String,
        val network: String,
    )

    private companion object {
        const val CACHE_MIN_AMOUNT_DURATION = 1000L * 60
        const val CACHE_FINAL_QUOTE_DURATION = 1000L * 60 * 5
        const val RATE_TYPE = "fixed"
        val networkByBlockchainUid = mapOf(
            BlockchainType.Bitcoin.uid to "BTC",
            BlockchainType.BitcoinCash.uid to "BCH",
            BlockchainType.ECash.uid to "XEC",
            BlockchainType.Litecoin.uid to "LTC",
            BlockchainType.Dogecoin.uid to "DOGE",
            BlockchainType.Dash.uid to "DASH",
            BlockchainType.Zcash.uid to "ZEC",
            BlockchainType.Stellar.uid to "XLM",
            BlockchainType.Ethereum.uid to "ETH",
            BlockchainType.BinanceSmartChain.uid to "BSC",
            BlockchainType.Polygon.uid to "MATIC",
            BlockchainType.Avalanche.uid to "AVAXC",
            BlockchainType.Optimism.uid to "OPTIMISM",
            BlockchainType.ArbitrumOne.uid to "ARBITRUM",
            BlockchainType.Solana.uid to "SOL",
            BlockchainType.Tron.uid to "TRX",
            BlockchainType.Ton.uid to "TON",
            BlockchainType.Base.uid to "BASE",
            BlockchainType.ZkSync.uid to "ZKSYNCERA",
            BlockchainType.Fantom.uid to "FTM",
            BlockchainType.Monero.uid to "XMR",
        )
    }

    override suspend fun start() {
        assetMutex.withLock {
            currencyNetworksCache.clear()
            assetCache.clear()
        }
    }

    override suspend fun supports(tokenFrom: Token, tokenTo: Token) =
        supports(tokenFrom) && supports(tokenTo)

    override suspend fun supports(token: Token): Boolean {
        if (token.isZcashShielded) return false
        return withContext(dispatcherProvider.io) {
            resolveAsset(token) != null
        }
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote = withContext(dispatcherProvider.io) {
        mutex.withLock {
            val assetIn = requireAsset(tokenIn)
            val assetOut = requireAsset(tokenOut)
            val key = getCacheKey(assetIn, assetOut)
            val cachedValue = minAmount[key]
            val cachedTimestamp = minAmountTimestamp[key] ?: 0L
            if (cachedValue != null &&
                (System.currentTimeMillis() - cachedTimestamp < CACHE_MIN_AMOUNT_DURATION) &&
                cachedValue > amountIn
            ) {
                throw SwapDepositTooSmall(cachedValue)
            }

            val rate = try {
                exolixRepository.getRate(
                    coinFrom = assetIn.coin,
                    networkFrom = assetIn.network,
                    coinTo = assetOut.coin,
                    networkTo = assetOut.network,
                    amount = amountIn,
                    rateType = RATE_TYPE,
                )
            } catch (e: BackendExolixResponseError) {
                val amount = e.message.extractBigDecimal() ?: throw e
                throw SwapDepositTooSmall(amount)
            } catch (e: Exception) {
                throw IllegalStateException("ExolixProvider: error fetching amount", e)
            }

            minAmount.put(key, rate.minAmount)
            minAmountTimestamp.put(key, System.currentTimeMillis())
            if (rate.minAmount > amountIn) {
                throw SwapDepositTooSmall(rate.minAmount)
            }

            val actionRequired = getCreateTokenActionRequired(tokenIn, tokenOut)

            SwapQuoteOffChain(
                amountOut = rate.toAmount,
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
    ): ActionCreate? = providerSupport.getCreateTokenActionRequired(
        tokenIn = tokenIn,
        tokenOut = tokenOut,
        useTransparentZcashRefundAddress = useTransparentZcashRefundAddress(tokenIn),
    )

    override suspend fun getWarningMessage(tokenIn: Token, tokenOut: Token): TranslatableString? =
        withContext(dispatcherProvider.io) {
            providerSupport.getWarningMessage(
                tokenIn = tokenIn,
                useTransparentZcashRefundAddress = useTransparentZcashRefundAddress(tokenIn),
            )
        }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: ISwapQuote
    ): ISwapFinalQuote = withContext(dispatcherProvider.io) {
        mutex.withLock {
            val transaction = try {
                val assetIn = requireAsset(tokenIn)
                val assetOut = requireAsset(tokenOut)
                val request = NewTransactionExolixRequest(
                    coinFrom = assetIn.coin,
                    networkFrom = assetIn.network,
                    coinTo = assetOut.coin,
                    networkTo = assetOut.network,
                    amount = amountIn.toPlainString(),
                    withdrawalAddress = walletUseCase.getReceiveAddress(tokenOut),
                    rateType = RATE_TYPE,
                    refundAddress = providerSupport.getRefundAddress(
                        tokenIn = tokenIn,
                        useTransparentZcashRefundAddress = useTransparentZcashRefundAddress(tokenIn),
                    ),
                )
                val cached = finalQuote
                if (cached != null &&
                    cached.request == request &&
                    System.currentTimeMillis() - cached.timestamp < CACHE_FINAL_QUOTE_DURATION
                ) {
                    cached.response
                } else {
                    exolixRepository.createTransaction(
                        newTransactionRequest = request
                    ).also {
                        finalQuote = CachedFinalQuote(request, it, System.currentTimeMillis())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw IllegalStateException("ExolixProvider: error fetchFinalQuote", e)
            }

            val fields = buildList {
                add(
                    DataFieldRecipientExtended(
                        address = Address(transaction.depositAddress),
                        blockchainType = tokenIn.blockchainType
                    )
                )
            }
            val swapProviderTransaction = providerSupport.buildSwapProviderTransaction(
                provider = SwapProvider.EXOLIX,
                transactionId = transaction.id,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn,
                amountOut = transaction.amountTo,
            )

            SwapFinalQuoteEvm(
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn,
                amountOut = transaction.amountTo,
                amountOutMin = transaction.amountTo,
                sendTransactionData = providerSupport.buildTransactionData(
                    tokenIn = tokenIn,
                    amountIn = amountIn,
                    depositAddress = transaction.depositAddress,
                    memo = transaction.depositExtraId,
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

    private fun useTransparentZcashRefundAddress(token: Token): Boolean =
        !token.isZcashUnified

    private suspend fun requireAsset(token: Token): ExolixAsset =
        requireNotNull(resolveAsset(token)) { "ExolixProvider: asset for $token is not found" }

    private suspend fun resolveAsset(token: Token): ExolixAsset? {
        val cacheKey = getAssetCacheKey(token)
        assetMutex.withLock {
            if (assetCache.containsKey(cacheKey)) return assetCache[cacheKey]
        }

        val network = getExolixNetwork(token) ?: return null
        val asset = findRemoteAsset(token, network)

        assetMutex.withLock {
            assetCache[cacheKey] = asset
        }

        return asset
    }

    private suspend fun findRemoteAsset(token: Token, network: String): ExolixAsset? {
        val coin = getExolixCoin(token)
        val contractAddress = token.contractAddress()
        val exolixNetwork = getCurrencyNetworks(coin).firstOrNull { exolixNetwork ->
            val networkMatches = exolixNetwork.network.equals(network, ignoreCase = true)
            val contractMatches = contractAddress.isEmpty() ||
                    exolixNetwork.contract?.equals(contractAddress, ignoreCase = true) == true

            networkMatches && contractMatches
        }

        return exolixNetwork?.let { ExolixAsset(coin, it.network) }
    }

    private suspend fun getCurrencyNetworks(code: String): List<ExolixNetwork> {
        val cacheKey = code.lowercase()
        assetMutex.withLock {
            currencyNetworksCache[cacheKey]?.let { return it }
        }

        val result = try {
            exolixRepository.getCurrencyNetworks(code)
        } catch (e: BackendExolixResponseError) {
            if (e.statusCode == 404) emptyList() else throw e
        }

        assetMutex.withLock {
            currencyNetworksCache[cacheKey] = result
        }

        return result
    }

    private fun getExolixCoin(token: Token): String {
        return when {
            token.blockchainType == BlockchainType.Zcash &&
                    token.type == TokenType.AddressSpecTyped(TokenType.AddressSpecType.Unified) -> "ZEC-SHIELDED"

            token.type is TokenType.Native &&
                    token.blockchainType == BlockchainType.Polygon &&
                    token.coin.code.equals("MATIC", ignoreCase = true) -> "POL"

            else -> token.coin.code
        }
    }

    private fun getExolixNetwork(token: Token): String? =
        networkByBlockchainUid[token.blockchainType.uid]

    private fun getCacheKey(
        assetIn: ExolixAsset,
        assetOut: ExolixAsset,
    ) = "${assetIn.coin}-${assetIn.network}-${assetOut.coin}-${assetOut.network}"

    private fun getAssetCacheKey(token: Token) =
        "${token.coin.uid}-${token.blockchainType.uid}-${token.type}-${token.contractAddress()}"
}
