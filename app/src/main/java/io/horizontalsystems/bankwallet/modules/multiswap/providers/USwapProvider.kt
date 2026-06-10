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
import io.horizontalsystems.bankwallet.modules.multiswap.action.ISwapProviderAction
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

    // Some provider+tokenOut pairs fan a dry quote into multiple routes (currently: Exolix
    // returns both transparent and shielded ZEC). The dry call picks one and carries it on the
    // returned quote so the confirmation (non-dry) call re-quotes exactly that same route.
    private data class SelectedAlternateRoute(
        val buyAsset: String,
        val destinationAddress: String,
    )

    // SwapQuote variant that remembers the route the dry quote settled on, so fetchFinalQuote can
    // replay it. Kept local to USwapProvider since no other provider needs alternate routes.
    private class USwapQuote(
        amountOut: BigDecimal,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        actionRequired: ISwapProviderAction?,
        estimationTime: Long?,
        val selectedAlternateRoute: SelectedAlternateRoute?,
    ) : SwapQuote(amountOut, tokenIn, tokenOut, amountIn, actionRequired, estimationTime)

    private data class RouteSelection(
        val route: UnstoppableAPI.Response.Quote.Route,
        val selectedAlternateRoute: SelectedAlternateRoute?,
    )

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
        // For providers that fan a dry quote into multiple routes (Exolix ZEC-out), resolve both
        // the transparent and shielded destinations so the server can return both routes;
        // pickRoute then remembers the better one for the confirmation quote.
        var destinationAddress: String? = null
        var destinationAddressUnified: String? = null
        if (supportsAlternateRouteSelection(tokenOut)) {
            destinationAddress = SwapHelper.getReceiveAddressForToken(tokenOut)
            destinationAddressUnified = SwapHelper.getReceiveAddressUnifiedForZcash(tokenOut)
        }

        val routeSelection = quoteSwapBestRoute(
            tokenIn,
            tokenOut,
            amountIn,
            BigDecimal("1"),
            destinationAddress,
            destinationAddressUnified,
            null,
            null,
            null,
            true
        )
        val bestRoute = routeSelection.route

        val approvalAddress = bestRoute.meta?.approvalAddress?.let { router ->
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

        return USwapQuote(
            amountOut = bestRoute.expectedBuyAmount ?: BigDecimal.ZERO,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = actionApprove,
            estimationTime = bestRoute.estimatedTime?.total,
            selectedAlternateRoute = routeSelection.selectedAlternateRoute,
        )
    }

    private suspend fun quoteSwapBestRoute(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        destinationAddress: String?,
        destinationAddressUnified: String?,
        sourceAddress: String?,
        refundAddress: String?,
        buyAssetOverride: String?,
        dry: Boolean,
    ): RouteSelection {
        val usingDerivedIdentifiers = assetsMap.isEmpty()
        val assetIn = assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn")
        val assetOut = assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut")
        val chainId = if (usingDerivedIdentifiers) chainIdByBlockchainType[tokenIn.blockchainType] else null

        val quote = unstoppableAPI.quote(
            UnstoppableAPI.Request.Quote(
                sellAsset = assetIn,
                buyAsset = buyAssetOverride ?: assetOut,
                sellAmount = amountIn.toPlainString(),
                providers = setOf(provider.id),
                slippage = slippage,
                destinationAddress = destinationAddress,
                destinationAddressUnified = destinationAddressUnified,
                sourceAddress = sourceAddress,
                refundAddress = refundAddress,
                dry = dry,
                chainId = chainId,
            )
        )

        return pickRoute(
            routes = quote.routes,
            dry = dry,
            tokenOut = tokenOut,
            fallbackBuyAsset = assetOut,
            destinationAddress = destinationAddress,
            destinationAddressUnified = destinationAddressUnified,
        )
    }

    // True for provider+tokenOut pairs whose dry quote fans into multiple routes. Today only
    // Exolix's ZEC pair does (transparent + shielded); extend here if another provider splits.
    private fun supportsAlternateRouteSelection(tokenOut: Token): Boolean {
        return provider == UProvider.Exolix && tokenOut.blockchainType == BlockchainType.Zcash
    }

    private fun pickRoute(
        routes: List<UnstoppableAPI.Response.Quote.Route>,
        dry: Boolean,
        tokenOut: Token,
        fallbackBuyAsset: String,
        destinationAddress: String?,
        destinationAddressUnified: String?,
    ): RouteSelection {
        if (!dry || !supportsAlternateRouteSelection(tokenOut)) {
            return RouteSelection(routes.maxBy { it.expectedBuyAmount ?: BigDecimal.ZERO }, null)
        }

        // Exolix's ZEC dry quote can carry both the transparent and shielded routes. Pick the
        // better-priced one — preferring shielded on a tie, for privacy — and remember it so
        // the confirmation quote replays the same route.
        val best = routes.maxWithOrNull(
            compareBy<UnstoppableAPI.Response.Quote.Route> { it.expectedBuyAmount ?: BigDecimal.ZERO }
                .thenBy { if (it.buyAsset == ZCASH_SHIELDED_ASSET) 1 else 0 }
        ) ?: throw IllegalStateException("No routes")

        val selectedDestination = if (best.buyAsset == ZCASH_SHIELDED_ASSET) {
            destinationAddressUnified ?: destinationAddress
        } else {
            destinationAddress
        }

        val selectedAlternateRoute = selectedDestination?.let {
            SelectedAlternateRoute(
                buyAsset = best.buyAsset ?: fallbackBuyAsset,
                destinationAddress = it,
            )
        }

        return RouteSelection(best, selectedAlternateRoute)
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
        val sourceAddress = SwapHelper.getSendingAddressForToken(tokenIn)
        val refundAddress = SwapHelper.getReceiveAddressForToken(tokenIn)

        // When a dry quote previously fanned out into multiple routes, the confirmation quote must
        // re-request the exact (buyAsset, destination) the dry call settled on. An explicit
        // recipient overrides any selection.
        val selection = (swapQuote as? USwapQuote)?.selectedAlternateRoute?.takeIf {
            recipient == null && supportsAlternateRouteSelection(tokenOut)
        }
        val destination = selection?.destinationAddress
            ?: recipient?.hex
            ?: SwapHelper.getReceiveAddressForToken(tokenOut)

        val bestRoute = quoteSwapBestRoute(
            tokenIn,
            tokenOut,
            amountIn,
            slippage,
            destination,
            null,
            sourceAddress,
            refundAddress,
            selection?.buyAsset,
            false,
        ).route

        val amountOut = bestRoute.expectedBuyAmount ?: BigDecimal.ZERO

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
                bestRoute,
                tokenOut
            ),
            priceImpact = null,
            fields = fields,
            estimatedTime = bestRoute.estimatedTime?.total,
            slippage = slippage,
            providerSwapId = bestRoute.providerSwapId,
            fromAsset = assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn"),
            toAsset = assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut"),
            depositAddress = bestRoute.inboundAddress,
        )
    }

    private fun getSendTransactionData(
        tokenIn: Token,
        amountIn: BigDecimal,
        bestRoute: UnstoppableAPI.Response.Quote.Route,
        tokenOut: Token
    ): SendTransactionData {
        val blockchainType = tokenIn.blockchainType

        if (blockchainType.isEvm) {
            if (bestRoute.tx?.isJsonObject == true) {
                val jsonObject = bestRoute.tx.asJsonObject
                val transactionData = TransactionData(
                    to = Address(jsonObject["to"].asString),
                    value = BigInteger(jsonObject["value"].asString.stripHexPrefix(), 16),
                    input = (jsonObject["data"].asString).hexStringToByteArray()
                )

                return SendTransactionData.Evm(
                    transactionData = transactionData,
                    gasLimit = jsonObject["gas"]?.asString?.hexStringToByteArray()?.toLong(),
                )
            } else {
                throw IllegalStateException("No tx found")
            }
        }

        when (blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
            BlockchainType.ECash,
                -> {
                // supported only providers that accepts any type of outputs
                // providers with specific requirements like thorchain is not supported
                // if thorchain support needed then it should be handled separately
                val simpleBtcTransactionProviders = listOf(
                    UProvider.Near,
                    UProvider.QuickEx,
                    UProvider.LetsExchange,
                    UProvider.StealthEx,
                    UProvider.Exolix,
                    UProvider.Cce,
                    UProvider.Swapuz
                )

                if (!simpleBtcTransactionProviders.contains(provider)) {
                    throw IllegalStateException("Only simple BTC tx providers are supported")
                }

                return SendTransactionData.Btc(
                    address = bestRoute.inboundAddress,
                    memo = bestRoute.txExtraAttribute?.get("memo"),
                    amount = amountIn,
                    recommendedGasRate = null,
                    minimumSendAmount = null,
                    changeToFirstInput = false,
                    utxoFilters = UtxoFilters(),
                )
            }

            BlockchainType.Solana -> {
                if (bestRoute.tx?.isJsonPrimitive == true) {
                    return SendTransactionData.Solana.WithRawTransaction(
                        Base64.decode(
                            bestRoute.tx.asString,
                            Base64.DEFAULT
                        )
                    )
                } else {
                    throw IllegalStateException("No tx found")
                }
            }

            BlockchainType.Tron -> {
                if (bestRoute.tx != null) {
                    val rawTransaction = APIClient.gson.fromJson(
                        bestRoute.tx,
                        CreatedTransaction::class.java
                    )

                    return SendTransactionData.Tron.WithCreateTransaction(rawTransaction)
                } else {
                    throw IllegalStateException("No tx found")
                }
            }

            BlockchainType.Stellar -> {
                val memo = bestRoute.txExtraAttribute?.get("memo")
                    ?: throw IllegalStateException("No memo found")

                return SendTransactionData.Stellar.Regular(
                    address = bestRoute.inboundAddress,
                    memo = memo,
                    amount = amountIn
                )
            }

            BlockchainType.Ton -> {
                if (bestRoute.tx != null) {
                    return SendTransactionData.Ton.SendRequest(JSONObject(bestRoute.tx.toString()))
                } else {
                    throw IllegalStateException("No tx found")
                }
            }

            BlockchainType.Zcash -> {
                val simpleZcashTransactionProviders = listOf(
                    UProvider.Near,
                    UProvider.QuickEx,
                    UProvider.LetsExchange,
                    UProvider.StealthEx,
                    UProvider.Exolix,
                    UProvider.Cce,
                    UProvider.Swapuz
                )

                if (!simpleZcashTransactionProviders.contains(provider)) {
                    throw IllegalStateException("Only simple ZEC tx providers are supported")
                }

                return SendTransactionData.Zcash.Regular(
                    address = bestRoute.inboundAddress,
                    amount = amountIn,
                    memo = bestRoute.txExtraAttribute?.get("memo") ?: ""
                )
            }

            BlockchainType.Monero -> {
                val simpleMoneroTransactionProviders = listOf(
                    UProvider.Near,
                    UProvider.QuickEx,
                    UProvider.LetsExchange,
                    UProvider.StealthEx,
                    UProvider.Exolix,
                    UProvider.Cce,
                    UProvider.Swapuz
                )

                if (!simpleMoneroTransactionProviders.contains(provider)) {
                    throw IllegalStateException("Only simple XMR tx providers are supported")
                }

                return SendTransactionData.Monero(
                    address = bestRoute.inboundAddress,
                    amount = amountIn,
                    memo = bestRoute.txExtraAttribute?.get("memo")
                )
            }

            BlockchainType.Zano -> {
                val simpleZanoTransactionProviders = listOf(
                    UProvider.Near,
                    UProvider.QuickEx,
                    UProvider.LetsExchange,
                    UProvider.StealthEx,
                    UProvider.Exolix,
                    UProvider.Cce,
                    UProvider.Swapuz
                )

                if (!simpleZanoTransactionProviders.contains(provider)) {
                    throw IllegalStateException("Only simple ZANO tx providers are supported")
                }

                return SendTransactionData.Zano(
                    address = bestRoute.inboundAddress,
                    amount = amountIn,
                    memo = bestRoute.txExtraAttribute?.get("memo")
                )
            }

            else -> Unit
        }

        throw IllegalArgumentException("Not supported blockchainType: $blockchainType")
    }

    companion object {
        // Exolix's shielded Zcash route. Internal routing detail — the app always quotes ZEC.ZEC
        // and lets the server expand it into this shielded variant.
        private const val ZCASH_SHIELDED_ASSET = "ZEC.ZECSHIELDED"
    }
}

