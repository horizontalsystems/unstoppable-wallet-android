package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.hexToByteArray
import io.horizontalsystems.bankwallet.core.isEvm
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.bankwallet.modules.multiswap.providers.AllBridgeAPI.Response
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.settings.ISwapSetting
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldAllowance
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipientExtended
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldSlippage
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
    override val icon = R.drawable.allbridge
    override val priority = 0
    private val feePaymentMethod = FeePaymentMethod.StableCoin

    private val proxies = mapOf(
        //Ethereum
        "0x609c690e8F7D68a59885c9132e812eEbDaAf0c9e" to "0x6153F92eF47A97046820714233956f0B0F99d886",
        //BNB Chain
        "0x3C4FA639c8D7E65c603145adaD8bD12F2358312f" to "0x6153F92eF47A97046820714233956f0B0F99d886",
        //Tron
        "TAuErcuAtU6BPt6YwL51JZ4RpDCPQASCU2" to null,
        //Solana
        "BrdgN2RPzEMWF96ZbnnJaUtQDQx7VRXYaHHbYCBvceWB" to null,
        //Polygon
        "0x7775d63836987f444E2F14AA0fA2602204D7D3E0" to "0x6153F92eF47A97046820714233956f0B0F99d886",
        //Arbitrum
        "0x9Ce3447B58D58e8602B7306316A5fF011B92d189" to "0x6153F92eF47A97046820714233956f0B0F99d886",
        //Stellar
        "CBQ6GW7QCFFE252QEVENUNG45KYHHBRO4IZIWFJOXEFANHPQUXX5NFWV" to null,
        //Avalanche
        "0x9068E1C28941D0A680197Cc03be8aFe27ccaeea9" to "0x6153F92eF47A97046820714233956f0B0F99d886",
        //Base
        "0x001E3f136c2f804854581Da55Ad7660a2b35DEf7" to "0x6153F92eF47A97046820714233956f0B0F99d886",
        //OP Mainnet
        "0x97E5BF5068eA6a9604Ee25851e6c9780Ff50d5ab" to "0x6153F92eF47A97046820714233956f0B0F99d886",
        //Celo
        "0x80858f5F8EFD2Ab6485Aba1A0B9557ED46C6ba0e" to null,
        //Sui
        "0x83d6f864a6b0f16898376b486699aa6321eb6466d1daf6a2e3764a51908fe99d" to null,
    )

    private val allBridgeAPI = APIClient
        .retrofit("https://allbridge.blocksdecoded.com", 60)
        .create(AllBridgeAPI::class.java)

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

    private fun getProxyAddress(bridgeAddress: String) = proxies[bridgeAddress]

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
        val settingRecipient = SwapSettingRecipient(settings, tokenOut)
        var settingSlippage: SwapSettingSlippage? = null

        val amountOut = estimateAmountOut(tokenIn, tokenOut, amountIn)

        val tokenPairIn = tokenPairs.first { it.token == tokenIn }
        val bridgeAddress = tokenPairIn.abToken.bridgeAddress

        val cautions = mutableListOf<HSCaution>()
        val allowance: BigDecimal?
        val actionRequired: ISwapProviderAction?

        if (tokenIn.blockchainType.isEvm) {
            val proxyAddress = getProxyAddress(bridgeAddress)
            val finalAddress = proxyAddress ?: bridgeAddress

            allowance = EvmSwapHelper.getAllowance(tokenIn, finalAddress)
            actionRequired = EvmSwapHelper.actionApprove(allowance, amountIn, finalAddress, tokenIn)
        } else if (tokenIn.blockchainType == BlockchainType.Tron) {
            allowance = SwapHelper.getAllowanceTrc20(tokenIn, bridgeAddress)
            actionRequired = SwapHelper.actionApproveTrc20(allowance, amountIn, bridgeAddress, tokenIn)
        } else {
            allowance = null
            actionRequired = null
        }

        val crosschain = tokenIn.blockchainType != tokenOut.blockchainType
        if (!crosschain) {
            settingSlippage = SwapSettingSlippage(settings, BigDecimal("1"))
        }

        val fields = buildList {
            settingRecipient.value?.let {
                add(DataFieldRecipient(it))
            }
            settingSlippage?.value?.let {
                add(DataFieldSlippage(it))
            }
            if (allowance != null && allowance < amountIn) {
                add(DataFieldAllowance(allowance, tokenIn))
            }
        }

        return object : ISwapQuote {
            override val amountOut: BigDecimal = amountOut
            override val priceImpact: BigDecimal? = null
            override val fields: List<DataField> = fields
            override val settings: List<ISwapSetting> = listOfNotNull(settingRecipient, settingSlippage)
            override val tokenIn: Token = tokenIn
            override val tokenOut: Token = tokenOut
            override val amountIn: BigDecimal = amountIn
            override val actionRequired: ISwapProviderAction? = actionRequired
            override val cautions: List<HSCaution> = cautions
        }
    }

    private suspend fun estimateAmountOut(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): BigDecimal {
        val tokenPairIn = tokenPairs.first { it.token == tokenIn }
        val tokenPairOut = tokenPairs.first { it.token == tokenOut }

        val sourceToken = tokenPairIn.abToken.tokenAddress
        val destinationToken = tokenPairOut.abToken.tokenAddress

        var resAmountIn = amountIn

        val bridgeAddress = tokenPairIn.abToken.bridgeAddress

        getProxyAddress(bridgeAddress)?.let { proxyAddress ->
            val proxyFee = EvmSwapHelper.getAllBridgeProxyFee(proxyAddress, amountIn)

            resAmountIn -= proxyFee

            if (resAmountIn < BigDecimal.ZERO) {
                throw Exception("Amount is less than required fee")
            }
        }

        if (feePaymentMethod == FeePaymentMethod.StableCoin) {
            val gasFee = allBridgeAPI.gasFee(
                sourceToken = sourceToken,
                destinationToken = destinationToken
            )

            val allbridgeFee = gasFee.stablecoin.float

            resAmountIn -= allbridgeFee

            if (resAmountIn < BigDecimal.ZERO) {
                throw Exception("Amount is less than required fee")
            }
        }

        val amount = resAmountIn.movePointRight(tokenPairIn.abToken.decimals).toBigInteger()

        // to get the minimum expected receive amount used endpoint pendingInfo instead of bridgeReceiveCalculate
        val pendingInfo = allBridgeAPI.pendingInfo(
            amount = amount,
            sourceToken = sourceToken,
            destinationToken = destinationToken
        )

        return pendingInfo.estimatedAmount.min.float
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: ISwapQuote,
    ): ISwapFinalQuote {
        val cautions = mutableListOf<HSCaution>()

        val settingRecipient = SwapSettingRecipient(swapSettings, tokenOut)
        var settingSlippage: SwapSettingSlippage? = null

        val crosschain = tokenIn.blockchainType != tokenOut.blockchainType
        if (!crosschain) {
            settingSlippage = SwapSettingSlippage(swapSettings, BigDecimal("1"))
        }

        val slippage = settingSlippage?.value

        val amountOut = estimateAmountOut(tokenIn, tokenOut, amountIn)

        val amountOutMin = slippage?.let {
            amountOut - amountOut / BigDecimal(100) * slippage
        }

        val sendTransactionData = getSendTransactionData(
            tokenIn,
            tokenOut,
            amountIn,
            amountOutMin ?: amountOut,
            settingRecipient.value
        )

        val fields = buildList {
            settingRecipient.value?.let {
                add(DataFieldRecipientExtended(it, tokenOut.blockchainType))
            }
            settingSlippage?.value?.let {
                add(DataFieldSlippage(it))
            }
        }

        return object : ISwapFinalQuote {
            override val tokenIn: Token = tokenIn
            override val tokenOut: Token = tokenOut
            override val amountIn: BigDecimal = amountIn
            override val amountOut: BigDecimal = amountOut
            override val amountOutMin: BigDecimal? = amountOutMin
            override val sendTransactionData: SendTransactionData = sendTransactionData
            override val priceImpact: BigDecimal? = null
            override val fields: List<DataField> = fields
            override val cautions: List<HSCaution> = cautions
        }
    }

    private suspend fun getSendTransactionData(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        expectedAmountOutMin: BigDecimal,
        recipient: io.horizontalsystems.bankwallet.entities.Address?,
    ): SendTransactionData {
        val tokenPairIn = tokenPairs.first { it.token == tokenIn }
        val tokenPairOut = tokenPairs.first { it.token == tokenOut }
        val recipientStr = recipient?.hex ?: SwapHelper.getReceiveAddressForToken(tokenOut)

        val amount = amountIn.movePointRight(tokenPairIn.abToken.decimals).toBigInteger()

        var solanaTxFeeParams: String? = null
        var solanaTxFeeValue: String? = null

        if (tokenIn.blockchainType == BlockchainType.Solana) {
            solanaTxFeeParams = "PRICE_PER_UNIT_IN_MICRO_LAMPORTS"
            solanaTxFeeValue = "71428"
        }

        val rawTransactionStr = if (tokenIn.blockchainType == tokenOut.blockchainType) {
            val amountOutMinInt = expectedAmountOutMin.movePointRight(tokenPairOut.abToken.decimals).toBigInteger()

            allBridgeAPI.rawSwap(
                amount = amount,
                sender = SwapHelper.getReceiveAddressForToken(tokenIn),
                recipient = recipientStr,
                sourceToken = tokenPairIn.abToken.tokenAddress,
                destinationToken = tokenPairOut.abToken.tokenAddress,
                minimumReceiveAmount = amountOutMinInt,
                solanaTxFeeParams = solanaTxFeeParams,
                solanaTxFeeValue = solanaTxFeeValue,
            )
        } else {
            allBridgeAPI.rawBridge(
                amount = amount,
                sender = SwapHelper.getReceiveAddressForToken(tokenIn),
                recipient = recipientStr,
                sourceToken = tokenPairIn.abToken.tokenAddress,
                destinationToken = tokenPairOut.abToken.tokenAddress,
                feePaymentMethod = feePaymentMethod.value,
                solanaTxFeeParams = solanaTxFeeParams,
                solanaTxFeeValue = solanaTxFeeValue,
            )
        }

        return when {
            tokenIn.blockchainType.isEvm -> {
                val rawTransaction = APIClient.gson.fromJson(
                    rawTransactionStr,
                    Response.RawTransaction::class.java
                )

                val bridgeAddress = rawTransaction.to
                val proxyAddress = getProxyAddress(bridgeAddress)
                val finalAddress = proxyAddress ?: bridgeAddress

                SendTransactionData.Evm(
                    transactionData = TransactionData(
                        to = Address(finalAddress),
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

            tokenIn.blockchainType == BlockchainType.Stellar -> {
                SendTransactionData.Stellar.WithTransactionEnvelope(rawTransactionStr)
            }

            tokenIn.blockchainType == BlockchainType.Solana -> {
                SendTransactionData.Solana.WithRawTransaction(rawTransactionStr.hexToByteArray())
            }

            else -> throw IllegalArgumentException("Swapping ${tokenIn.blockchainType} not supported")
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

    @GET("/pending/info")
    suspend fun pendingInfo(
        @Query("amount") amount: BigInteger,
        @Query("sourceToken") sourceToken: String,
        @Query("destinationToken") destinationToken: String,
    ): Response.PendingInfo

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
        @Query("solanaTxFeeParams") solanaTxFeeParams: String?,
        @Query("solanaTxFeeValue") solanaTxFeeValue: String?
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
        @Query("solanaTxFeeParams") solanaTxFeeParams: String?,
        @Query("solanaTxFeeValue") solanaTxFeeValue: String?
    ): String

    @GET("/gas/fee")
    suspend fun gasFee(
        @Query("sourceToken") sourceToken: String,
        @Query("destinationToken") destinationToken: String,
        @Query("messenger") messenger: String = "ALLBRIDGE"
    ): Response.GasFeeOptions

    object Response {
        data class PendingInfo(
            val pendingTxs: Long,
            val estimatedAmount: EstimatedAmount
        )

        data class EstimatedAmount(
            val min: Amount,
            val max: Amount
        )

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
//            val confirmations: Int,
            val chainSymbol: String,
//            val chainId: String,
//            val chainType: String,
//            val chainName: String,
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
