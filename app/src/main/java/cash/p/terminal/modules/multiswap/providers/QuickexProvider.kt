package cash.p.terminal.modules.multiswap.providers

import androidx.collection.LruCache
import cash.p.terminal.R
import cash.p.terminal.core.ISendEthereumAdapter
import cash.p.terminal.core.isEvm
import cash.p.terminal.core.isUtxoBased
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.modules.multiswap.ISwapFinalQuote
import cash.p.terminal.modules.multiswap.ISwapQuote
import cash.p.terminal.modules.multiswap.SwapDepositTooSmall
import cash.p.terminal.modules.multiswap.SwapFinalQuoteEvm
import cash.p.terminal.modules.multiswap.SwapQuoteChangeNow
import cash.p.terminal.modules.multiswap.action.ActionCreate
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.ui.DataFieldRecipientExtended
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.quickex.data.entity.BackendQuickexResponseError
import cash.p.terminal.network.quickex.data.entity.request.InstrumentRequest
import cash.p.terminal.network.quickex.data.entity.request.NewTransactionQuickexRequest
import cash.p.terminal.network.quickex.domain.entity.NewTransactionQuickexResponse
import cash.p.terminal.network.quickex.domain.entity.QuickexInstrument
import cash.p.terminal.network.quickex.domain.repository.QuickexRepository
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import java.math.BigDecimal

