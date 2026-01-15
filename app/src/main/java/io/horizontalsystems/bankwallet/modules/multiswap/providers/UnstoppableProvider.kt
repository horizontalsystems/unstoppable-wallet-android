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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger

class UnstoppableProvider(private val provider: UProvider) : IMultiSwapProvider {
    override val id = "u_${provider.id}"
    override val title = provider.title
    override val icon = provider.icon
    override val priority = 0

    private val unstoppableAPI = APIClient.build(
        App.appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to App.appConfigProvider.uswapApiKey)
    ).create(UnstoppableAPI::class.java)

    private val blockchainTypes = mapOf(
        //AVAX:43114
        "43114" to BlockchainType.Avalanche,
        //OP:10
        "10" to BlockchainType.Optimism,
        //BASE:8453
        "8453" to BlockchainType.Base,
        //TRON:728126428
        "728126428" to BlockchainType.Tron,
        //ARB:42161
        "42161" to BlockchainType.ArbitrumOne,
        //BSC:56
        "56" to BlockchainType.BinanceSmartChain,
        //BERA:80094
        "80094" to null,
        //SOL:solana
        "solana" to BlockchainType.Solana,
        //POL:137
        "137" to BlockchainType.Polygon,
        //XRP:ripple
        "ripple" to null,
        //DOGE:dogecoin
        "dogecoin" to null,
        //GNO:100
        "100" to null,
        //BTC:bitcoin
        "bitcoin" to BlockchainType.Bitcoin,
        //ETH:1
        "1" to BlockchainType.Ethereum,
        //ZEC:zcash
        "zcash" to BlockchainType.Zcash,
        //NEAR:near
        "near" to null,
        //BCH:bitcoincash
        "bitcoincash" to BlockchainType.BitcoinCash,
        //GAIA:cosmoshub-4
        "cosmoshub-4" to null,
        //LTC:litecoin
        "litecoin" to BlockchainType.Litecoin,
        //THOR:thorchain-1
        "thorchain-1" to null,
        "stellar" to BlockchainType.Stellar
    )

    private val assetsMap = mutableMapOf<Token, String>()

    override suspend fun start() {
        val tokens = unstoppableAPI.tokens(provider.id).tokens
        for (token in tokens) {
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
                    -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.Eip20(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        registerAsset(it, token.identifier)
                    }
                }

                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.Litecoin,
                BlockchainType.Zcash,
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
                        registerAsset(it, token.identifier)
                    }
                }

                BlockchainType.Solana -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.Spl(token.address)
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        registerAsset(it, token.identifier)
                    }
                }

                BlockchainType.Stellar -> {
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TODO()
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                        registerAsset(it, token.identifier)
                    }
                }

                BlockchainType.Dash -> TODO()
                BlockchainType.ECash -> TODO()
                BlockchainType.Fantom -> TODO()
                BlockchainType.Gnosis -> TODO()
                BlockchainType.Monero -> TODO()
                BlockchainType.Ton -> TODO()
                is BlockchainType.Unsupported -> TODO()
                BlockchainType.ZkSync -> TODO()
            }
        }
    }

    private fun registerAsset(token: Token, identifier: String) {
        assetsMap[token] = identifier
    }

    override fun supports(blockchainType: BlockchainType): Boolean {
        // overriding fun supports(tokenFrom: Token, tokenTo: Token) makes this method redundant
        return true
    }

    override fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        return assetsMap.contains(tokenFrom) && assetsMap.contains(tokenTo)
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): SwapQuote {
        val bestRoute = quoteSwapBestRoute(
            tokenIn,
            tokenOut,
            amountIn,
            BigDecimal("1"),
            null,
            true
        )

        val approvalAddress = bestRoute.approvalAddress?.let { router ->
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
            amountOut = bestRoute.expectedBuyAmount ?: BigDecimal.ZERO,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = actionApprove
        )
    }

    private suspend fun quoteSwapBestRoute(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        recipient: io.horizontalsystems.bankwallet.entities.Address?,
        dry: Boolean
    ): UnstoppableAPI.Response.Quote.Route {
        val assetIn = assetsMap[tokenIn]!!
        val assetOut = assetsMap[tokenOut]!!
        val destination = recipient?.hex ?: SwapHelper.getReceiveAddressForToken(tokenOut)

        val quote = unstoppableAPI.quote(
            UnstoppableAPI.Request.Quote(
                sellAsset = assetIn,
                buyAsset = assetOut,
                sellAmount = amountIn.toPlainString(),
                providers = setOf(provider.id),
                slippage = slippage.toInt(),
                destinationAddress = destination,
                sourceAddress = SwapHelper.getSendingAddressForToken(tokenIn),
                refundAddress = SwapHelper.getReceiveAddressForToken(tokenIn),
                dry = dry
            )
        )

        return quote.routes.maxBy { it.expectedBuyAmount ?: BigDecimal.ZERO }
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
        val bestRoute = quoteSwapBestRoute(
            tokenIn,
            tokenOut,
            amountIn,
            slippage,
            recipient,
            false
        )

        val amountOut = bestRoute.expectedBuyAmount ?: BigDecimal.ZERO

        val amountOutMin = amountOut.subtract(amountOut.multiply(slippage.movePointLeft(2)))

        val fields = buildList {
            recipient?.let {
                add(DataFieldRecipient(it))
            }
            add(DataFieldSlippage(slippage))
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
                    feesMap = mapOf()
                )
            } else {
                throw IllegalStateException("No tx found")
            }
        }

        when (blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin -> {
                // supported only providers that accepts any type of outputs
                // providers with specific requirements like thorchain is not supported
                // if thorchain support needed then it should be handled separately
                val simpleBtcTransactionProviders = listOf(
                    UProvider.Near,
                    UProvider.QuickEx,
                    UProvider.LetsExchange,
                    UProvider.StealthEx,
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
                    feesMap = mapOf()
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

            BlockchainType.Zcash -> {
                val simpleZcashTransactionProviders = listOf(
                    UProvider.Near,
                    UProvider.QuickEx,
                    UProvider.LetsExchange,
                    UProvider.StealthEx,
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

            else -> Unit
        }

        throw IllegalArgumentException("Not supported blockchainType: $blockchainType")
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

    object Request {
        data class Quote(
            val sellAsset: String,
            val buyAsset: String,
            val sellAmount: String,
            val providers: Set<String>,
            val slippage: Int,
            val destinationAddress: String,
            val sourceAddress: String?,
            val refundAddress: String,
            val dry: Boolean,
        )
    }

    object Response {
        data class Provider(
            val provider: String
        )

        data class Tokens(
            val tokens: List<Token>
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
                val approvalAddress: String?,
                val tx: JsonElement?,
                val inboundAddress: String,
                val memo: String?,
                val txExtraAttribute: Map<String, String>?,
            )
        }
    }
}
