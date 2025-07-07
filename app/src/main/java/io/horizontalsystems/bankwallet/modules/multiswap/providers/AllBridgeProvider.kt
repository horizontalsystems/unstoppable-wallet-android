package io.horizontalsystems.bankwallet.modules.multiswap.providers

import android.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.isEvm
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.bankwallet.modules.multiswap.providers.AllBridgeAPI.Response
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.settings.ISwapSetting
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.hexStringToByteArray
import io.horizontalsystems.tronkit.network.CreatedTransaction
import retrofit2.http.GET
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger

object AllBridgeProvider : IMultiSwapProvider {
    override val id = "allbridge"
    override val title = "AllBridge"
    override val url = "https://allbridge.io/"
    override val icon = R.drawable.allbridge
    override val priority = 0
    private val feePaymentMethod = FeePaymentMethod.StableCoin

    private val allBridgeAPI =
        APIClient.retrofit("http://192.168.1.8:3000", 60).create(AllBridgeAPI::class.java)

    private val blockchainTypes = mapOf(
        "ARB" to BlockchainType.ArbitrumOne,
        "AVA" to BlockchainType.Avalanche,
        "BAS" to BlockchainType.Base,
        "BSC" to BlockchainType.BinanceSmartChain,
        "ETH" to BlockchainType.Ethereum,
        "OPT" to BlockchainType.Optimism,
        "POL" to BlockchainType.Polygon,
        "SOL" to BlockchainType.Solana,
        "SRB" to BlockchainType.Stellar,
        "TRX" to BlockchainType.Tron,
    )

    private var tokenPairs = listOf<AllBridgeTokenPair>()

    override suspend fun start() {
        val tokenPairs = mutableListOf<AllBridgeTokenPair>()

        val tokens = allBridgeAPI.tokens()
        tokens.forEach { abToken ->
            val blockchainType = blockchainTypes[abToken.chainSymbol]
            val tokenType = when (blockchainType) {
                BlockchainType.ArbitrumOne,
                BlockchainType.Avalanche,
                BlockchainType.Base,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Ethereum,
                BlockchainType.Optimism,
                BlockchainType.Polygon,
                BlockchainType.Tron,
                    -> {
                    TokenType.Eip20(abToken.tokenAddress)
                }

                BlockchainType.Solana -> {
                    TokenType.Spl(abToken.tokenAddress)
                }

                BlockchainType.Stellar -> {
                    abToken.originTokenAddress?.let { originTokenAddress ->
                        val parts = originTokenAddress.split(":")

                        if (parts.size == 2) {
                            TokenType.Asset(parts[0], parts[1])
                        } else {
                            null
                        }
                    }
                }

                else -> null
            }

            if (blockchainType != null && tokenType != null) {
                App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                    tokenPairs.add(AllBridgeTokenPair(abToken, it))
                }
            }
        }

