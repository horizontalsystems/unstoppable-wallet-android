package io.horizontalsystems.bankwallet.modules.multiswap.providers

import android.util.Base64
import com.google.gson.JsonElement
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.derivation
import io.horizontalsystems.bankwallet.core.isEvm
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.nativeTokenQueries
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.SwapFinalQuoteThorChain
import io.horizontalsystems.bankwallet.modules.multiswap.SwapQuoteThorChain
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldAllowance
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipientExtended
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldSlippage
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

object UnstoppableProvider : IMultiSwapProvider {
    override val id = "unstoppable"
    override val title = "Unstoppable"
    override val icon = R.drawable.unstoppable
    override val priority = 0

    private val unstoppableAPI = APIClient.build(
        "https://swap-api.unstoppable.money/",
        mapOf("x-api-key" to "79a24bddb8b1768dbb2662e136aca9006baa6d4e3e6d761219b2ab4279a42bb4")
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
        "thorchain-1" to null
    )

    private var assetsMap = mapOf<Token, Asset>()

    override suspend fun start() {
        val providers = unstoppableAPI.providers().map { it.provider }

        providers.forEach { provider ->
            val tokens = unstoppableAPI.tokens(provider).tokens
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
                            registerAsset(it, token.identifier, provider)
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
                            registerAsset(it, token.identifier, provider)
                        }
                    }

                    BlockchainType.Solana -> {
                        val tokenType = if (!token.address.isNullOrBlank()) {
                            TokenType.Spl(token.address)
                        } else {
                            TokenType.Native
                        }

                        App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                            registerAsset(it, token.identifier, provider)
                        }
                    }
                    else -> null
                }
            }
        }
    }

    private fun registerAsset(
        token: Token,
        identifier: String,
        provider: String
    ) {
        val providers = mutableListOf(provider)

        assetsMap[token]?.let {
            providers.addAll(it.providers)
        }

        assetsMap = buildMap {
            putAll(assetsMap)
            put(token, Asset(identifier, providers))
        }
    }

    override fun supports(blockchainType: BlockchainType): Boolean {
        // overriding fun supports(tokenFrom: Token, tokenTo: Token) makes this method redundant
        return true
    }

    override fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        val sendNotSupported = listOf(
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
            BlockchainType.Zcash
        )

        if (sendNotSupported.contains(tokenFrom.blockchainType)) return false

        val tokenFromProviders = assetsMap[tokenFrom]?.providers ?: return false
        val tokenToProviders = assetsMap[tokenTo]?.providers ?: return false

        return tokenFromProviders.intersect(tokenToProviders).isNotEmpty()
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>,
    ): ISwapQuote {
        val settingRecipient = SwapSettingRecipient(settings, tokenOut)
        val settingSlippage = SwapSettingSlippage(settings, BigDecimal("1"))

        val bestRoute = quoteSwapBestRoute(
            tokenIn,
            tokenOut,
            amountIn,
            settingSlippage.value,
            settingRecipient.value,
            false
        )

        val cautions = mutableListOf<HSCaution>()

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

        val fields = buildList {
            settingRecipient.value?.let {
                add(DataFieldRecipient(it))
            }
            add(DataFieldSlippage(settingSlippage.value))
            if (allowance != null && allowance < amountIn) {
                add(DataFieldAllowance(allowance, tokenIn))
            }
        }

        return SwapQuoteThorChain(
            amountOut = bestRoute.expectedBuyAmount ?: BigDecimal.ZERO,
            priceImpact = null,
            fields = fields,
            settings = listOf(settingRecipient, settingSlippage),
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = actionApprove,
            cautions = cautions
        )
    }

    private suspend fun quoteSwapBestRoute(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        recipient: io.horizontalsystems.bankwallet.entities.Address?,
        includeTx: Boolean
    ): UnstoppableAPI.Response.Quote.Route {
        val assetIn = assetsMap[tokenIn]!!
        val assetOut = assetsMap[tokenOut]!!
        val destination = recipient?.hex ?: SwapHelper.getReceiveAddressForToken(tokenOut)

        val sourceAddress = if (includeTx) {
            SwapHelper.getSendingAddressForToken(tokenIn)
        } else {
            // In this case only NEAR needs sourceAddress. It uses it as refundAddress
            // refundAddress is not checked for its balance, so it can be any valid user address
            // So getReceiveAddressForToken used since it gives non null address compared to getSendingAddressForToken
            SwapHelper.getReceiveAddressForToken(tokenIn)
        }

        val quote = unstoppableAPI.quote(
            UnstoppableAPI.Request.Quote(
                sellAsset = assetIn.identifier,
                buyAsset = assetOut.identifier,
                sellAmount = amountIn.toPlainString(),
                providers = assetIn.providers.intersect(assetOut.providers),
                slippage = slippage.toInt(),
                destinationAddress = destination,
                sourceAddress = sourceAddress,
                includeTx = includeTx
            )
        )

        return quote.routes.maxBy { it.expectedBuyAmount ?: BigDecimal.ZERO }
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: ISwapQuote,
    ): ISwapFinalQuote {
        check(swapQuote is SwapQuoteThorChain)

        val settingRecipient = SwapSettingRecipient(swapSettings, tokenOut)
        val settingSlippage = SwapSettingSlippage(swapSettings, BigDecimal("1"))
        val slippage = settingSlippage.value

        val cautions = mutableListOf<HSCaution>()

        val bestRoute = quoteSwapBestRoute(
            tokenIn,
            tokenOut,
            amountIn,
            slippage,
            settingRecipient.value,
            true
        )

        val amountOut = bestRoute.expectedBuyAmount ?: BigDecimal.ZERO

        val amountOutMin = amountOut.subtract(amountOut.multiply(slippage.movePointLeft(2)))

        val fields = buildList {
            settingRecipient.value?.let {
                add(DataFieldRecipientExtended(it, tokenOut.blockchainType))
            }
            add(DataFieldSlippage(slippage))
        }

        return SwapFinalQuoteThorChain(
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
            cautions = cautions,
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
                    value = BigInteger(jsonObject["value"].asString),
                    input = (jsonObject["data"].asString).hexStringToByteArray()
                )

                return SendTransactionData.Evm(
                    transactionData = transactionData,
                    gasLimit = jsonObject["gas"]?.asString?.hexStringToByteArray()?.toLong(),
                    feesMap = mapOf()
                )
            }
        }

        when (blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
                -> TODO()

            BlockchainType.Solana -> {
                if (bestRoute.tx?.isJsonPrimitive == true) {
                    return SendTransactionData.Solana.WithRawTransaction(
                        Base64.decode(
                            bestRoute.tx.asString,
                            Base64.DEFAULT
                        )
                    )
                }
            }
            BlockchainType.Tron -> {
                if (bestRoute.tx != null) {
                    val rawTransaction = APIClient.gson.fromJson(
                        bestRoute.tx,
                        CreatedTransaction::class.java
                    )

                    return SendTransactionData.Tron.WithCreateTransaction(rawTransaction)
                }
            }
            BlockchainType.Zcash -> TODO()
            else -> Unit
        }

        throw IllegalArgumentException("Not supported blockchainType: $blockchainType")
    }

    data class Asset(val identifier: String, val providers: List<String>)
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
            val includeTx: Boolean,
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
                val tx: JsonElement?
            )
        }
    }
}
