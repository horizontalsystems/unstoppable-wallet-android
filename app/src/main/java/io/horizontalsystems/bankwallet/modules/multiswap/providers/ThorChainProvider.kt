package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinCashAdapter
import io.horizontalsystems.bankwallet.core.adapters.LitecoinAdapter
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
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
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingSlippage
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldAllowance
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

object ThorChainProvider : IMultiSwapProvider {
    override val id = "thorchain"
    override val title = "THORChain"
    override val url = "https://thorchain.org/swap"
    override val icon = R.drawable.thorchain
    override val priority = 0
    private val adapterManager = App.adapterManager
    private val affiliate: String? = null
    private val affiliateBps: Int? = null

    private val thornodeAPI =
        APIClient.retrofit("https://thornode.ninerealms.com", 60).create(ThornodeAPI::class.java)


    private val blockchainTypes = mapOf(
        "AVAX" to BlockchainType.Avalanche,
        "BCH" to BlockchainType.BitcoinCash,
        "BSC" to BlockchainType.BinanceSmartChain,
        "BTC" to BlockchainType.Bitcoin,
        "ETH" to BlockchainType.Ethereum,
        "LTC" to BlockchainType.Litecoin,
        "BASE" to BlockchainType.Base,
    )

    private var assets = listOf<Asset>()

    override suspend fun start() {
        val assets = mutableListOf<Asset>()

        val pools = thornodeAPI.pools().filter { it.status.lowercase() == "available" }
        for (pool in pools) {
            val (assetBlockchainId, assetId) = pool.asset.split(".")
            val blockchainType = blockchainTypes[assetBlockchainId] ?: continue

            when (blockchainType) {
                BlockchainType.Avalanche,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Ethereum,
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
                    -> {
                    val tokens = App.marketKit.tokens(blockchainType.nativeTokenQueries)
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
        val quoteSwap = quoteSwap(tokenIn, tokenOut, amountIn)

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
            settingSlippage.value?.let {
                add(DataFieldSlippage(it))
            }
            if (allowance != null && allowance < amountIn) {
                add(DataFieldAllowance(allowance, tokenIn))
            }
        }

        return SwapQuoteThorChain(
            amountOut = BigDecimal(quoteSwap.expected_amount_out).movePointLeft(8),
            priceImpact = null,
            fields = fields,
            settings = listOf(settingSlippage),
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = actionApprove
        )
    }

    private suspend fun quoteSwap(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): Response.QuoteSwap {
        val assetIn = assets.first { it.token == tokenIn }
        val assetOut = assets.first { it.token == tokenOut }
        val destination = resolveDestination(tokenOut)

        return thornodeAPI.quoteSwap(
            fromAsset = assetIn.asset,
            toAsset = assetOut.asset,
            amount = amountIn.movePointRight(8).toLong(),
            destination = destination,
            affiliate = affiliate,
            affiliateBps = affiliateBps
        )
    }

    private fun resolveDestination(token: Token): String {
        val blockchainType = token.blockchainType

//        if let recipient = storage.recipient(blockchainType: blockchainType) {
//            return recipient.raw
//        }
//
        adapterManager.getAdapterForToken<IReceiveAdapter>(token)?.let {
            return it.receiveAddress
        }

        val accountManager = App.accountManager
        val evmBlockchainManager = App.evmBlockchainManager

        val account = accountManager.activeAccount ?: throw NoActiveAccount()

        when (blockchainType) {
            BlockchainType.Avalanche,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Ethereum -> {
                val chain = evmBlockchainManager.getChain(blockchainType)
                val evmAddress = account.type.evmAddress(chain) ?: throw SwapError.NoDestinationAddress()
                return evmAddress.eip55
            }
            BlockchainType.Bitcoin -> {
                return BitcoinAdapter.firstAddress(account.type, token.type)
            }
            BlockchainType.BitcoinCash ->{
                return BitcoinCashAdapter.firstAddress(account.type, token.type)
            }
            BlockchainType.Litecoin -> {
                return LitecoinAdapter.firstAddress(account.type, token.type)
            }
            else -> throw SwapError.NoDestinationAddress()
        }
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
    ): ISwapFinalQuote {
        val quoteSwap = quoteSwap(tokenIn, tokenOut, amountIn)

        val settingSlippage = SwapSettingSlippage(swapSettings, BigDecimal("1"))
        val slippage = settingSlippage.valueOrDefault()
        val amountOut = BigDecimal(quoteSwap.expected_amount_out).movePointLeft(8)

        val fields = buildList {
            settingSlippage.value?.let {
                add(DataFieldSlippage(it))
            }
        }

        return SwapFinalQuoteThorChain(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = amountOut,
            amountOutMin = null,
            sendTransactionData = getSendTransactionData(
                tokenIn,
                amountIn,
                quoteSwap,
                tokenOut,
                slippage
            ),
            priceImpact = null,
            fields = fields,
        )
    }

    private fun getSendTransactionData(
        tokenIn: Token,
        amountIn: BigDecimal,
        quoteSwap: Response.QuoteSwap,
        tokenOut: Token,
        slippage: BigDecimal,
    ): SendTransactionData {
        val inboundAddress = quoteSwap.inbound_address
        val memo = quoteSwap.memo.let {
            val amountOut = quoteSwap.expected_amount_out.toBigDecimal()
            val amountOutFeeIncluded = amountOut + quoteSwap.fees.total
            val amountOutMin = amountOutFeeIncluded - amountOutFeeIncluded / BigDecimal(100) * slippage

            val parts = it.split(":").toMutableList()

            val amountOutMinStr = amountOutMin.toBigInteger().toString()
            if (parts.size > 3) {
                parts[3] = amountOutMinStr
            } else if (parts.size == 3) {
                parts.add(amountOutMinStr)
            }

            parts.joinToString(":")
        }

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
            BlockchainType.Avalanche,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Ethereum,
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
                -> {
                SendTransactionData.Btc(
                    address = inboundAddress,
                    memo = memo,
                    amount = amountIn,
                    recommendedGasRate = recommendedGasRate,
                    dustThreshold = dustThreshold,
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
    @GET("/thorchain/pools")
    suspend fun pools(): List<Response.Pool>

    @GET("/thorchain/quote/swap")
    suspend fun quoteSwap(
        @Query("from_asset") fromAsset: String,
        @Query("to_asset") toAsset: String,
        @Query("amount") amount: Long,
        @Query("destination") destination: String,
        @Query("affiliate") affiliate: String?,
        @Query("affiliate_bps") affiliateBps: Int?,
//        @Query("streaming_interval") streamingInterval: Long,
//        @Query("streaming_quantity") streamingQuantity: Long,
//        @Query("tolerance_bps") toleranceBps: Long,

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
            val expected_amount_out: String,
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
): ContractMethod() {
    override val methodSignature = DepositWithExpiryMethod.methodSignature
    override fun getArguments() = listOf(inboundAddress, asset, amount, memo, expiry)

    companion object {
        const val methodSignature = "depositWithExpiry(address,address,uint256,string,uint256)"
    }
}


sealed class SwapError : Exception() {
    class NoDestinationAddress : SwapError()
}