        this.tokenPairs = tokenPairs
    }

    override fun supports(blockchainType: BlockchainType): Boolean {
        // overriding fun supports(tokenFrom: Token, tokenTo: Token) makes this method redundant
        return true
    }

    override fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        return tokenPairs.any { it.token == tokenFrom } && tokenPairs.any { it.token == tokenTo }
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>,
    ): ISwapQuote {
        val bridgeAmounts = getQuote(tokenIn, tokenOut, amountIn)

        val tokenPairIn = tokenPairs.first { it.token == tokenIn }
        val bridgeAddress = tokenPairIn.abToken.bridgeAddress

        var actionRequired: ISwapProviderAction? = null

        if (tokenIn.blockchainType.isEvm) {
            val bridgeAddressEvm = try {
                Address(bridgeAddress)
            } catch (_: Throwable) {
                null
            }

            if (bridgeAddressEvm != null) {
                val allowance = EvmSwapHelper.getAllowance(tokenIn, bridgeAddressEvm)
                actionRequired = EvmSwapHelper.actionApprove(allowance, amountIn, bridgeAddressEvm, tokenIn)
            }
        } else if (tokenIn.blockchainType == BlockchainType.Tron) {
            val allowance = SwapHelper.getAllowanceTrc20(tokenIn, bridgeAddress)
            actionRequired = SwapHelper.actionApproveTrc20(allowance, amountIn, bridgeAddress, tokenIn)
        }

        return object : ISwapQuote {
            override val amountOut: BigDecimal = bridgeAmounts.amountReceivedInFloat
            override val priceImpact: BigDecimal? = null
            override val fields: List<DataField> = listOf()
            override val settings: List<ISwapSetting> = listOf()
            override val tokenIn: Token = tokenIn
            override val tokenOut: Token = tokenOut
            override val amountIn: BigDecimal = amountIn
            override val actionRequired: ISwapProviderAction? = actionRequired
            override val cautions: List<HSCaution> = listOf()
        }
    }

    private suspend fun getQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): Response.BridgeAmounts {
        val tokenPairIn = tokenPairs.first { it.token == tokenIn }
        val tokenPairOut = tokenPairs.first { it.token == tokenOut }

        val sourceToken = tokenPairIn.abToken.tokenAddress
        val destinationToken = tokenPairOut.abToken.tokenAddress

        var resAmountIn = amountIn

        if (feePaymentMethod == FeePaymentMethod.StableCoin) {
            val gasFee = allBridgeAPI.gasFee(
                sourceToken = sourceToken,
                destinationToken = destinationToken
            )

            resAmountIn -= gasFee.stablecoin.float

            if (resAmountIn < BigDecimal.ZERO) {
                throw Exception("Amount is less than required fee")
            }
        }

        val amount = resAmountIn.movePointRight(tokenPairIn.abToken.decimals).toBigInteger()
        val bridgeAmounts = allBridgeAPI.bridgeReceiveCalculate(
            amount = amount,
            sourceToken = sourceToken,
            destinationToken = destinationToken,
        )
        return bridgeAmounts
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: ISwapQuote,
    ): ISwapFinalQuote {
        val bridgeAmounts = getQuote(tokenIn, tokenOut, amountIn)
        val amountOut = bridgeAmounts.amountReceivedInFloat
        val sendTransactionData = getSendTransactionData(tokenIn, tokenOut, amountIn, amountOut)

        return object : ISwapFinalQuote {
            override val tokenIn: Token = tokenIn
            override val tokenOut: Token = tokenOut
            override val amountIn: BigDecimal = amountIn
            override val amountOut: BigDecimal = amountOut
            override val amountOutMin: BigDecimal? = null
            override val sendTransactionData: SendTransactionData = sendTransactionData
            override val priceImpact: BigDecimal? = null
            override val fields: List<DataField> = listOf()
            override val cautions: List<HSCaution> = listOf()
        }
    }

    private suspend fun getSendTransactionData(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        amountOutMin: BigDecimal,
    ): SendTransactionData {
        val tokenPairIn = tokenPairs.first { it.token == tokenIn }
        val tokenPairOut = tokenPairs.first { it.token == tokenOut }

        val amount = amountIn.movePointRight(tokenPairIn.abToken.decimals).toBigInteger()

        val rawTransactionStr = if (tokenIn.blockchainType == tokenOut.blockchainType) {
            val amountOutMinInt = amountOutMin.movePointRight(tokenPairOut.abToken.decimals).toBigInteger()

            allBridgeAPI.rawSwap(
                amount = amount,
                sender = SwapHelper.getReceiveAddressForToken(tokenIn),
                recipient = SwapHelper.getReceiveAddressForToken(tokenOut),
                sourceToken = tokenPairIn.abToken.tokenAddress,
                destinationToken = tokenPairOut.abToken.tokenAddress,
                minimumReceiveAmount = amountOutMinInt
            )
        } else {
            allBridgeAPI.rawBridge(
                amount = amount,
                sender = SwapHelper.getReceiveAddressForToken(tokenIn),
                recipient = SwapHelper.getReceiveAddressForToken(tokenOut),
                sourceToken = tokenPairIn.abToken.tokenAddress,
                destinationToken = tokenPairOut.abToken.tokenAddress,
                feePaymentMethod = feePaymentMethod.value
            )
        }

        return when {
            tokenIn.blockchainType.isEvm -> {
                val rawTransaction = APIClient.gson.fromJson(
                    rawTransactionStr,
                    Response.RawTransaction::class.java
                )

                SendTransactionData.Evm(
                    transactionData = TransactionData(
                        to = Address(rawTransaction.to),
                        value = rawTransaction.value?.toBigInteger() ?: BigInteger.ZERO,
                        input = rawTransaction.data.hexStringToByteArray(),
                    ),
                    gasLimit = null
                )
            }

            tokenIn.blockchainType == BlockchainType.Tron -> {
                val rawTransaction = APIClient.gson.fromJson(
                    rawTransactionStr,
                    CreatedTransaction::class.java
                )

                SendTransactionData.Tron.WithCreateTransaction(rawTransaction)
            }

            else -> {
                Log.e("AAA", "Not implemented for: ${tokenIn.blockchainType}, transaction: $rawTransactionStr")
                TODO("Not yet implemented")
            }
        }
    }

    enum class FeePaymentMethod(val value: String) {
        Native("WITH_NATIVE_CURRENCY"),
        StableCoin("WITH_STABLECOIN");
    }
}


