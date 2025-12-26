package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.derivation
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.nativeTokenQueries
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapFinalQuote
import io.horizontalsystems.bankwallet.modules.multiswap.ISwapQuote
import io.horizontalsystems.bankwallet.modules.multiswap.SwapFinalQuoteThorChain
import io.horizontalsystems.bankwallet.modules.multiswap.SwapQuoteThorChain
import io.horizontalsystems.bankwallet.modules.multiswap.providers.ThornodeAPI.Response
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.FeeType
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldAllowance
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldRecipientExtended
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldSlippage
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import retrofit2.http.GET
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date

abstract class BaseThorChainProvider(
    baseUrl: String,
    private val affiliate: String?,
    private val affiliateBps: Int?,
) : IMultiSwapProvider {

    private val thornodeAPI =
        APIClient.retrofit(baseUrl, 60).create(ThornodeAPI::class.java)

    private val blockchainTypes = mapOf(
        "ARB" to BlockchainType.ArbitrumOne,
        "AVAX" to BlockchainType.Avalanche,
        "BCH" to BlockchainType.BitcoinCash,
        "BSC" to BlockchainType.BinanceSmartChain,
        "BTC" to BlockchainType.Bitcoin,
        "ETH" to BlockchainType.Ethereum,
        "LTC" to BlockchainType.Litecoin,
        "BASE" to BlockchainType.Base,
        "DASH" to BlockchainType.Dash,
//        "ZEC" to BlockchainType.Zcash,
    )

    private var assets = listOf<Asset>()

    override suspend fun start() {
        val assets = mutableListOf<Asset>()

        val pools = thornodeAPI.pools().filter { it.status.lowercase() == "available" }
        for (pool in pools) {
            val (assetBlockchainId, assetId) = pool.asset.split(".")
            val blockchainType = blockchainTypes[assetBlockchainId] ?: continue

            when (blockchainType) {
                BlockchainType.ArbitrumOne,
                BlockchainType.Avalanche,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Ethereum,
                BlockchainType.Base,
                    -> {
                    val components = assetId.split("-")

                    val tokenType = if (components.size == 2) {
                        TokenType.Eip20(components[1])
                    } else {
                        TokenType.Native
                    }

                    App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let { token ->
                        assets.add(Asset(pool.asset, token))
                    }
                }

                BlockchainType.BitcoinCash,
                BlockchainType.Bitcoin,
                BlockchainType.Litecoin,
                BlockchainType.Dash,
//                BlockchainType.Zcash,
                    -> {
                    var nativeTokenQueries = blockchainType.nativeTokenQueries

                    // filter out taproot for ltc
                    if (blockchainType == BlockchainType.Litecoin) {
                        nativeTokenQueries = nativeTokenQueries.filterNot {
                            it.tokenType.derivation == TokenType.Derivation.Bip86
                        }
                    }

                    val tokens = App.marketKit.tokens(nativeTokenQueries)
                    assets.addAll(tokens.map { Asset(pool.asset, it) })
                }

                else -> Unit
            }
        }

        this.assets = assets
    }

    override fun supports(blockchainType: BlockchainType): Boolean {
        // overriding fun supports(tokenFrom: Token, tokenTo: Token) makes this method redundant
        return true
    }

    override fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        return assets.any { it.token == tokenFrom } && assets.any { it.token == tokenTo }
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>,
    ): ISwapQuote {
        val settingRecipient = SwapSettingRecipient(settings, tokenOut)

        val quoteSwap = quoteSwap(tokenIn, tokenOut, amountIn, null, settingRecipient.value)

        val cautions = mutableListOf<HSCaution>()
        val settingSlippage = SwapSettingSlippage(settings, BigDecimal("1"))

        val routerAddress = quoteSwap.router?.let { router ->
            try {
                Address(router)
            } catch (_: Throwable) {
                null
            }
        }

        val allowance = routerAddress?.let { EvmSwapHelper.getAllowance(tokenIn, it) }
        val actionApprove = routerAddress?.let {
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
            amountOut = quoteSwap.expected_amount_out.movePointLeft(8),
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

    private suspend fun quoteSwap(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal?,
        recipient: io.horizontalsystems.bankwallet.entities.Address?
    ): Response.QuoteSwap {
        val assetIn = assets.first { it.token == tokenIn }
        val assetOut = assets.first { it.token == tokenOut }
        val destination = recipient?.hex ?: SwapHelper.getReceiveAddressForToken(tokenOut)

        return thornodeAPI.quoteSwap(
            fromAsset = assetIn.asset,
            toAsset = assetOut.asset,
            amount = amountIn.movePointRight(8).toLong(),
            destination = destination,
            affiliate = affiliate,
            affiliateBps = affiliateBps,
            streamingInterval = 1,
            streamingQuantity = 0,
            liquidityToleranceBps = slippage?.movePointRight(2)?.toLong()
        )
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

        val quoteSwap = quoteSwap(tokenIn, tokenOut, amountIn, slippage, settingRecipient.value)

        val amountOut = quoteSwap.expected_amount_out.movePointLeft(8)

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
                quoteSwap,
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
        quoteSwap: Response.QuoteSwap,
        tokenOut: Token,
    ): SendTransactionData {
        val inboundAddress = quoteSwap.inbound_address
        val memo = quoteSwap.memo

        val router = quoteSwap.router
        val recommendedGasRate = quoteSwap.recommended_gas_rate.toInt()
        val dustThreshold = quoteSwap.dust_threshold?.toInt()

        val outboundFee = CoinValue(tokenOut, quoteSwap.fees.outbound.movePointLeft(8))
        val liquidityFee = CoinValue(tokenOut, quoteSwap.fees.liquidity.movePointLeft(8))

        val feesMap = mapOf(
            FeeType.Liquidity to liquidityFee,
            FeeType.Outbound to outboundFee,
        )

        return when (tokenIn.blockchainType) {
            BlockchainType.ArbitrumOne,
            BlockchainType.Avalanche,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Ethereum,
            BlockchainType.Base,
                -> {
                val transactionData = when (val tokenType = tokenIn.type) {
                    TokenType.Native -> {
                        TransactionData(
                            Address(inboundAddress),
                            amountIn.movePointRight(tokenIn.decimals).toBigInteger(),
                            memo.toByteArray()
                        )
                    }

                    is TokenType.Eip20 -> {
                        val method = DepositWithExpiryMethod(
                            Address(inboundAddress),
                            Address(tokenType.address),
                            amountIn.movePointRight(tokenIn.decimals).toBigInteger(),
                            memo,
                            BigInteger.valueOf(Date().time / 1000 + 1 * 60 * 60)
                        )

                        checkNotNull(router)

                        TransactionData(
                            Address(router),
                            BigInteger.ZERO,
                            method.encodedABI()
                        )
                    }

                    else -> throw IllegalArgumentException()
                }

                SendTransactionData.Evm(
                    transactionData = transactionData,
                    gasLimit = null,
                    feesMap = feesMap
                )
            }

            BlockchainType.BitcoinCash,
            BlockchainType.Bitcoin,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
                -> {
                SendTransactionData.Btc(
                    address = inboundAddress,
                    memo = memo,
                    amount = amountIn,
                    recommendedGasRate = recommendedGasRate,
                    minimumSendAmount = dustThreshold?.plus(1),
                    changeToFirstInput = true,
                    utxoFilters = UtxoFilters(
                        scriptTypes = listOf(ScriptType.P2PKH, ScriptType.P2WPKHSH, ScriptType.P2WPKH),
                        maxOutputsCountForInputs = 10
                    ),
                    feesMap = feesMap
                )
            }

            else -> throw IllegalArgumentException()
        }
    }

    data class Asset(val asset: String, val token: Token)

}

interface ThornodeAPI {
    @GET("pools")
    suspend fun pools(): List<Response.Pool>

    @GET("quote/swap")
    suspend fun quoteSwap(
        @Query("from_asset") fromAsset: String,
        @Query("to_asset") toAsset: String,
        @Query("amount") amount: Long,
        @Query("destination") destination: String,
        @Query("affiliate") affiliate: String?,
        @Query("affiliate_bps") affiliateBps: Int?,
        @Query("streaming_interval") streamingInterval: Long,
        @Query("streaming_quantity") streamingQuantity: Long,
        @Query("liquidity_tolerance_bps") liquidityToleranceBps: Long?,

    ): Response.QuoteSwap

    object Response {
        data class QuoteSwap(
            val inbound_address: String,
//  "inbound_confirmation_blocks": 1,
//  "inbound_confirmation_seconds": 600,
//  "outbound_delay_blocks": 179,
//  "outbound_delay_seconds": 1074,
            val fees: Fees,
//  "fees": {
//    "asset": "ETH.ETH",
//    "affiliate": "0",
//    "outbound": "54840",
//    "liquidity": "2037232",
//    "total": "2092072",
//    "slippage_bps": 9,
//    "total_bps": 10
//  },
            val router: String?,
//  "slippage_bps": 41,
//  "streaming_slippage_bps": 9,
            val expiry: Long,
//  "warning": "Do not cache this response. Do not send funds after the expiry.",
//  "notes": "First output should be to inbound_address, second output should be change back to self, third output should be OP_RETURN, limited to 80 bytes. Do not send below the dust threshold. Do not use exotic spend scripts, locks or address formats (P2WSH with Bech32 address format preferred).",
            val dust_threshold: String?,
//  "recommended_min_amount_in": "10760",
            val recommended_gas_rate: String,
//  "gas_rate_units": "satsperbyte",
            val memo: String,
            val expected_amount_out: BigDecimal,
//  "expected_amount_out_streaming": "2035299208",
//  "max_streaming_quantity": 8,
//  "streaming_swap_blocks": 7,
//  "streaming_swap_seconds": 42,
//  "total_swap_seconds": 1674
        ) {
            data class Fees(
                val affiliate: BigDecimal,
                val outbound: BigDecimal,
                val liquidity: BigDecimal,
                val total: BigDecimal,
            )
        }

        data class Pool(
            val asset: String,
            val status: String,
// "short_code": "b",
// "decimals": 6,
// "pending_inbound_asset": "101713319",
// "pending_inbound_rune": "464993836",
// "balance_asset": "3197744873",
// "balance_rune": "13460619152985",
// "asset_tor_price": "123456",
// "pool_units": "14694928607473",
// "LP_units": "14694928607473",
// "synth_units": "0",
// "synth_supply": "0",
// "savers_depth": "199998",
// "savers_units": "199998",
// "savers_fill_bps": "4500",
// "savers_capacity_remaining": "1000",
// "synth_mint_paused": true,
// "synth_supply_remaining": "123456",
// "loan_collateral": "123456",
// "loan_collateral_remaining": "123456",
// "loan_cr": "123456",
// "derived_depth_bps": "123456"
        )
    }
}

class DepositWithExpiryMethod(
    val inboundAddress: Address,
    val asset: Address,
    val amount: BigInteger,
    val memo: String,
    val expiry: BigInteger,
) : ContractMethod() {
    override val methodSignature = "depositWithExpiry(address,address,uint256,string,uint256)"
    override fun getArguments() = listOf(inboundAddress, asset, amount, memo, expiry)
}


sealed class SwapError : Exception() {
    class NoDestinationAddress : SwapError()
}