class QuickexProvider(
    override val walletUseCase: WalletUseCase,
    private val quickexRepository: QuickexRepository,
    private val swapProviderTransactionsStorage: SwapProviderTransactionsStorage
) : IMultiSwapProvider {
    override val id = "quickex"
    override val title = "Quickex"
    override val icon = R.drawable.ic_quickex
    override val priority = 0

    override val mevProtectionAvailable: Boolean = false
    private val marketKit: MarketKitWrapper by KoinJavaComponent.inject(MarketKitWrapper::class.java)
    private val currencies = mutableListOf<QuickexInstrument>()
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

    private val minAmount: LruCache<String, BigDecimal> = LruCache(10)
    private var minAmountTimestamp = LruCache<String, Long>(10)

    // SwapConfirmViewModel calls final quote too many times, so cache results
    private var cachedFinalQuote: Pair<NewTransactionQuickexRequest, NewTransactionQuickexResponse>? =
        null
    private var cacheUpdateTimestamp = 0L
    private val mutex = Mutex()
    private val zcashAddressMutex = Mutex()
    private var cachedZcashTransparentAddress: String? = null

    private companion object {
        const val CACHE_MIN_AMOUNT_DURATION = 1000L * 60
        const val CACHE_FINAL_QUOTE_DURATION = 1000L * 60 * 5
    }

    private var swapProviderTransaction: SwapProviderTransaction? = null

    override suspend fun start() {
        currencies.clear()
        currencies.addAll(quickexRepository.getAvailablePairs())
    }

    override suspend fun supports(tokenFrom: Token, tokenTo: Token) =
        supports(tokenFrom) && supports(tokenTo)


    override suspend fun supports(token: Token): Boolean =
        withContext(coroutineScope.coroutineContext) {
            currencies.find {
                isSameToken(it, token) ||
                        isSameContract(it, token)
            } != null
        }

    private fun isSameToken(quickexInstrument: QuickexInstrument, token: Token): Boolean {
        val tokenTicker = getQuickexTicker(token) ?: return false
        val tokenNetwork = getQuickexNetwork(token) ?: return false
        return quickexInstrument.currencyTitle.equals(tokenTicker, true) &&
                quickexInstrument.networkTitle.equals(tokenNetwork, true)
    }

    private fun isSameContract(quickexInstrument: QuickexInstrument, token: Token): Boolean {
        val contractAddress = when (token.type) {
            is TokenType.Eip20 -> (token.type as? TokenType.Eip20)?.address
            is TokenType.Spl -> (token.type as? TokenType.Spl)?.address
            is TokenType.Jetton -> (token.type as? TokenType.Jetton)?.address
            else -> null
        }.orEmpty()
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

            SwapQuoteChangeNow(
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
    ): ActionCreate? {
        val tokenInWalletCreated = walletUseCase.getWallet(tokenIn) != null
        val tokenOutWalletCreated = walletUseCase.getWallet(tokenOut) != null

        var tokenZCashToCreate: Token? = null
        if (isZCashUnifiedOrShielded(tokenIn)) {
            tokenZCashToCreate = getZCashTransparentToken()
        }
        val needCreateTransparentWallet =
            tokenZCashToCreate != null && walletUseCase.getWallet(tokenZCashToCreate) == null

        return if (!tokenInWalletCreated || !tokenOutWalletCreated || needCreateTransparentWallet) {
            val tokensToAdd = mutableSetOf<Token>()
            if (!tokenInWalletCreated) {
                tokensToAdd.add(tokenIn)
            }
            if (!tokenOutWalletCreated) {
                tokensToAdd.add(tokenOut)
            }
            if (needCreateTransparentWallet) {
                tokensToAdd.add(tokenZCashToCreate)
            }
            ActionCreate(
                inProgress = false,
                descriptionResId = if (!needCreateTransparentWallet) {
                    R.string.swap_create_wallet_description
                } else {
                    R.string.swap_create_wallet_description_with_zcash
                },
                tokensToAdd = tokensToAdd
            )
        } else {
            null
        }
    }

    override suspend fun getWarningMessage(tokenIn: Token, tokenOut: Token): TranslatableString? =
        withContext(Dispatchers.IO) {
            if (!isZCashUnifiedOrShielded(tokenIn)) return@withContext null

            val refundAddress = getCachedZcashTransparentAddress() ?: return@withContext null

            TranslatableString.ResString(R.string.zec_transparent_used, refundAddress)
        }

    private suspend fun getCachedZcashTransparentAddress(): String? =
        zcashAddressMutex.withLock {
            cachedZcashTransparentAddress ?: initializeZcashAddress()
        }

    private suspend fun initializeZcashAddress(): String? {
        val transparentToken = getZCashTransparentToken() ?: return null
        val address = tryOrNull {
            walletUseCase.getOneTimeReceiveAddress(transparentToken)
        } ?: return null

        cachedZcashTransparentAddress = address
        return address
    }

    private fun getZCashTransparentToken() = marketKit.token(
        TokenQuery(
            BlockchainType.Zcash,
            TokenType.AddressSpecTyped(TokenType.AddressSpecType.Transparent)
        )
    )

    /**
     * ChangeNow does not support Unified an Shielded addresses as return address,
     */
    private fun isZCashUnifiedOrShielded(tokenIn: Token): Boolean =
        tokenIn.blockchainType == BlockchainType.Zcash &&
                (tokenIn.type == TokenType.AddressSpecTyped(TokenType.AddressSpecType.Unified) ||
                        tokenIn.type == TokenType.AddressSpecTyped(TokenType.AddressSpecType.Shielded))

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

                var refundAddress = walletUseCase.getReceiveAddress(tokenIn)
                // For ZCash unified or shielded we need to use transparent address as refund address
                if (isZCashUnifiedOrShielded(tokenIn)) {
                    getCachedZcashTransparentAddress()?.let {
                        refundAddress = it
                    } ?: throw IllegalStateException("Can't find ZCASH transparent wallet")
                }

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
                    refundAddress = refundAddress,
                    claimedDepositAmount = amountIn.toPlainString()
                )
                if (System.currentTimeMillis() - cacheUpdateTimestamp < CACHE_FINAL_QUOTE_DURATION &&
                    cachedFinalQuote?.first == request
                ) {
                    cachedFinalQuote?.second!!
                } else {
                    quickexRepository.createTransaction(
                        newTransactionRequest = request
                    ).also {
                        cachedFinalQuote = request to it
                        cacheUpdateTimestamp = System.currentTimeMillis()
                    }
                }
            } catch (e: Exception) {
                throw IllegalStateException("QuickexProvider: error fetchFinalQuote", e)
            }

            val fields = buildList {
                add(
                    DataFieldRecipientExtended(
                        address = Address(transaction.depositAddress.depositAddress),
                        blockchainType = tokenOut.blockchainType
                    )
                )
            }

            swapProviderTransaction = SwapProviderTransaction(
                date = System.currentTimeMillis(),
                outgoingRecordUid = null, //set later
                transactionId = transaction.orderId.toString(),
                status = TransactionStatusEnum.NEW.name.lowercase(),
                provider = SwapProvider.QUICKEX,
                coinUidIn = tokenIn.coin.uid,
                blockchainTypeIn = tokenIn.blockchainType.uid,
                amountIn = amountIn,
                addressIn = walletUseCase.getReceiveAddress(tokenIn),
                coinUidOut = tokenOut.coin.uid,
                blockchainTypeOut = tokenOut.blockchainType.uid,
                amountOut = transaction.amountToGet,
                addressOut = walletUseCase.getReceiveAddress(tokenOut)
            )

            SwapFinalQuoteEvm(
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn,
                amountOut = transaction.amountToGet,
                amountOutMin = transaction.amountToGet,
                sendTransactionData = buildTransactionData(
                    tokenIn = tokenIn,
                    amountIn = amountIn,
                    transaction = transaction
                ),
                priceImpact = null,
                fields = fields
            )
        }
    }

    private fun buildTransactionData(
        tokenIn: Token,
        amountIn: BigDecimal,
        transaction: NewTransactionQuickexResponse
    ): SendTransactionData {
        return when {
            tokenIn.blockchainType.isEvm -> {
                val adapterManager: IAdapterManager by KoinJavaComponent.inject(IAdapterManager::class.java)
                val adapter = adapterManager.getAdapterForToken<ISendEthereumAdapter>(tokenIn)
                    ?: throw IllegalStateException("Ethereum adapter not found")

                val transactionData =
                    adapter.getTransactionData(
                        amountIn,
                        io.horizontalsystems.ethereumkit.models.Address(transaction.depositAddress.depositAddress)
                    )
                SendTransactionData.Evm(transactionData, null, amount = amountIn)
            }

            tokenIn.blockchainType == BlockchainType.Tron -> {
                SendTransactionData.Tron.Regular(
                    amount = amountIn,
                    address = transaction.depositAddress.depositAddress
                )
            }

            tokenIn.blockchainType == BlockchainType.Stellar -> {
                SendTransactionData.Stellar.Regular(
                    amount = amountIn,
                    address = transaction.depositAddress.depositAddress,
                    memo = transaction.depositAddress.depositAddressMemo.orEmpty()
                )
            }

            tokenIn.blockchainType == BlockchainType.Solana -> {
                SendTransactionData.Solana.Regular(
                    amount = amountIn,
                    address = transaction.depositAddress.depositAddress
                )
            }

            tokenIn.blockchainType.isUtxoBased -> {
                SendTransactionData.Btc(
                    address = transaction.depositAddress.depositAddress,
                    memo = transaction.depositAddress.depositAddressMemo.orEmpty(),
                    amount = amountIn,
                    recommendedGasRate = null,
                    minimumSendAmount = null,
                    changeToFirstInput = false,
                    utxoFilters = UtxoFilters(),
                    feesMap = emptyMap()
                )
            }

            tokenIn.blockchainType == BlockchainType.Ton ->
                SendTransactionData.Ton(
                    amount = amountIn,
                    address = transaction.depositAddress.depositAddress,
                    memo = transaction.depositAddress.depositAddressMemo
                )

            tokenIn.blockchainType == BlockchainType.Monero ->
                SendTransactionData.Monero(
                    amount = amountIn,
                    address = transaction.depositAddress.depositAddress
                )

            else -> SendTransactionData.Unsupported
        }
    }

    fun onTransactionCompleted(result: SendTransactionResult) {
        swapProviderTransaction?.let {
            swapProviderTransaction = it.copy(
                outgoingRecordUid = result.getRecordUid(),
                date = System.currentTimeMillis()
            ).also { transactionWithRecordUid ->
                swapProviderTransactionsStorage.save(transactionWithRecordUid)
            }
        }
    }

    override fun getProviderTransactionId(): String? = swapProviderTransaction?.transactionId
}