interface AllBridgeAPI {
    @GET("/tokens")
    suspend fun tokens(): List<Response.Token>

    @GET("/bridge/receive/calculate")
    suspend fun bridgeReceiveCalculate(
        @Query("amount") amount: BigInteger,
        @Query("sourceToken") sourceToken: String,
        @Query("destinationToken") destinationToken: String,
        @Query("messenger") messenger: String = "ALLBRIDGE",
    ): Response.BridgeAmounts

    @GET("/raw/swap")
    suspend fun rawSwap(
        @Query("amount") amount: BigInteger,
        @Query("sender") sender: String,
        @Query("recipient") recipient: String,
        @Query("sourceToken") sourceToken: String,
        @Query("destinationToken") destinationToken: String,
        @Query("minimumReceiveAmount") minimumReceiveAmount: BigInteger,
    ): String

    @GET("/raw/bridge")
    suspend fun rawBridge(
        @Query("amount") amount: BigInteger,
        @Query("sender") sender: String,
        @Query("recipient") recipient: String,
        @Query("sourceToken") sourceToken: String,
        @Query("destinationToken") destinationToken: String,
        @Query("messenger") messenger: String = "ALLBRIDGE",
        @Query("feePaymentMethod") feePaymentMethod: String,
    ): String

    @GET("/gas/fee")
    suspend fun gasFee(
        @Query("sourceToken") sourceToken: String,
        @Query("destinationToken") destinationToken: String,
        @Query("messenger") messenger: String = "ALLBRIDGE"
    ): Response.GasFeeOptions

    object Response {
        data class GasFeeOptions(
            val native: Amount,
            val stablecoin: Amount,
        )

        data class Amount(
            val int: BigInteger,
            val float: BigDecimal,
        )

        data class RawTransaction(
            val from: String,
            val to: String,
            val value: BigDecimal?,
            val data: String,
        )

        data class BridgeAmounts(
            val amountInFloat: BigDecimal,
            val amountReceivedInFloat: BigDecimal,
        )

        data class Token(
            val symbol: String,
            val name: String,
            val decimals: Int,
            val tokenAddress: String,
            val originTokenAddress: String?,
            val confirmations: Int,
            val chainSymbol: String,
            val chainId: String,
            val chainType: String,
            val chainName: String,
            val bridgeAddress: String,
            //    "poolAddress": "string",
            //    "cctpAddress": "string",
            //    "cctpFeeShare": "string",
            //    "cctpV2Address": "string",
            //    "cctpV2FeeShare": "string",
            //    "feeShare": "string",
            //    "apr7d": "string",
            //    "apr30d": "string",
            //    "lpRate": "string",
            //    "allbridgeChainId": 0,
        )
    }
}

data class AllBridgeTokenPair(val abToken: Response.Token, val token: Token)