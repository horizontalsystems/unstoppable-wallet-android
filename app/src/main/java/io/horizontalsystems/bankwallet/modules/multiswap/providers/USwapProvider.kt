package io.horizontalsystems.bankwallet.modules.multiswap.providers

import android.util.Base64
import com.google.gson.JsonElement
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.derivation
import io.horizontalsystems.bankwallet.core.isEvm
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.nativeTokenQueries
import io.horizontalsystems.bankwallet.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.SwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldSlippage
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.ethereumkit.core.stripHexPrefix
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.spv.core.toLong
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.hexStringToByteArray
import io.horizontalsystems.tronkit.network.CreatedTransaction
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger

class USwapProvider(private val provider: UProvider) : IMultiSwapProvider {
    override val id = "u_${provider.id}"
    override val title = provider.title
    override val type = provider.type
    override val amlPrecheck = provider.amlPrecheck
    override val isEvm = provider.isEvm
    override val requireTerms = provider.requireTerms
    override val riskLevel = provider.riskLevel

    override fun isSingleTransactionSwap(tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String) = provider.isSingleTransactionSwap

    private val unstoppableAPI = APIClient.build(
        App.appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to App.appConfigProvider.uswapApiKey)
    ).create(UnstoppableAPI::class.java)

    private val blockchainTypes = mapOf(
        "43114" to BlockchainType.Avalanche,
        "10" to BlockchainType.Optimism,
        "8453" to BlockchainType.Base,
        "728126428" to BlockchainType.Tron,
        "42161" to BlockchainType.ArbitrumOne,
        "56" to BlockchainType.BinanceSmartChain,
        "solana" to BlockchainType.Solana,
        "137" to BlockchainType.Polygon,
        "bitcoin" to BlockchainType.Bitcoin,
        "1" to BlockchainType.Ethereum,
        "zcash" to BlockchainType.Zcash,
        "bitcoincash" to BlockchainType.BitcoinCash,
        "litecoin" to BlockchainType.Litecoin,
        "stellar" to BlockchainType.Stellar,
        "ton" to BlockchainType.Ton,
        "dash" to BlockchainType.Dash,
        "ecash" to BlockchainType.ECash,
        "monero" to BlockchainType.Monero,
        "zano" to BlockchainType.Zano,
        "100" to BlockchainType.Gnosis,
//        "" to BlockchainType.Fantom,
//        "" to BlockchainType.ZkSync,
    )

    private val chainIdByBlockchainType = blockchainTypes.entries.associate { (k, v) -> v to k }

    private var assetsMap = mapOf<Token, String>()
    private var supportedBlockchainTypes = setOf<BlockchainType>()

    private sealed class ProviderData {
        data class TokenMap(val map: Map<Token, String>) : ProviderData()
        data class ChainIds(val ids: List<String>) : ProviderData()
    }

    override suspend fun start() {
        SwapProviderCacheHelper.getCachedChainIds(id)?.let { chainIds ->
            supportedBlockchainTypes = chainIds.mapNotNull { blockchainTypes[it] }.toSet()
            return
        }

        SwapProviderCacheHelper.getCachedTokenMap(id) { it }?.let { map ->
            assetsMap = map
            return
        }

        when (val data = fetchProviderData()) {
            is ProviderData.TokenMap -> {
                assetsMap = data.map
                SwapProviderCacheHelper.saveTokenMap(id, data.map) { it }
            }
            is ProviderData.ChainIds -> {
                supportedBlockchainTypes = data.ids.mapNotNull { blockchainTypes[it] }.toSet()
                SwapProviderCacheHelper.saveChainIds(id, data.ids)
            }
        }
    }

    private suspend fun fetchProviderData(): ProviderData {
        val response = unstoppableAPI.tokens(provider.id)
        val tokens = response.tokens

        if (tokens.isEmpty()) {
            return ProviderData.ChainIds(response.supportedChainIds)
        }

        val assetsMap = mutableMapOf<Token, String>()
        for (token in tokens) {
            // ZEC.ZECSHIELDED is an internal Exolix routing variant. The app always quotes
            // ZEC.ZEC and lets the server expand it into the shielded route, so skip it here
            // to keep the Zcash native token mapping deterministic.
            if (token.identifier == ZCASH_SHIELDED_ASSET) continue

            val blockchainType = blockchainTypes[token.chainId] ?: continue

            when (blockchainType) {
                BlockchainType.ArbitrumOne,
                BlockchainType.Avalanche,
                BlockchainType.Base,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Ethereum,
                BlockchainType.Optimism,
                BlockchainType.Polygon,
                BlockchainType.Tron,
                BlockchainType.Fantom,
                BlockchainType.Gnosis,
                BlockchainType.ZkSync,
                    -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.Eip20(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.Litecoin,
                BlockchainType.Zcash,
                BlockchainType.Dash,
                BlockchainType.ECash,
                    -> {
                    var nativeTokenQueries = blockchainType.nativeTokenQueries

                    // filter out taproot for ltc
                    if (blockchainType == BlockchainType.Litecoin) {
                        nativeTokenQueries = nativeTokenQueries.filterNot {
                            it.tokenType.derivation == TokenType.Derivation.Bip86
                        }
                    }

                    val tokens = App.marketKit.tokens(nativeTokenQueries)
                    tokens.forEach {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Solana -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.Spl(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Stellar -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        null
                    } else {
                        TokenType.Native
                    }

                    tokenType?.let {
                        App.marketKit.token(TokenQuery(blockchainType, it))
                    }?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Ton -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.Jetton(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Monero -> {
                    App.marketKit.token(TokenQuery(blockchainType, TokenType.Native))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                BlockchainType.Zano -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.ZanoAsset(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        assetsMap[it] = token.identifier
                    }
                }

                is BlockchainType.Unsupported -> Unit
            }
        }

        return ProviderData.TokenMap(assetsMap)
    }

    override fun supports(blockchainType: BlockchainType): Boolean {
        // overriding fun supports(tokenFrom: Token, tokenTo: Token) makes this method redundant
        return true
    }

    override fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        return if (assetsMap.isNotEmpty()) {
            assetsMap.contains(tokenFrom) && assetsMap.contains(tokenTo)
        } else {
            tokenFrom.blockchainType in supportedBlockchainTypes &&
                    tokenTo.blockchainType in supportedBlockchainTypes
        }
    }

    private fun deriveIdentifier(token: Token): String? = when (val type = token.type) {
        is TokenType.Eip20 -> type.address
        TokenType.Native if token.blockchainType.isEvm -> "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
        else -> null
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): SwapQuote {
        val bestRoute = rateBestRoute(tokenIn, tokenOut, amountIn, BigDecimal("1"))

        val approvalAddress = bestRoute.approvalSpenderOrExecution?.let { router ->
            try {
                Address(router)
            } catch (_: Throwable) {
                null
            }
        }

        val allowance = approvalAddress?.let { EvmSwapHelper.getAllowance(tokenIn, it) }
        val actionApprove = approvalAddress?.let {
            EvmSwapHelper.actionApprove(allowance, amountIn, it, tokenIn)
        }

        return SwapQuote(
            amountOut = bestRoute.expectedBuyAmountOrZero,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = actionApprove,
            estimationTime = bestRoute.estimatedTime?.total,
            extraData = SwapQuoteExtraData(bestRoute)
        )
    }

    // /v2/rate — read-only price/route comparison narrowed to this provider. Picks the route
    // with the best expectedBuyAmount. On an Exolix ZEC pair it additionally fans out a
    // shielded (ZEC.ZECSHIELDED) variant and keeps the better-priced one; the winning
    // sell/buy asset travels back on the route so commitSwap can replay the exact variant.
    private suspend fun rateBestRoute(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
    ): UnstoppableAPI.Response.Route {
        val usingDerivedIdentifiers = assetsMap.isEmpty()
        val assetIn = assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn")
        val assetOut = assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut")
        val chainId = if (usingDerivedIdentifiers) chainIdByBlockchainType[tokenIn.blockchainType] else null

        val request = UnstoppableAPI.Request.Rate(
            sellAsset = assetIn,
            buyAsset = assetOut,
            sellAmount = amountIn.toPlainString(),
            slippage = slippage,
            providers = setOf(provider.id),
            chainId = chainId,
        )
        var bestRoute = unstoppableAPI.rate(request).routes.maxBy { it.expectedBuyAmountOrZero }

        if (provider == UProvider.Exolix) {
            val requestAlternate = when {
                tokenIn.blockchainType == BlockchainType.Zcash -> {
                    request.copy(sellAsset = ZCASH_SHIELDED_ASSET)
                }

                tokenOut.blockchainType == BlockchainType.Zcash -> {
                    request.copy(buyAsset = ZCASH_SHIELDED_ASSET)
                }

                else -> null
            }

            if (requestAlternate != null) {
                try {
                    val bestRouteAlternate = unstoppableAPI.rate(requestAlternate).routes.maxBy { it.expectedBuyAmountOrZero }
                    if (bestRouteAlternate.expectedBuyAmountOrZero >= bestRoute.expectedBuyAmountOrZero) {
                        bestRoute = bestRouteAlternate
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {

                }
            }
        }

        return bestRoute
    }

    // /v2/swap — commits the order with this single provider and returns the executable
    // route (execution + uuid). `sellAsset`/`buyAsset` replay the variant the rate quote
    // chose (Exolix ZEC). A committed route with no uuid can't be tracked, so reject it
    // before the user sends funds rather than create an untrackable swap.
    private suspend fun commitSwap(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        destinationAddress: String,
        sourceAddress: String?,
        refundAddress: String?,
        sellAsset: String?,
        buyAsset: String?,
    ): UnstoppableAPI.Response.Route {
        val usingDerivedIdentifiers = assetsMap.isEmpty()
        val assetIn = sellAsset ?: assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn")
        val assetOut = buyAsset ?: assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut")
        val chainId = if (usingDerivedIdentifiers) chainIdByBlockchainType[tokenIn.blockchainType] else null

        val request = UnstoppableAPI.Request.Swap(
            sellAsset = assetIn,
            buyAsset = assetOut,
            sellAmount = amountIn.toPlainString(),
            slippage = slippage,
            provider = provider.id,
            destinationAddress = destinationAddress,
            refundAddress = refundAddress,
            sourceAddress = sourceAddress,
            chainId = chainId,
        )
        val route = unstoppableAPI.swap(request)

        if (route.uuid.isNullOrEmpty()) {
            throw IllegalStateException("Swap is not trackable (no uuid)")
        }

        return route
    }

    override suspend fun checkAmlAddresses(addresses: List<String>): Boolean? {
        return unstoppableAPI.checkAddresses(addresses.joinToString(",")).passedAmlCheck
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: io.horizontalsystems.bankwallet.entities.Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote {
        val selectedRoute = (swapQuote.extraData as? SwapQuoteExtraData)?.route
        val destination = when {
            recipient != null -> recipient.hex

            selectedRoute?.buyAsset == ZCASH_SHIELDED_ASSET -> {
                SwapHelper.getReceiveAddressUnifiedForZcash(tokenOut)
            }

            else -> SwapHelper.getReceiveAddressForToken(tokenOut)
        }

        // sourceAddress is the build signal — send it only for chains whose server-built tx
        // we actually consume (EVM/Tron/TON/Solana), where it is also required for the
        // signed_transaction `from`. For UTXO/Zcash/Monero/Zano/Stellar we omit it and build
        // the transfer ourselves (e.g. multi-UTXO sweeps).
        val sourceAddress = if (tokenIn.needsServerBuiltTx) {
            SwapHelper.getSendingAddressForToken(tokenIn)
        } else {
            null
        }
        val refundAddress = SwapHelper.getReceiveAddressForToken(tokenIn)

        val bestRoute = commitSwap(
            tokenIn,
            tokenOut,
            amountIn,
            slippage,
            destination,
            sourceAddress,
            refundAddress,
            selectedRoute?.sellAsset,
            selectedRoute?.buyAsset
        )

        val amountOut = bestRoute.expectedBuyAmountOrZero

        val amountOutMin = amountOut.subtract(amountOut.multiply(slippage.movePointLeft(2)))

        val fields = buildList {
            recipient?.let {
                add(DataFieldRecipient(it))
            }
            DataFieldSlippage.getField(slippage)?.let {
                add(it)
            }
        }

        return SwapFinalQuote(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = amountOut,
            amountOutMin = amountOutMin,
            sendTransactionData = getSendTransactionData(
                tokenIn,
                amountIn,
                bestRoute
            ),
            priceImpact = null,
            fields = fields,
            estimatedTime = bestRoute.estimatedTime?.total,
            slippage = slippage,
            providerSwapId = bestRoute.uuid,
            fromAsset = assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn"),
            toAsset = assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut"),
            depositAddress = bestRoute.execution?.resolvedDepositAddress(),
        )
    }

    private fun getSendTransactionData(
        tokenIn: Token,
        amountIn: BigDecimal,
        bestRoute: UnstoppableAPI.Response.Route,
    ): SendTransactionData {
        val blockchainType = tokenIn.blockchainType
        val execution = bestRoute.execution ?: throw IllegalStateException("No execution found")

        if (blockchainType.isEvm) {
            val signable = execution.primarySignable?.takeIf { it.kind == "evm" }
                ?: throw IllegalStateException("No evm tx found")

            val transactionData = TransactionData(
                to = Address(signable.to ?: throw IllegalStateException("No tx `to`")),
                value = BigInteger((signable.value ?: "0x0").stripHexPrefix(), 16),
                input = (signable.data ?: "0x").hexStringToByteArray()
            )

            return SendTransactionData.Evm(
                transactionData = transactionData,
                gasLimit = signable.gas?.hexStringToByteArray()?.toLong(),
            )
        }

        when (blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
            BlockchainType.ECash,
                -> {
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple BTC tx providers are supported")
                }

                return SendTransactionData.Btc(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    memo = execution.resolvedMemo(),
                    amount = amountIn,
                    recommendedGasRate = null,
                    minimumSendAmount = null,
                    changeToFirstInput = false,
                    utxoFilters = UtxoFilters(),
                )
            }

            BlockchainType.Solana -> {
                val message = execution.primarySignable?.takeIf { it.kind == "solana" }?.message
                    ?: throw IllegalStateException("No solana tx found")

                return SendTransactionData.Solana.WithRawTransaction(
                    Base64.decode(message, Base64.DEFAULT)
                )
            }

            BlockchainType.Tron -> {
                val tx = execution.primarySignable?.takeIf { it.kind == "tron" }?.tx
                    ?: throw IllegalStateException("No tron tx found")

                val rawTransaction = APIClient.gson.fromJson(tx, CreatedTransaction::class.java)
                return SendTransactionData.Tron.WithCreateTransaction(rawTransaction)
            }

            BlockchainType.Stellar -> {
                val memo = execution.resolvedMemo()
                    ?: throw IllegalStateException("No memo found")

                return SendTransactionData.Stellar.Regular(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    memo = memo,
                    amount = amountIn
                )
            }

            BlockchainType.Ton -> {
                val tx = execution.primarySignable?.takeIf { it.kind == "ton" }?.tx
                    ?: throw IllegalStateException("No ton tx found")

                return SendTransactionData.Ton.SendRequest(JSONObject(tx.toString()))
            }

            BlockchainType.Zcash -> {
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple ZEC tx providers are supported")
                }

                return SendTransactionData.Zcash.Regular(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = execution.resolvedMemo() ?: ""
                )
            }

            BlockchainType.Monero -> {
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple XMR tx providers are supported")
                }

                return SendTransactionData.Monero(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = execution.resolvedMemo()
                )
            }

            BlockchainType.Zano -> {
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple ZANO tx providers are supported")
                }

                return SendTransactionData.Zano(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = execution.resolvedMemo()
                )
            }

            else -> Unit
        }

        throw IllegalArgumentException("Not supported blockchainType: $blockchainType")
    }

    // Chains where we consume the server-built tx from `execution` (so we send sourceAddress
    // on /v2/swap). Everything else builds its own transfer to the deposit address.
    private val Token.needsServerBuiltTx: Boolean
        get() = blockchainType.isEvm || blockchainType in setOf(
            BlockchainType.Tron,
            BlockchainType.Ton,
            BlockchainType.Solana,
        )

    companion object {
        // Exolix's shielded Zcash route. Internal routing detail — the app always quotes ZEC.ZEC
        // and lets the server expand it into this shielded variant.
        private const val ZCASH_SHIELDED_ASSET = "ZEC.ZECSHIELDED"
    }

    data class SwapQuoteExtraData(val route: UnstoppableAPI.Response.Route) : SwapQuote.ExtraData
}

interface UnstoppableAPI {
    @GET("providers")
    suspend fun providers(): List<Response.Provider>

    @GET("tokens")
    suspend fun tokens(
        @Query("provider") provider: String
    ): Response.Tokens

    // /v2/rate — read-only, prices the swap across the requested providers. Returns
    // { routes: [...] } with economics only — no execution, no uuid.
    @POST("rate")
    suspend fun rate(
        @Body request: Request.Rate,
    ): Response.Rate

    // /v2/swap — commits against ONE provider. Creates the order and returns the single
    // executable route DIRECTLY (no { routes } wrapper), now carrying execution + uuid.
    @POST("swap")
    suspend fun swap(
        @Body request: Request.Swap,
    ): Response.Route

    // /v2/track — our recorded swaps, tracked by the route's uuid alone (the server resolves
    // the provider and every swap detail from the record).
    @POST("track")
    suspend fun track(
        @Body request: Request.Track,
    ): Response.Track

    // /v2/track/evm — stateless on-chain reader for native EVM swaps (1inch/Uniswap/Pancake)
    // that were not created through /v2/swap, so there is no record to look up by uuid.
    @POST("track/evm")
    suspend fun trackEvm(
        @Body request: Request.Track,
    ): Response.Track

    // /v2/track/thorchain — stateless on-chain reader for native THORChain/Mayachain swaps.
    @POST("track/thorchain")
    suspend fun trackThorchain(
        @Body request: Request.Track,
    ): Response.Track

    @GET("check-addresses")
    suspend fun checkAddresses(
        @Query("addresses") addresses: String,
    ): Response.CheckAddresses

    object Request {
        // /v2/rate request — compare routes; narrow the fan-out to a single provider.
        data class Rate(
            val sellAsset: String,
            val buyAsset: String,
            val sellAmount: String,
            val slippage: BigDecimal,
            val providers: Set<String>,
            val chainId: String? = null,
        )

        // /v2/swap request — commit with the single provider. `sourceAddress` is the build
        // signal: supply it and the server returns a ready-to-sign tx; omit it and we build
        // the tx ourselves (UTXO/Zcash/Monero/Zano/Stellar).
        data class Swap(
            val sellAsset: String,
            val buyAsset: String,
            val sellAmount: String,
            val slippage: BigDecimal,
            val provider: String,
            val destinationAddress: String,
            val refundAddress: String? = null,
            val sourceAddress: String? = null,
            val chainId: String? = null,
        )

        data class Track(
            // Recorded swaps (/v2/track): the route's uuid resolves provider + all details.
            val uuid: String? = null,
            // Broadcast tx hash — required for DEX swaps, harmless for P2P/NEAR.
            val inboundTxHash: String? = null,
            // Stateless readers (/v2/track/evm, /v2/track/thorchain) carry full context.
            val provider: String? = null,
            val hash: String? = null,
            val chainId: String? = null,
            val fromAsset: String? = null,
            val fromAddress: String? = null,
            val fromAmount: String? = null,
            val toAsset: String? = null,
            val toAddress: String? = null,
            val toAmount: String? = null,
            val depositAddress: String? = null,
            // Debug-only: forces the server to return an action_required swap. Null in release builds.
            val testActionRequired: Boolean? = null,
        )
    }

    object Response {
        data class Provider(
            val provider: String,
            val name: String? = null,
            val supportedChainIds: List<String> = emptyList(),
            val amlPolicy: String? = null,
            val amlPolicyDescription: String? = null,
            val contacts: Contacts? = null,
        ) {
            data class Contacts(
                val email: String? = null,
                val telegram: String? = null,
                val twitter: String? = null,
                val website: String? = null,
            )
        }

        data class Tokens(
            val tokens: List<Token>,
            val supportedChainIds: List<String> = emptyList(),
        )

        data class Token(
            val chain: String,
            val chainId: String,
            val address: String?,
            val identifier: String,
        )

        // /v2/rate response — a list of routes to compare. Each route carries economics
        // only (no execution, no uuid — those appear after committing with /v2/swap).
        data class Rate(
            val routes: List<Route>
        )

        // A single route. From /rate it is economics-only; from /swap it additionally
        // carries an `execution` block and a top-level `uuid` tracking handle.
        data class Route(
            val sellAsset: String?,
            val buyAsset: String?,
            val expectedBuyAmount: BigDecimal?,
            val estimatedTime: EstimatedTime?,
            // EVM ERC20 spender to approve before swapping (1inch/Barter/Circle). On a rate
            // route it is top-level; on a committed route it rides execution.approval.spender.
            val approvalSpender: String?,
            // Present only on a committed (/v2/swap) route — tells you how to send funds.
            val execution: Execution?,
            // v2 tracking handle (swap_records.uuid), top-level on the committed response.
            val uuid: String?,
        ) {
            // should be getter, otherwise it will be null when restored from json
            val expectedBuyAmountOrZero: BigDecimal
                get() = expectedBuyAmount ?: BigDecimal.ZERO

            // The ERC20 spender used to compute allowance, wherever it lives on the route.
            val approvalSpenderOrExecution: String?
                get() = approvalSpender ?: execution?.approvalSpender

            data class EstimatedTime(
                val total: Long
            )
        }

        // /v2 `execution` discriminated union — switch on `method`. Modeled as one flat
        // class (Gson-friendly) with accessors that read only the fields the method uses.
        data class Execution(
            val method: String,        // signed_transaction | transfer | thorchain_deposit
            val chain: String?,
            // signed_transaction
            val transactions: List<SignableTx>?,
            val approval: Approval?,
            // transfer
            val depositAddress: String?,
            val amount: String?,
            val asset: String?,
            val attachment: Attachment?,
            val unsignedTx: SignableTx?,
            // thorchain_deposit
            val protocol: String?,
            val inboundAddress: String?,
            val memo: String?,
            val delivery: Delivery?,
        ) {
            // The single tx a client signs, if any: signed_transaction's first, or the
            // optional unsignedTx on transfer / thorchain delivery (sent only when we
            // supplied a sourceAddress).
            val primarySignable: SignableTx?
                get() = when (method) {
                    "signed_transaction" -> transactions?.firstOrNull()
                    "transfer" -> unsignedTx
                    "thorchain_deposit" -> delivery?.unsignedTx
                    else -> null
                }

            // The deposit address for the address-transfer methods. signed_transaction is
            // tx-only and has none.
            fun resolvedDepositAddress(): String? = when (method) {
                "transfer" -> depositAddress
                "thorchain_deposit" -> inboundAddress
                else -> null
            }

            // The binding memo for an address transfer — the order identifier the provider
            // uses to credit the deposit, which we must echo back or the funds are lost.
            // Every chain we build a transfer for here (Stellar/Zcash/Monero/Zano/UTXO) puts
            // it in the memo field, whether the server typed it `text` (RUNE/GAIA/TON/NEAR)
            // or `destination_tag` (a numeric tag, e.g. a Stellar memo-id), so accept both.
            // The dedicated XRP destination-tag path (where the tag is a separate tx field,
            // not a memo) is not built by this provider, so this can't misroute one.
            fun resolvedMemo(): String? = when (method) {
                "thorchain_deposit" -> memo
                "transfer" -> attachment?.value
                else -> null
            }

            val approvalSpender: String?
                get() = when (method) {
                    "signed_transaction" -> approval?.spender
                    "thorchain_deposit" -> delivery?.approval?.spender
                    else -> null
                }
        }

        // A signable transaction the server built. `kind` tags the shape; each per-chain
        // builder reads the matching field (evm: to/value/data/gas; solana: message;
        // stellar: xdr; utxo: psbt; tron/ton/cosmos/ripple/near: tx).
        data class SignableTx(
            val kind: String,
            // evm
            val to: String?,
            val from: String?,
            val value: String?,
            val data: String?,
            val gas: String?,
            val gasPrice: String?,
            // base64 forms
            val psbt: String?,
            val message: String?,
            val xdr: String?,
            // object forms (cosmos / ripple / ton / tron / near)
            val tx: JsonElement?,
        )

        data class Approval(
            val token: String?,
            val spender: String,
            val amount: String?,
        )

        // transfer.attachment — an order identifier the provider uses to credit the deposit.
        data class Attachment(
            val type: String,   // destination_tag | text
            val value: String,
        )

        // thorchain_deposit.delivery — chain-specific memo binding.
        data class Delivery(
            val kind: String,   // evm_contract_call | utxo_op_return | cosmos_memo
            val router: String?,
            val approval: Approval?,
            val shieldedMemoAddress: String?,
            val unsignedTx: SignableTx?,
        )

        data class CheckAddresses(
            val passedAmlCheck: Boolean?,
            val results: List<AddressResult>,
        ) {
            data class AddressResult(
                val address: String,
                val passed: Boolean,
                val completed: Boolean,
                val error: String? = null,
            )
        }

        data class Track(
            val status: String, // not_started, pending, swapping, completed, refunded, unknown, failed, action_required
            val type: String?,
            val hash: String?,
            val chainId: String?,
            val fromAsset: String?,
            val fromAmount: String?,
            val fromAddress: String?,
            val toAsset: String?,
            val toAmount: String?,
            val toAddress: String?,
            val legs: List<Leg>?,
            val meta: Meta? = null,
        ) {
            data class Leg(
                val type: String,   // "swap" | "native_send"
                val status: String,
                val hash: String?,
                val chainId: String?,
                val fromAsset: String?,
                val fromAmount: String?,
                val fromAddress: String?,
                val toAsset: String?,
                val toAmount: String?,
                val toAddress: String?,
            )

            data class Meta(
                val provider: String?,
                val pauseReason: String?, // "overdue_with_funds" | "aml" | "frozen"
            )
        }
    }
}