interface UnstoppableAPI {
    @GET("providers")
    suspend fun providers(): List<Response.Provider>

    @GET("tokens")
    suspend fun tokens(
        @Query("provider") provider: String
    ): Response.Tokens

    @POST("quote")
    suspend fun quote(
        @Body quote: Request.Quote,
    ): Response.Quote

    @POST("track")
    suspend fun track(
        @Body request: Request.Track,
    ): Response.Track

    @POST("track/evm")
    suspend fun trackEvm(
        @Body request: Request.Track,
    ): Response.Track

    @GET("quote/check-addresses")
    suspend fun checkAddresses(
        @Query("addresses") addresses: String,
    ): Response.CheckAddresses

    object Request {
        data class Quote(
            val sellAsset: String,
            val buyAsset: String,
            val sellAmount: String,
            val providers: Set<String>,
            val slippage: BigDecimal,
            val destinationAddress: String?,
            val destinationAddressUnified: String? = null,
            val sourceAddress: String?,
            val refundAddress: String?,
            val dry: Boolean,
            val chainId: String? = null,
        )

        data class Track(
            val provider: String,
            val hash: String? = null,
            val chainId: String? = null,
            val fromAsset: String? = null,
            val fromAddress: String? = null,
            val fromAmount: String? = null,
            val toAsset: String? = null,
            val toAddress: String? = null,
            val toAmount: String? = null,
            val depositAddress: String? = null,
            val providerSwapId: String? = null,
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

        data class Quote(
            val routes: List<Route>
        ) {
            data class Route(
                val expectedBuyAmount: BigDecimal?,
                val buyAsset: String?,
                val tx: JsonElement?,
                val inboundAddress: String,
                val memo: String?,
                val txExtraAttribute: Map<String, String>?,
                val estimatedTime: EstimatedTime?,
                val providerSwapId: String?,
                val meta: Meta?
            ) {
                data class EstimatedTime(
                    val total: Long
                )

                data class Meta(val approvalAddress: String)
            }
        }

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
