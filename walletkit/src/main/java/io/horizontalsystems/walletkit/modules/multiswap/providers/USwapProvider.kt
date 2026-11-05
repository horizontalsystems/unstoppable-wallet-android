package io.horizontalsystems.walletkit.modules.multiswap.providers

import android.util.Base64
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.derivation
import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.core.nativeTokenQueries
import io.horizontalsystems.walletkit.modules.multiswap.SplitRateException
import io.horizontalsystems.walletkit.modules.multiswap.SplitSwapOutput
import io.horizontalsystems.walletkit.modules.multiswap.SplitSwapQuoteExtraData
import io.horizontalsystems.walletkit.modules.multiswap.SplitSwapRecipient
import io.horizontalsystems.walletkit.modules.multiswap.SplitSwapRules
import io.horizontalsystems.walletkit.modules.multiswap.SwapMevProtection
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitFailedException
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitFailure
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitment
import io.horizontalsystems.walletkit.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import io.horizontalsystems.walletkit.modules.multiswap.ThorChainSwapMemo
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.EvmTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldSlippage
import io.horizontalsystems.walletkit.core.stripHexPrefix
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.hexStringToByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.http.GET
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Collections

class USwapProvider(
    private val provider: UProvider,
    // Narrows the sell side to sources the host app can actually execute (e.g. a
    // route-calldata provider offered only where contract calls are possible).
    private val supportsSourceToken: (Token) -> Boolean = { true },
    // Whether /swap gets `sourceAddress` for this sell token — the server-built-tx
    // signal. An app that builds the source leg itself (e.g. a deposit transfer
    // batched into its own signer flow) turns it off so the server returns the
    // plain deposit route instead of building and gas-estimating a transaction.
    private val shouldIncludeSourceAddress: (Token) -> Boolean = { provider.consumesServerBuiltTx(it) },
    // Builds the EVM source leg of a transfer route: the deposit calldata the app's own
    // signer executes. Defaults to the EVM chain plugin's plain transfer; an app whose
    // signer flow needs another shape supplies its own.
    private val buildEvmDepositTransfer: ((tokenIn: Token, amountIn: BigDecimal, depositAddress: String) -> SendTransactionData.Evm)? = null,
) : ISplitSwapProvider {
    override val id = "u_${provider.id}"
    override val title = provider.title
    override val type = provider.type
    override val amlPrecheck = provider.amlPrecheck
    override val isEvm = provider.isEvm

    override fun isSingleTransactionSwap(tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String) = when (provider) {
        // LI.FI spans both: a same-chain pair is a plain single-tx DEX swap, a cross-chain
        // pair is deposit → bridge → delivery, so the flag depends on the pair, not the provider.
        UProvider.Lifi -> tokenInBlockchainTypeUid == tokenOutBlockchainTypeUid
        else -> provider.isSingleTransactionSwap
    }

    private val unstoppableAPI = APIClient.build(
        App.appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to App.appConfigProvider.uswapApiKey)
    ).create(UnstoppableAPI::class.java)

    private val blockchainTypes = chainIdBlockchainTypes

    private val chainIdByBlockchainType = blockchainTypes.entries.associate { (k, v) -> v to k }

    private var assetsMap = mapOf<Token, String>()
    private var supportedBlockchainTypes = setOf<BlockchainType>()

    private sealed class ProviderData {
        data class TokenMap(val map: Map<Token, String>) : ProviderData()
        data class ChainIds(val ids: List<String>) : ProviderData()
    }

    override suspend fun start() {
        // The Stellar DEX routes publish no token list and serve one chain, so the card never
        // waits on /v3/tokens: it is offered as soon as the swap screen opens, as the native
        // Stellar client was.
        if (provider == UProvider.Stellar) {
            supportedBlockchainTypes = setOf(BlockchainType.Stellar)
            return
        }

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
                BlockchainType.RobinhoodChain,
                BlockchainType.Arc,
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
                    val issuer = token.address
                    val tokenType = if (!issuer.isNullOrBlank()) {
                        // Classic asset: the server sends the issuer in `address` and the
                        // asset code in `ticker` (the Axelar ITS SHX case).
                        token.ticker?.let { TokenType.Asset(it, issuer) }
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

                BlockchainType.Thorchain,
                BlockchainType.Mayachain -> {
                    // No address on these chains: the identifier is the asset notation itself
                    // (THOR.RUNE, THOR.RUJI, BTC-BTC, MAYA.CACAO), which the chain's plugin
                    // resolves to the bank denom the token is keyed on.
                    ChainRegistry[blockchainType]?.swapAssetTokenType(token.identifier)?.let { tokenType ->
                        App.marketKit.token(TokenQuery(blockchainType, tokenType))?.let {
                            assetsMap[it] = token.identifier
                        }
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

                BlockchainType.Xrp -> {
                    // Native XRP only: the server lists no XRPL issued token (`address` is
                    // always null), and CanonicalAssetId maps TokenType.XrpAsset to no id, so
                    // a trust-line asset would be unsuspendable even if one appeared.
                    if (token.address.isNullOrBlank()) {
                        App.marketKit.token(TokenQuery(blockchainType, TokenType.Native))?.let {
                            assetsMap[it] = token.identifier
                        }
                    }
                }

                BlockchainType.Near -> {
                    // `address` is the NEP-141 contract; native NEAR has none (the server
                    // routes it through wrap.near itself, which is not the user's wNEAR token)
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        TokenType.Nep141(token.address)
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
        if (!supportsSourceToken(tokenFrom)) return false
        return if (assetsMap.isNotEmpty()) {
            assetsMap.contains(tokenFrom) && assetsMap.contains(tokenTo)
        } else {
            // A raw-address identifier names no chain: the request carries the sell token's
            // chainId for both sides, so such a pair is only expressible on one chain.
            (!usesRawAddressIdentifiers || tokenFrom.blockchainType == tokenTo.blockchainType) &&
                    tokenFrom.blockchainType in supportedBlockchainTypes &&
                    tokenTo.blockchainType in supportedBlockchainTypes &&
                    deriveIdentifier(tokenFrom) != null &&
                    deriveIdentifier(tokenTo) != null
        }
    }

    private val usesRawAddressIdentifiers: Boolean
        get() = when (provider) {
            UProvider.Jupiter, UProvider.Lifi, UProvider.Stellar -> false
            else -> true
        }

    // Raw asset encoding for providers that sync no token list from the server (the
    // /tokens response carries only supportedChainIds), so assets are encoded as raw
    // chain addresses instead of resolved through the asset map.
    private fun deriveIdentifier(token: Token): String? = when (provider) {
        // Solana-only: raw SPL mint encoding, case-sensitive base58 — pass verbatim, never
        // re-cased. The wSOL mint means native SOL server-side. The Solana check also makes
        // `supports()` reject non-Solana pairs (`Native` alone would match any chain).
        UProvider.Jupiter -> if (token.blockchainType != BlockchainType.Solana) {
            null
        } else when (val type = token.type) {
            TokenType.Native -> WSOL_MINT
            // The wSOL TOKEN is not swappable here: the server reads the wSOL mint as native
            // SOL, so SOL→wSOL degenerates to "same asset" (wrapping is not a swap) and
            // X→wSOL would deliver native SOL while the user watches the wSOL token balance.
            is TokenType.Spl -> type.address.takeIf { it != WSOL_MINT }
            else -> null
        }
        // No token list (BARTER-style), but LI.FI is CROSS-CHAIN, so each side must be
        // self-describing — the chain travels with the asset so the server resolves a
        // cross-chain pair without a shared `chainId` hint. EVM token → `<CHAIN>.<contract>`,
        // EVM native → `<CHAIN>.<0xeee…>` sentinel, Solana → `SOL.<mint>` (wSOL = native SOL),
        // Tron → `TRON.TRX` (native) / `TRON.<contract>` (TRC20).
        UProvider.Lifi -> when (token.blockchainType) {
            BlockchainType.Solana -> when (val type = token.type) {
                TokenType.Native -> "SOL.$WSOL_MINT"
                // The wSOL TOKEN is excluded for the same reason as on the JUPITER path above.
                is TokenType.Spl -> type.address.takeIf { it != WSOL_MINT }?.let { "SOL.$it" }
                else -> null
            }
            // Tron is TVM (base58 addresses, not `0x`): the server resolves `TRON.TRX` as native
            // and `TRON.<contract>` as a TRC20 — the EVM `0xeee…` native sentinel does not apply.
            // TRC20 contracts ride the `Eip20` type with a base58 address.
            BlockchainType.Tron -> when (val type = token.type) {
                TokenType.Native -> "TRON.TRX"
                is TokenType.Eip20 -> "TRON.${type.address}"
                else -> null
            }
            else -> lifiChainCodes[token.blockchainType]?.let { code ->
                when (val type = token.type) {
                    is TokenType.Eip20 -> "$code.${type.address}"
                    TokenType.Native -> "$code.0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
                    else -> null
                }
            }
        }
        // The server's self-describing Stellar identifiers: native → `XLM.XLM`, classic asset →
        // `XLM.<CODE>-<ISSUER>` with the code verbatim (case-sensitive). The issuer is a `G…`
        // account; a Soroban contract token (`C…`) is not a classic asset and is not served.
        UProvider.Stellar -> if (token.blockchainType != BlockchainType.Stellar) {
            null
        } else when (val type = token.type) {
            TokenType.Native -> "XLM.XLM"
            is TokenType.Asset -> type.issuer.takeIf { it.startsWith("G") }?.let { "XLM.${type.code}-$it" }
            else -> null
        }
        // Raw EVM address encoding (BARTER's server adapter expects addresses, not identifiers).
        else -> when (val type = token.type) {
            is TokenType.Eip20 -> type.address
            TokenType.Native if token.blockchainType.isEvm -> "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
            else -> null
        }
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): SwapQuote {
        val bestRoute = rateBestRoute(tokenIn, tokenOut, amountIn, BigDecimal("1"))

        val approvalAddress = bestRoute.approvalSpenderOrExecution
        val actionRequired = approvalAddress?.let { approvalAddress ->
            val plugin = ChainRegistry[tokenIn.blockchainType]
            val allowance = plugin?.eip20Allowance(tokenIn, approvalAddress)
            plugin?.eip20ApproveAction(allowance, amountIn, approvalAddress, tokenIn)
        } ?: ChainRegistry[tokenOut.blockchainType]?.swapActivationAction(tokenOut)

        return SwapQuote(
            amountOut = bestRoute.expectedBuyAmountOrZero,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = actionRequired,
            estimationTime = bestRoute.estimatedTime?.total,
            extraData = SwapQuoteExtraData(bestRoute)
        )
    }

    // /v3/rate — read-only price/route comparison narrowed to this provider. Picks the route
    // with the best expectedBuyAmount. On an Exolix ZEC pair it additionally fans out a
    // shielded (ZEC.ZECSHIELDED) variant and keeps the better-priced one; the winning
    // sell/buy asset travels back on the route so commitSwap can replay the exact variant.
    private suspend fun rateBestRoute(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        providers: Set<String> = rateProviders,
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
            providers = providers,
            chainId = chainId,
        )
        var bestRoute = unstoppableAPI.rate(request).routes?.maxByOrNull { it.expectedBuyAmountOrZero }
            ?: throw NoSuchElementException("No routes")

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
                    val bestRouteAlternate = unstoppableAPI.rate(requestAlternate).routes?.maxByOrNull { it.expectedBuyAmountOrZero }
                    if (bestRouteAlternate != null &&
                        bestRouteAlternate.expectedBuyAmountOrZero >= bestRoute.expectedBuyAmountOrZero
                    ) {
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

    // The server providers /v3/rate fans out over. A card normally quotes its one server provider;
    // the Stellar card quotes the Stellar DEX set and keeps the best-priced route. STELLARBROKER
    // itself is never requested: it has no service-fee mechanism, and its execution is a broker
    // WebSocket session this app has no client for.
    private val rateProviders: Set<String>
        get() = when (provider) {
            UProvider.Stellar -> STELLAR_ROUTE_PROVIDERS
            else -> setOf(provider.id)
        }

    // The server provider /v3/preview is asked for. The Stellar card previews with the provider
    // that won the rate fan-out — except for a third-party recipient: Aquarius settles on the
    // trader's own account and Soroban routes deliver to the signing account only, so just the
    // path-payment provider can serve one, and the fan-out is re-run over it when the winner is
    // another.
    private suspend fun previewProviderId(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        selectedRoute: UnstoppableAPI.Response.Route?,
        recipient: io.horizontalsystems.walletkit.entities.Address?,
        sourceAddress: String?,
    ): String {
        if (provider != UProvider.Stellar) return provider.id

        val thirdPartyRecipient = recipient != null && recipient.hex != sourceAddress
        val winner = selectedRoute?.providerId
        if (winner != null && (!thirdPartyRecipient || winner in STELLAR_RECIPIENT_CAPABLE_PROVIDERS)) {
            return winner
        }

        val providers = if (thirdPartyRecipient) STELLAR_RECIPIENT_CAPABLE_PROVIDERS else STELLAR_ROUTE_PROVIDERS
        return rateBestRoute(tokenIn, tokenOut, amountIn, slippage, providers).providerId
            ?: throw IllegalStateException("No Stellar route selected")
    }

    // /v3/preview — the committed route shape for this single provider, without creating an order.
    // `sellAsset`/`buyAsset` replay the variant the rate quote chose (Exolix ZEC). The route must
    // carry the token that [commit] later turns into an order; one without it cannot be executed.
    private suspend fun previewSwap(
        providerId: String,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        destinationAddress: String,
        sourceAddress: String?,
        refundAddress: String?,
        sellAsset: String?,
        buyAsset: String?,
        sendTransactionSettings: SendTransactionSettings?,
    ): UnstoppableAPI.Response.Route {
        val usingDerivedIdentifiers = assetsMap.isEmpty()
        val assetIn = sellAsset ?: assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn")
        val assetOut = buyAsset ?: assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut")
        val chainId = if (usingDerivedIdentifiers) chainIdByBlockchainType[tokenIn.blockchainType] else null

        val request = UnstoppableAPI.Request.Preview(
            sellAsset = assetIn,
            buyAsset = assetOut,
            sellAmount = amountIn.toPlainString(),
            slippage = slippage,
            provider = providerId,
            destinationAddress = destinationAddress,
            refundAddress = refundAddress,
            sourceAddress = sourceAddress,
            chainId = chainId,
            networkFee = UnstoppableAPI.Request.NetworkFee.of(tokenIn, sendTransactionSettings),
        )
        val route = unstoppableAPI.preview(request)

        if (route.previewToken.isNullOrEmpty()) {
            throw IllegalStateException("Preview carries no token")
        }

        return route
    }

    override suspend fun checkAmlAddresses(addresses: List<String>): Boolean? {
        return unstoppableAPI.checkAddresses(addresses.joinToString(",")).passedAmlCheck
    }

    // ----- Private send (same-asset confidential transfer) -----

    // The server-side id ("NEAR_CONFIDENTIAL"), matched against GET /providers by the
    // private send stack. `id` is the app-side "u_…" form used in swap records.
    val serverProviderId: String get() = provider.id

    fun supportsPrivateSend(token: Token) = supports(token, token)

    // /v3/rate in exact-output mode: same asset on both sides, buyAmount = what the recipient
    // must receive. Returns the whole response so the caller can read providerErrors when no
    // route comes back.
    suspend fun privateSendRate(
        token: Token,
        amountOut: BigDecimal,
        slippage: BigDecimal,
    ): UnstoppableAPI.Response.Rate = exactOutputRate(token, token, amountOut, slippage)

    // /v3/rate in exact-output mode across assets (the CrossPay shape): buyAmount = what the
    // recipient must receive in tokenOut, sellAmount comes back as the answer. Returns the
    // whole response so the caller can read providerErrors when no route comes back.
    suspend fun exactOutputRate(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigDecimal,
        slippage: BigDecimal,
    ): UnstoppableAPI.Response.Rate {
        val sellAsset = assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn")
        val buyAsset = assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut")
        val chainId = if (assetsMap.isEmpty()) chainIdByBlockchainType[tokenIn.blockchainType] else null

        return unstoppableAPI.rate(
            UnstoppableAPI.Request.Rate(
                sellAsset = sellAsset,
                buyAsset = buyAsset,
                slippage = slippage,
                providers = setOf(provider.id),
                chainId = chainId,
                buyAmount = amountOut.toPlainString(),
            )
        )
    }

    // Previews in exact-output mode. `sourceAddress` is deliberately never sent: it is optional
    // for transfer providers, and handing the provider the sender's address defeats the point
    // of a private send. The committed route's execution later carries the deposit address and
    // the exact amount to transfer.
    suspend fun privateSendPreview(
        token: Token,
        amountOut: BigDecimal,
        destinationAddress: String,
        refundAddress: String,
        slippage: BigDecimal,
    ): UnstoppableAPI.Response.Route =
        exactOutputPreview(token, token, amountOut, destinationAddress, refundAddress, slippage)

    // /v3/preview in exact-output mode across assets (the CrossPay shape): no order yet, the
    // route carries the token [commitPreview] turns into one. With [includeSourceAddress] the
    // swap screen's build-signal contract applies verbatim; private send keeps it off entirely.
    suspend fun exactOutputPreview(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigDecimal,
        destinationAddress: String,
        refundAddress: String,
        slippage: BigDecimal,
        includeSourceAddress: Boolean = false,
    ): UnstoppableAPI.Response.Route {
        val sellAsset = assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn")
        val buyAsset = assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut")
        val chainId = if (assetsMap.isEmpty()) chainIdByBlockchainType[tokenIn.blockchainType] else null

        val sourceAddress = if (includeSourceAddress && shouldIncludeSourceAddress(tokenIn)) {
            SwapHelper.getSendingAddressForToken(tokenIn)
        } else {
            null
        }

        val route = unstoppableAPI.preview(
            UnstoppableAPI.Request.Preview(
                sellAsset = sellAsset,
                buyAsset = buyAsset,
                slippage = slippage,
                provider = provider.id,
                destinationAddress = destinationAddress,
                refundAddress = refundAddress,
                sourceAddress = sourceAddress,
                chainId = chainId,
                buyAmount = amountOut.toPlainString(),
            )
        )

        if (route.previewToken.isNullOrEmpty()) {
            throw IllegalStateException("Preview carries no token")
        }

        return route
    }

    // /v3/commit for a route previewed here. Throws [UnstoppableAPI.CommitException] on refusal.
    // The commit's `execution` carries the deposit address, amount and attachment.
    suspend fun commitPreview(previewToken: String): UnstoppableAPI.Response.Commit {
        // A token is spent by its first commit, whatever the outcome: a refused one may be retried
        // server-side and run a second provider order, and an accepted one is replayed, which
        // would fund the same order twice. Either way the screen no longer describes it.
        if (!consumedPreviewTokens.add(previewToken)) {
            throw UnstoppableAPI.CommitException(null, "refresh_required", "Preview already committed", null)
        }

        currentCoroutineContext().ensureActive()

        val commit = try {
            unstoppableAPI.commit(UnstoppableAPI.Request.Commit(previewToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw UnstoppableAPI.CommitException.from(e)
        }

        if (commit.status != "ok" || commit.uuid.isNullOrEmpty()) {
            throw UnstoppableAPI.CommitException(null, commit.status, null, null)
        }

        return commit
    }

    /**
     * The deposit transfer for a route committed in exact-output mode: exactly
     * [depositAmount] (the route's execution.amount) to its deposit address, in the same
     * per-chain shapes the swap confirmation uses — including the attachment
     * deliverability rules. Chains that consume a server-built tx must have committed with
     * the source address included, exactly as the swap flow does.
     */
    fun depositTransactionData(
        tokenIn: Token,
        depositAmount: BigDecimal,
        route: UnstoppableAPI.Response.Route,
    ): SendTransactionData = getSendTransactionData(tokenIn, depositAmount, route)

    /**
     * The deposit transfer of exactly [depositAmount] to [depositAddress] carrying [attachment],
     * for a route whose execution is known from its parts: a committed order's deposit, or the
     * preview's stand-in address (with no attachment) the confirmation screen prices while the
     * real one does not exist yet.
     */
    fun depositTransactionData(
        tokenIn: Token,
        depositAmount: BigDecimal,
        depositAddress: String,
        attachment: UnstoppableAPI.Response.Attachment?,
    ): SendTransactionData {
        val route = UnstoppableAPI.Response.Route(
            sellAsset = null,
            buyAsset = null,
            expectedBuyAmount = null,
            minBuyAmount = null,
            estimatedTime = null,
            approvalSpender = null,
            execution = transferExecution(depositAddress, attachment),
            uuid = null,
        )
        return getSendTransactionData(tokenIn, depositAmount, route)
    }

    /**
     * The address a sell of [tokenIn] through this provider is refunded to: the wallet's own
     * address on the sell chain, which for ZEC is the transparent address unless the provider
     * takes a unified refund (see [ZcashSellAddresses]). Exact-output flows name it too, so a
     * CrossPay buffer refund follows the same policy as a swap refund.
     */
    suspend fun refundAddress(tokenIn: Token): String =
        if (tokenIn.blockchainType == BlockchainType.Zcash) {
            zcashSellAddresses(tokenIn).refund
        } else {
            SwapHelper.getReceiveAddressForToken(tokenIn)
        }

    private suspend fun zcashSellAddresses(tokenIn: Token): ZcashSellAddresses =
        ZcashSellAddresses.resolve(
            provider = provider,
            transparentAddress = { SwapHelper.getReceiveAddressForToken(tokenIn) },
            shieldedOnlyAddress = {
                ChainRegistry[BlockchainType.Zcash]?.swapShieldedRefundAddress(tokenIn)
                    ?: throw IllegalStateException("Zcash support is not available")
            },
        )

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: io.horizontalsystems.walletkit.entities.Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote {
        val selectedRoute = (swapQuote.extraData as? SwapQuoteExtraData)?.route

        // The delivery address of a ZEC output depends on how this provider settles, which only
        // /v3/providers says; a cold start with nothing cached must not fall back to transparent.
        // Only awaited while nothing is stored: a stale cache still answers, and the hourly sync
        // refreshes it on its own.
        if (tokenOut.blockchainType == BlockchainType.Zcash && !App.swapProviderInfoManager.hasExecutionTypes) {
            App.swapProviderInfoManager.sync()
        }

        val destination = when {
            recipient != null -> recipient.hex

            // A ZEC output goes to the wallet's unified address when the route delivers to a
            // shielded receiver: Exolix's shielded variant, or a vault-settled provider (per its
            // /v3/providers executionType), whose own Zcash client pays unified receivers.
            tokenOut.blockchainType == BlockchainType.Zcash &&
                    (routeDeliversShieldedZcash(swapQuote) || App.swapProviderInfoManager.deliversShieldedZcash(id, tokenIn)) -> {
                ChainRegistry[BlockchainType.Zcash]?.swapUnifiedReceiveAddress(tokenOut)
                    ?: throw IllegalStateException("Zcash support is not available")
            }

            else -> SwapHelper.getReceiveAddressForToken(tokenOut)
        }

        // sourceAddress is the build signal — send it only for chains whose server-built
        // signed_transaction / thorchain_deposit tx we consume (by default EVM/Tron/TON/Solana,
        // and Stellar on the Stellar DEX card), where it is also required for the tx `from`. For
        // UTXO/Monero/Zano (and any token the host app opts out of) we omit it. A transfer
        // route's deposit is built locally on every chain, whether or not the address was sent.
        // A ZEC sell is the exception: its source and refund are one decision, the provider's
        // refund policy (ZcashSellAddresses), independent of the build signal.
        val sourceAddress: String?
        val refundAddress: String
        if (tokenIn.blockchainType == BlockchainType.Zcash) {
            val addresses = zcashSellAddresses(tokenIn)
            sourceAddress = addresses.source
            refundAddress = addresses.refund
        } else {
            sourceAddress = if (shouldIncludeSourceAddress(tokenIn)) {
                SwapHelper.getSendingAddressForToken(tokenIn)
            } else {
                null
            }
            refundAddress = SwapHelper.getReceiveAddressForToken(tokenIn)
        }

        val previewProviderId = previewProviderId(tokenIn, tokenOut, amountIn, slippage, selectedRoute, recipient, sourceAddress)

        val bestRoute = previewSwap(
            previewProviderId,
            tokenIn,
            tokenOut,
            amountIn,
            slippage,
            destination,
            sourceAddress,
            refundAddress,
            selectedRoute?.sellAsset,
            selectedRoute?.buyAsset,
            sendTransactionSettings,
        )

        // A THORChain-style deposit memo is what routes the outbound leg: a route without one
        // cannot be delivered, and one whose destination is not the recipient shown to the user
        // must not be signed.
        bestRoute.execution?.takeIf { ThorChainDepositRoute.isVaultDeposit(it) }?.let { execution ->
            val memo = execution.memo ?: throw IllegalStateException("No THORChain deposit memo")
            ThorChainSwapMemo.requireDestination(memo, destination, tokenOut.blockchainType)
        }

        val amountOut = bestRoute.expectedBuyAmountOrZero

        // The server's `minBuyAmount` is the enforced floor the route can deliver — show it
        // directly as the "Guaranteed" amount instead of deriving one from slippage. `null`
        // means the route is a floating P2P estimate — nothing guarantees the amount (or applies
        // our slippage), so the confirm page must not show the "Guaranteed" or slippage rows.
        val amountOutMin = bestRoute.minBuyAmount
        val effectiveSlippage = if (bestRoute.minBuyAmount != null) slippage else null

        val fields = buildList {
            recipient?.let {
                add(DataFieldRecipient(it))
            }
            DataFieldSlippage.getField(effectiveSlippage)?.let {
                add(it)
            }
        }

        val execution = bestRoute.execution
        val sendTransactionData = if (execution != null) {
            getSendTransactionData(tokenIn, amountIn, bestRoute)
        } else {
            // A transfer provider's deposit address exists only once the order does, so the
            // confirmation screen estimates the transfer against the server's stand-in account,
            // and [commit] rebuilds it for the real address. The screen shows no deposit address
            // meanwhile: the stand-in must never be mistaken for one. Without a stand-in (some
            // sell chains have none configured) there is nothing to estimate against, and the
            // quote is shown without a network fee instead of being refused.
            bestRoute.stubDepositAddress?.takeIf { it.isNotBlank() }?.let { stub ->
                getSendTransactionData(tokenIn, amountIn, bestRoute.copy(execution = transferExecution(stub, estimationAttachment(tokenIn))))
            }
        }

        return SwapFinalQuote(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = amountOut,
            amountOutMin = amountOutMin,
            sendTransactionData = sendTransactionData,
            priceImpact = null,
            fields = fields,
            estimatedTime = bestRoute.estimatedTime?.total,
            slippage = effectiveSlippage,
            providerSwapId = null,
            fromAsset = assetsMap[tokenIn] ?: deriveIdentifier(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn"),
            toAsset = assetsMap[tokenOut] ?: deriveIdentifier(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut"),
            depositAddress = execution?.resolvedDepositAddress(),
            // XRP's identifier is a destination tag, not a memo — the XRP send service shows it
            // under its own label, so showing it here again as a "deposit memo" would misname it.
            depositMemo = execution
                ?.takeIf { tokenIn.blockchainType != BlockchainType.Xrp }
                ?.resolvedMemo(),
            approvalSpender = bestRoute.approvalSpenderOrExecution,
            providerContext = PreviewContext(
                previewToken = bestRoute.previewToken ?: throw IllegalStateException("Preview carries no token"),
                route = bestRoute,
                tokenIn = tokenIn,
                amountIn = amountIn,
            ),
            mevProtectionAllowed = SwapMevProtection.allowedForRoute(execution, tokenIn, tokenOut),
        )
    }

    // A plain-transfer execution known from its parts, so the per-chain builders serve a deposit
    // whether it came in a route or is being priced against the preview's stand-in address.
    private fun transferExecution(depositAddress: String, attachment: UnstoppableAPI.Response.Attachment?) =
        UnstoppableAPI.Response.Execution(
            method = "transfer",
            chain = null,
            transactions = null,
            approval = null,
            depositAddress = depositAddress,
            amount = null,
            asset = null,
            attachment = attachment,
            unsignedTx = null,
            protocol = null,
            inboundAddress = null,
            memo = null,
            delivery = null,
        )

    // The attachment a stand-in estimate carries: Stellar payments to a provider name a memo, so a
    // placeholder stands in for its size and for the builder's requirement that one is present.
    private fun estimationAttachment(tokenIn: Token): UnstoppableAPI.Response.Attachment? =
        if (tokenIn.blockchainType == BlockchainType.Stellar) UnstoppableAPI.Response.Attachment("text", "0") else null

    override suspend fun commit(finalQuote: SwapFinalQuote): SwapCommitment {
        // A split preview is committed through its own endpoint; its token is refused by /v3/commit.
        (finalQuote.providerContext as? SplitPreviewContext)?.let { return commitSplit(finalQuote, it) }

        // Every quote this provider returns carries a context; a missing one can only mean the
        // quote came from elsewhere, and executing it as-is would skip creating the order.
        val context = finalQuote.providerContext as? PreviewContext
            ?: throw SwapCommitFailedException(SwapCommitFailure.Failed, IllegalStateException("Quote was not previewed"))

        // A consumed token can never create a second order (the server replays the first), but
        // committing it again would hand back a deposit the screen no longer describes.
        if (!consumedPreviewTokens.add(context.previewToken)) {
            throw SwapCommitFailedException(SwapCommitFailure.RefreshRequired)
        }

        // commit has side effects: never create an order from a cancelled task
        currentCoroutineContext().ensureActive()

        val commit = try {
            unstoppableAPI.commit(UnstoppableAPI.Request.Commit(context.previewToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val commitException = UnstoppableAPI.CommitException.from(e)
            throw SwapCommitFailedException(commitException.toSwapCommitFailure(), commitException)
        }

        val uuid = commit.uuid
        if (commit.status != "ok" || uuid.isNullOrEmpty()) {
            throw SwapCommitFailedException(SwapCommitFailure.Failed, IllegalStateException("Commit answered ${commit.status} without a uuid"))
        }

        val execution = commit.execution
            ?: run {
                // Nothing to rebuild: only a preview that carried its own execution can be sent as
                // previewed. A transfer preview was built against the stand-in and has nowhere to go.
                if (context.route.execution == null) {
                    throw SwapCommitFailedException(SwapCommitFailure.Failed, IllegalStateException("Commit returned no deposit"))
                }
                return SwapCommitment(finalQuote.copy(providerSwapId = uuid, providerContext = null), uuid)
            }

        try {
            // A signed / thorchain preview already carries its final transaction; a different one
            // at commit would mean signing something the user never reviewed. Only a plain transfer
            // is ever rebuilt here, from its deposit address and attachment.
            val previewedExecution = context.route.execution
            if (previewedExecution != null && previewedExecution.method != "transfer") {
                throw IllegalStateException("Commit replaced a ${previewedExecution.method} execution")
            }
            if (execution.method != "transfer") {
                throw IllegalStateException("Commit returned a ${execution.method} execution")
            }
            // The order's floor can only hold or improve on what the user confirmed.
            val previewedMin = context.route.minBuyAmount
            val committedMin = commit.minBuyAmount
            if (previewedMin != null && committedMin != null && committedMin < previewedMin) {
                throw IllegalStateException("Commit lowered the guaranteed amount")
            }

            // The transfer moves the amount the user saw; the commit's sellAmount is not consulted.
            val route = context.route.merging(commit)
            val quote = finalQuote.copy(
                amountOut = route.expectedBuyAmountOrZero,
                amountOutMin = route.minBuyAmount,
                sendTransactionData = getSendTransactionData(context.tokenIn, context.amountIn, route),
                providerSwapId = uuid,
                depositAddress = execution.resolvedDepositAddress(),
                depositMemo = execution
                    .takeIf { context.tokenIn.blockchainType != BlockchainType.Xrp }
                    ?.resolvedMemo(),
                providerContext = null,
            )
            return SwapCommitment(quote, uuid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SwapCommitFailedException) {
            throw e
        } catch (e: Throwable) {
            // The order exists server-side and lapses unfunded; the user gets a fresh quote.
            throw SwapCommitFailedException(SwapCommitFailure.Failed, e)
        }
    }

    override suspend fun reportInboundTxHash(providerSwapId: String, txHash: String) {
        unstoppableAPI.signed(UnstoppableAPI.Request.Signed(providerSwapId, txHash))
    }

    // ----- Split swaps: one deposit, several outputs of the same buy token -----

    override suspend fun fetchSplitQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        percents: List<Int>,
    ): SwapQuote {
        SplitSwapRules.requireValid(percents)

        val route = splitRateBestRoute(tokenIn, tokenOut, amountIn, percents, IMultiSwapProvider.DEFAULT_SLIPPAGE)

        return SwapQuote(
            amountOut = route.expectedBuyAmountTotal,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            // The deposit is a plain transfer, so the sell side never needs an approval; the buy
            // side may still need the account to accept the token (a Stellar trustline).
            actionRequired = ChainRegistry[tokenOut.blockchainType]?.swapActivationAction(tokenOut),
            estimationTime = route.estimatedTime?.total,
            extraData = SplitSwapQuoteExtraData(route, percents),
        )
    }

    // /v3/split/rate — prices the shares for this provider alone. A refusal is a non-2xx carrying
    // providerErrors (a share under the per-output minimum, a pair the provider does not quote).
    private suspend fun splitRateBestRoute(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        percents: List<Int>,
        slippage: BigDecimal,
    ): UnstoppableAPI.Response.SplitRoute {
        val assetOut = assetIdentifier(tokenOut)
        val request = UnstoppableAPI.Request.SplitRate(
            sellAsset = assetIdentifier(tokenIn),
            sellAmount = amountIn.toPlainString(),
            slippage = slippage,
            outputs = percents.map { UnstoppableAPI.Request.SplitOutput(buyAsset = assetOut, amount = it.toString()) },
            providers = setOf(provider.id),
            chainId = rateChainId(tokenIn),
        )

        val response = try {
            unstoppableAPI.splitRate(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw SplitRateException.from(e)
        }

        // A route must price every requested output, or an output would have no amount to show.
        return response.routes.orEmpty()
            .filter { route -> route.outputsByIndex.map { it.index } == percents.indices.toList() }
            .maxByOrNull { it.expectedBuyAmountTotal }
            ?: throw SplitRateException(response.providerErrors.orEmpty(), "No split routes")
    }

    override suspend fun fetchSplitFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        recipients: List<SplitSwapRecipient>,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        slippage: BigDecimal,
    ): SwapFinalQuote {
        SplitSwapRules.requireValid(recipients.map { it.percent })

        // The same decision as for a single swap: where a failed deposit is refunded, and whether
        // the sender is named. A split is always a transfer the app builds, so the source
        // address is only recorded as the sender; the ZEC refund policy still decides both.
        val sourceAddress: String?
        val refundAddress: String
        if (tokenIn.blockchainType == BlockchainType.Zcash) {
            val addresses = zcashSellAddresses(tokenIn)
            sourceAddress = addresses.source
            refundAddress = addresses.refund
        } else {
            sourceAddress = if (shouldIncludeSourceAddress(tokenIn)) {
                SwapHelper.getSendingAddressForToken(tokenIn)
            } else {
                null
            }
            refundAddress = SwapHelper.getReceiveAddressForToken(tokenIn)
        }

        val assetIn = assetIdentifier(tokenIn)
        val assetOut = assetIdentifier(tokenOut)
        val request = UnstoppableAPI.Request.SplitPreview(
            sellAsset = assetIn,
            sellAmount = amountIn.toPlainString(),
            slippage = slippage,
            provider = provider.id,
            refundAddress = refundAddress,
            sourceAddress = sourceAddress,
            chainId = rateChainId(tokenIn),
            networkFee = UnstoppableAPI.Request.NetworkFee.of(tokenIn, sendTransactionSettings),
            outputs = recipients.map {
                UnstoppableAPI.Request.SplitOutput(buyAsset = assetOut, amount = it.percent.toString(), destinationAddress = it.address.hex)
            },
        )

        val route = try {
            unstoppableAPI.splitPreview(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw SplitRateException.from(e)
        }

        val previewToken = route.previewToken?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Preview carries no token")

        val outputs = previewedOutputs(route, recipients)

        // A split preview never names the deposit: the transfer is estimated against the
        // server's stand-in account and rebuilt for the real address at commit, exactly as a
        // single transfer provider's is. Without a stand-in the quote is shown without a fee.
        val sendTransactionData = route.stubDepositAddress?.takeIf { it.isNotBlank() }?.let { stub ->
            depositTransactionData(tokenIn, amountIn, stub, estimationAttachment(tokenIn))
        }

        // As for a single swap, a floor exists only where the server enforces one: the
        // Guaranteed and slippage rows show only when every output's amount is guaranteed.
        val amountOutMin = route.minBuyAmountTotal
        val effectiveSlippage = if (amountOutMin != null) slippage else null

        return SwapFinalQuote(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = outputs.fold(BigDecimal.ZERO) { sum, output -> sum + output.amountOut },
            amountOutMin = amountOutMin,
            sendTransactionData = sendTransactionData,
            priceImpact = null,
            fields = listOfNotNull(DataFieldSlippage.getField(effectiveSlippage)),
            estimatedTime = route.estimatedTime?.total,
            slippage = effectiveSlippage,
            providerSwapId = null,
            fromAsset = assetIn,
            toAsset = assetOut,
            depositAddress = null,
            depositMemo = null,
            approvalSpender = null,
            providerContext = SplitPreviewContext(
                previewToken = previewToken,
                tokenIn = tokenIn,
                amountIn = amountIn,
                recipients = recipients,
                previewedMins = route.outputsByIndex.map { it.minBuyAmount },
            ),
            mevProtectionAllowed = false,
            splitOutputs = outputs,
        )
    }

    // The previewed outputs matched to the recipients by index. Each must name the destination
    // that was sent: a preview paying elsewhere must not reach the confirmation screen.
    private fun previewedOutputs(
        route: UnstoppableAPI.Response.SplitRoute,
        recipients: List<SplitSwapRecipient>,
    ): List<SplitSwapOutput> {
        val outputs = route.outputsByIndex
        requireOneOutputPerRecipient(outputs.map { it.index }, recipients)
        return recipients.mapIndexed { index, recipient ->
            val output = outputs[index]
            requireSameDestination(output.destinationAddress, recipient, index)
            SplitSwapOutput(
                index = index,
                percent = recipient.percent,
                address = recipient.address.hex,
                amountOut = output.expectedBuyAmount ?: BigDecimal.ZERO,
            )
        }
    }

    private fun requireSameDestination(destinationAddress: String?, recipient: SplitSwapRecipient, index: Int) {
        if (destinationAddress != null && destinationAddress != recipient.address.hex) {
            throw IllegalStateException("Output $index names another destination")
        }
    }

    // Exactly one output per recipient, indexed as requested. A missing one leaves a recipient
    // unpaid; a surplus or repeated one would be summed into the split's totals unseen.
    private fun requireOneOutputPerRecipient(sortedIndices: List<Int>, recipients: List<SplitSwapRecipient>) {
        if (sortedIndices != recipients.indices.toList()) {
            throw IllegalStateException("Outputs $sortedIndices do not match ${recipients.size} recipients")
        }
    }

    // Both lists are in recipient order, one entry per recipient.
    private fun requireFloorsKept(previewed: List<BigDecimal?>, committed: List<BigDecimal?>) {
        previewed.forEachIndexed { index, previewedMin ->
            if (previewedMin == null) return@forEachIndexed
            val committedMin = committed.getOrNull(index)
            if (committedMin == null || committedMin < previewedMin) {
                throw IllegalStateException("Commit withdrew the guaranteed amount of output $index")
            }
        }
    }

    // /v3/split/commit for a split previewed here: creates the order, then rebuilds the one
    // transfer for the deposit address the order names.
    private suspend fun commitSplit(finalQuote: SwapFinalQuote, context: SplitPreviewContext): SwapCommitment {
        if (!consumedPreviewTokens.add(context.previewToken)) {
            throw SwapCommitFailedException(SwapCommitFailure.RefreshRequired)
        }

        currentCoroutineContext().ensureActive()

        val commit = try {
            unstoppableAPI.splitCommit(UnstoppableAPI.Request.Commit(context.previewToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val commitException = UnstoppableAPI.CommitException.from(e)
            throw SwapCommitFailedException(commitException.toSwapCommitFailure(), commitException)
        }

        val splitUuid = commit.splitUuid
        if (commit.status != "ok" || splitUuid.isNullOrEmpty()) {
            throw SwapCommitFailedException(SwapCommitFailure.Failed, IllegalStateException("Commit answered ${commit.status} without a splitUuid"))
        }

        try {
            val execution = commit.execution ?: throw IllegalStateException("Commit returned no deposit")
            if (execution.method != "transfer") {
                throw IllegalStateException("Commit returned a ${execution.method} execution")
            }
            // The order is funded with exactly the amount the user confirmed, once: an order
            // asking for anything else is left unfunded and lapses.
            val orderAmount = execution.amount?.toBigDecimalOrNull()
                ?: throw IllegalStateException("Commit names no deposit amount")
            if (orderAmount.compareTo(context.amountIn) != 0) {
                throw IllegalStateException("Commit asks for a different deposit amount")
            }
            val depositAddress = execution.depositAddress?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("No deposit address")

            val outputs = committedOutputs(commit, finalQuote.splitOutputs.orEmpty(), context.recipients)
            // Each recipient's floor can only hold or improve on what was previewed: a commit
            // that lowers one, or stops guaranteeing it, is not funded. Judged per output, since
            // an unchanged total could hide one recipient's loss behind another's gain.
            requireFloorsKept(context.previewedMins, commit.outputsByIndex.map { it.minBuyAmount })
            val quote = finalQuote.copy(
                amountOut = outputs.fold(BigDecimal.ZERO) { sum, output -> sum + output.amountOut },
                amountOutMin = commit.minBuyAmountTotal,
                sendTransactionData = depositTransactionData(context.tokenIn, context.amountIn, depositAddress, execution.attachment),
                providerSwapId = splitUuid,
                depositAddress = depositAddress,
                depositMemo = execution
                    .takeIf { context.tokenIn.blockchainType != BlockchainType.Xrp }
                    ?.resolvedMemo(),
                providerContext = null,
                splitOutputs = outputs,
            )
            return SwapCommitment(quote, splitUuid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SwapCommitFailedException) {
            throw e
        } catch (e: Throwable) {
            // The order exists server-side and lapses unfunded; the user gets a fresh quote.
            throw SwapCommitFailedException(SwapCommitFailure.Failed, e)
        }
    }

    // The committed outputs matched to the recipients by index: each must carry its own uuid and
    // name the destination that was previewed. The committed amount replaces the previewed one.
    private fun committedOutputs(
        commit: UnstoppableAPI.Response.SplitCommit,
        previewed: List<SplitSwapOutput>,
        recipients: List<SplitSwapRecipient>,
    ): List<SplitSwapOutput> {
        val outputs = commit.outputsByIndex
        requireOneOutputPerRecipient(outputs.map { it.index }, recipients)
        return recipients.mapIndexed { index, recipient ->
            val output = outputs[index]
            val uuid = output.uuid?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("Output $index has no uuid")
            requireSameDestination(output.destinationAddress, recipient, index)
            SplitSwapOutput(
                index = index,
                percent = recipient.percent,
                address = recipient.address.hex,
                amountOut = output.expectedBuyAmount ?: previewed.getOrNull(index)?.amountOut ?: BigDecimal.ZERO,
                uuid = uuid,
            )
        }
    }

    override suspend fun reportSplitInboundTxHash(splitUuid: String, txHash: String) {
        unstoppableAPI.splitSigned(UnstoppableAPI.Request.SplitSigned(splitUuid, txHash))
    }

    private fun assetIdentifier(token: Token): String =
        assetsMap[token] ?: deriveIdentifier(token) ?: throw IllegalStateException("No identifier for ${token.coin.code}")

    // The sell chain hint a raw-address identifier needs; a token-list identifier names its chain.
    private fun rateChainId(tokenIn: Token): String? =
        if (assetsMap.isEmpty()) chainIdByBlockchainType[tokenIn.blockchainType] else null

    // What [commitSplit] needs from the preview: the token to commit, the deposit the transfer
    // is rebuilt for, the recipients the order's outputs are checked against, and each output's
    // previewed floor (in recipient order) the committed ones must keep.
    private class SplitPreviewContext(
        val previewToken: String,
        val tokenIn: Token,
        val amountIn: BigDecimal,
        val recipients: List<SplitSwapRecipient>,
        val previewedMins: List<BigDecimal?>,
    )

    // What [commit] needs from the preview: the token to commit and the route to rebuild a transfer
    // provider's deposit from once the real address arrives.
    private class PreviewContext(
        val previewToken: String,
        val route: UnstoppableAPI.Response.Route,
        val tokenIn: Token,
        val amountIn: BigDecimal,
    )

    // Tokens already handed to /v3/commit. Shared by every instance: a token is bound to the
    // provider that previewed it, and the registry holds one instance per provider.
    private val consumedPreviewTokens: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    // A deposit attachment the chain cannot deliver readably must fail the route rather than
    // be silently kept local (Monero user note) or sent encrypted (Zano comment, shielded
    // Zcash memo): the provider matches the incoming deposit to its order by this identifier,
    // and a deposit it cannot match is typically unrecoverable.
    private fun resolvedDeliverableMemo(
        execution: UnstoppableAPI.Response.Execution,
        blockchainType: BlockchainType,
    ): String? {
        val memo = execution.resolvedMemo() ?: return null
        if (!blockchainType.memoDelivery.deliversAttachment) {
            throw IllegalStateException("Deposit attachment cannot be delivered on $blockchainType")
        }
        return memo
    }

    private fun getSendTransactionData(
        tokenIn: Token,
        amountIn: BigDecimal,
        bestRoute: UnstoppableAPI.Response.Route,
    ): SendTransactionData {
        val blockchainType = tokenIn.blockchainType
        val execution = bestRoute.execution ?: throw IllegalStateException("No execution found")

        if (blockchainType.isEvm) {
            if (execution.method == "transfer") {
                // A plain transfer the app builds itself, so the same code prices the stand-in at
                // preview and the real deposit at commit; the server's unsignedTx is not consumed.
                // EVM carries no memo: an attachment would never reach the provider.
                resolvedDeliverableMemo(execution, blockchainType)
                val depositAddress = execution.depositAddress ?: throw IllegalStateException("No deposit address")

                return buildEvmDepositTransfer?.invoke(tokenIn, amountIn, depositAddress)
                    ?: ChainRegistry[blockchainType]?.depositTransferData(tokenIn, amountIn, depositAddress)
                    ?: throw IllegalStateException("No evm transfer builder for $blockchainType")
            }

            val signable = execution.primarySignable?.takeIf { it.kind == "evm" }
                ?: throw IllegalStateException("No evm tx found")
            val to = signable.to ?: throw IllegalStateException("No tx `to`")
            ThorChainDepositRoute.requireEvmTarget(to, execution)

            val transactionData = EvmTransactionData(
                to = to,
                value = BigInteger((signable.value ?: "0x0").stripHexPrefix(), 16),
                input = (signable.data ?: "0x").hexStringToByteArray()
            )
            ThorChainDepositRoute.requireEvmCalldata(transactionData.input, transactionData.value, execution, tokenIn, amountIn)

            return SendTransactionData.Evm(
                transactionData = transactionData,
                gasLimit = signable.gas?.let { java.math.BigInteger(it.stripHexPrefix(), 16).toLong() },
            )
        }

        when (blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
            BlockchainType.ECash,
                -> {
                // A vault deposit (thorchain_deposit) refunds to the transaction's first input and
                // cannot pay a taproot address, so it spends only non-taproot UTXOs and returns
                // change to the first input. Every other route is a plain send, which only the
                // providers flagged for it accept.
                val vaultDeposit = ThorChainDepositRoute.isVaultDeposit(execution)
                if (!vaultDeposit && !provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple BTC tx providers are supported")
                }

                return SendTransactionData.Btc(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    // Deliverable here: the memo becomes an OP_RETURN output the provider can read.
                    memo = resolvedDeliverableMemo(execution, blockchainType),
                    amount = amountIn,
                    recommendedGasRate = null,
                    minimumSendAmount = null,
                    changeToFirstInput = vaultDeposit,
                    utxoFilters = if (vaultDeposit) VAULT_DEPOSIT_UTXO_FILTERS else UtxoFilters(),
                )
            }

            BlockchainType.Thorchain,
            BlockchainType.Mayachain -> {
                if (execution.method == "transfer") {
                    // A deposit to a provider's own address, as every other chain builds it.
                    // Deliverable here: the bank memo is a plain, publicly readable field.
                    return SendTransactionData.Thorchain.Send(
                        address = execution.depositAddress ?: throw IllegalStateException("No deposit address"),
                        amount = amountIn,
                        memo = resolvedDeliverableMemo(execution, blockchainType).orEmpty(),
                    )
                }

                return when (val transfer = ThorChainDepositRoute.settlementTransfer(execution)) {
                    is ThorChainDepositRoute.SettlementTransfer.Send -> SendTransactionData.Thorchain.Send(
                        address = transfer.inboundAddress,
                        amount = amountIn,
                        memo = transfer.memo,
                    )

                    is ThorChainDepositRoute.SettlementTransfer.Deposit -> SendTransactionData.Thorchain.Deposit(
                        asset = assetsMap[tokenIn] ?: throw IllegalStateException("No identifier for tokenIn"),
                        amount = amountIn,
                        memo = transfer.memo,
                    )
                }
            }

            BlockchainType.Solana -> {
                if (execution.method == "transfer") {
                    // Built locally like the EVM transfer; a plain SOL / SPL send carries no memo.
                    resolvedDeliverableMemo(execution, blockchainType)
                    return SendTransactionData.Solana.Simple(
                        address = execution.depositAddress ?: throw IllegalStateException("No deposit address"),
                        amount = amountIn,
                    )
                }

                val message = execution.primarySignable?.takeIf { it.kind == "solana" }?.message
                    ?: throw IllegalStateException("No solana tx found")

                return SendTransactionData.Solana.WithRawTransaction(
                    Base64.decode(message, Base64.DEFAULT)
                )
            }

            BlockchainType.Tron -> {
                if (execution.method == "transfer") {
                    // Built locally like the EVM transfer; a simple send cannot carry the
                    // provider's crediting identifier.
                    resolvedDeliverableMemo(execution, blockchainType)
                    return SendTransactionData.Tron.Simple(
                        address = execution.depositAddress ?: throw IllegalStateException("No deposit address"),
                        amount = amountIn,
                    )
                }

                val tx = execution.primarySignable?.takeIf { it.kind == "tron" }?.tx
                    ?: throw IllegalStateException("No tron tx found")

                return SendTransactionData.Tron.WithCreateTransaction(tx.toString())
            }

            BlockchainType.Stellar -> {
                // Server-built XDR envelope (signed_transaction — Axelar ITS' Stellar leg):
                // sign and submit it. Otherwise a deposit-address transfer (P2P providers)
                // with the provider's binding memo.
                execution.primarySignable?.takeIf { it.kind == "stellar" }?.xdr?.let { xdr ->
                    return SendTransactionData.Stellar.WithTransactionEnvelope(xdr)
                }

                // Deliverable here: a Stellar text memo is a plain, publicly readable field of
                // the payment transaction.
                val memo = resolvedDeliverableMemo(execution, blockchainType)
                    ?: throw IllegalStateException("No memo found")

                return SendTransactionData.Stellar.Regular(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    memo = memo,
                    amount = amountIn
                )
            }

            BlockchainType.Ton -> {
                if (execution.method == "transfer") {
                    // Built locally like the EVM transfer. Deliverable here: a TON comment is a
                    // plain, publicly readable field of the message.
                    return SendTransactionData.Ton.Regular(
                        address = execution.depositAddress ?: throw IllegalStateException("No deposit address"),
                        amount = amountIn,
                        memo = resolvedDeliverableMemo(execution, blockchainType),
                    )
                }

                val tx = execution.primarySignable?.takeIf { it.kind == "ton" }?.tx
                    ?: throw IllegalStateException("No ton tx found")

                return SendTransactionData.Ton.SendRequest(JSONObject(tx.toString()))
            }

            BlockchainType.Xrp -> {
                // The XRP payment carries no memo, and a vault deposit without its memo is a
                // deposit the vault cannot match: refused rather than sent.
                if (ThorChainDepositRoute.isVaultDeposit(execution)) {
                    throw IllegalStateException("XRP cannot carry a vault deposit memo")
                }

                // The identifier travels in the Payment's DestinationTag field rather than a
                // memo, so this builds the transfer from the tag — see [resolvedDestinationTag],
                // which refuses any attachment XRP cannot carry there.
                return SendTransactionData.Xrp(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    destinationTag = execution.resolvedDestinationTag(),
                )
            }

            BlockchainType.Near -> {
                // A deposit-address transfer: NEAR, or `ft_transfer` for a NEP-141 token. A
                // native NEAR transfer has no memo field, so a route that needs one fails in
                // resolvedDeliverableMemo rather than sending a deposit the provider cannot match.
                return SendTransactionData.Near(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = resolvedDeliverableMemo(execution, blockchainType),
                )
            }

            BlockchainType.Zcash -> {
                if (ThorChainDepositRoute.isVaultDeposit(execution)) {
                    val deposit = ThorChainDepositRoute.shieldedMemoDeposit(execution)
                    return SendTransactionData.Zcash.ShieldedMemo(
                        address = deposit.inboundAddress,
                        amount = amountIn,
                        memo = deposit.memo,
                        memoShieldedAddress = deposit.memoShieldedAddress,
                    )
                }

                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple ZEC tx providers are supported")
                }

                // Throws on any attachment: the memo would ride shielded (encrypted, unverifiable
                // that the provider reads it) or not at all on a transparent address. Neither
                // delivers the crediting identifier, and a deposit the provider cannot match is
                // unrecoverable.
                return SendTransactionData.Zcash.Regular(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = resolvedDeliverableMemo(execution, blockchainType) ?: ""
                )
            }

            BlockchainType.Monero -> {
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple XMR tx providers are supported")
                }

                // Throws on any attachment: a Monero memo never leaves this device — the adapter
                // stores it as a wallet user note against the tx id, so the provider can never
                // read the identifier it needs to match this deposit.
                return SendTransactionData.Monero(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = resolvedDeliverableMemo(execution, blockchainType)
                )
            }

            BlockchainType.Zano -> {
                if (!provider.supportsSimpleUtxoTransactions) {
                    throw IllegalStateException("Only simple ZANO tx providers are supported")
                }

                // Throws on any attachment: the adapter passes a Zano memo as the transfer's
                // "comment", which travels encrypted on-chain — whether the provider decrypts
                // and reads it is unverifiable from here, and the failure mode is lost funds.
                return SendTransactionData.Zano(
                    address = execution.resolvedDepositAddress() ?: throw IllegalStateException("No deposit address"),
                    amount = amountIn,
                    memo = resolvedDeliverableMemo(execution, blockchainType)
                )
            }

            else -> Unit
        }

        throw IllegalArgumentException("Not supported blockchainType: $blockchainType")
    }

    companion object {
        /**
         * Whether the route a quote selected delivers ZEC to a shielded receiver on its own,
         * whatever the provider's execution type: Exolix's shielded variant does. The recipient
         * rule and the delivery address must agree on it.
         */
        fun routeDeliversShieldedZcash(swapQuote: SwapQuote): Boolean =
            (swapQuote.extraData as? SwapQuoteExtraData)?.route?.buyAsset == ZCASH_SHIELDED_ASSET

        // The server's chain identifiers. Shared: instances resolve tokens through it, and the
        // private send stack uses it to recognise a committed route's execution.chain — which is
        // sometimes a chain id ("56") and sometimes a name ("bsc"), so only a recognised chain
        // resolving to a DIFFERENT blockchain counts as a mismatch there.
        val chainIdBlockchainTypes = mapOf(
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
            "ripple" to BlockchainType.Xrp,
            "near" to BlockchainType.Near,
            "ton" to BlockchainType.Ton,
            "dash" to BlockchainType.Dash,
            "ecash" to BlockchainType.ECash,
            "monero" to BlockchainType.Monero,
            "zano" to BlockchainType.Zano,
            "100" to BlockchainType.Gnosis,
            "4663" to BlockchainType.RobinhoodChain,
            "5042" to BlockchainType.Arc,
            "thorchain-1" to BlockchainType.Thorchain,
            "mayachain-mainnet-v1" to BlockchainType.Mayachain,
//            "" to BlockchainType.Fantom,
//            "" to BlockchainType.ZkSync,
        )

        // Exolix's shielded Zcash route. Internal routing detail — the app always quotes ZEC.ZEC
        // and lets the server expand it into this shielded variant.
        private const val ZCASH_SHIELDED_ASSET = "ZEC.ZECSHIELDED"

        // Wrapped-SOL mint — the JUPITER server adapter reads it as native SOL.
        private const val WSOL_MINT = "So11111111111111111111111111111111111111112"

        // Inputs a THORChain-family vault can refund to, and the input count its observers accept.
        private val VAULT_DEPOSIT_UTXO_FILTERS = UtxoFilters(
            scriptTypes = listOf(ScriptType.P2PKH, ScriptType.P2WPKHSH, ScriptType.P2WPKH),
            maxOutputsCountForInputs = 10,
        )

        // The Stellar card's fan-out set, and the subset whose execution can deliver to an
        // address other than the trading account (see previewProviderId).
        private val STELLAR_ROUTE_PROVIDERS = setOf("SOROSWAP", "AQUARIUS", "STELLAR_DEX")
        private val STELLAR_RECIPIENT_CAPABLE_PROVIDERS = setOf("STELLAR_DEX")

        // LI.FI has no token list, so assets are encoded self-describingly as `<CHAIN>.<address>`
        // (see `deriveIdentifier`). This maps each supported EVM chain to the server's chain
        // code — the prefix the server's LI.FI resolver expects. Solana and Tron are handled
        // inline (`SOL.` / `TRON.` prefixes) since their address formats aren't the EVM
        // `0xeee…`/contract shape.
        private val lifiChainCodes = mapOf(
            BlockchainType.Ethereum to "ETH",
            BlockchainType.Polygon to "POL",
            BlockchainType.ArbitrumOne to "ARB",
            BlockchainType.Optimism to "OP",
            BlockchainType.Base to "BASE",
            BlockchainType.Avalanche to "AVAX",
            BlockchainType.BinanceSmartChain to "BSC",
            BlockchainType.RobinhoodChain to "ROBINHOOD",
            BlockchainType.Arc to "ARC",
        )
    }

    data class SwapQuoteExtraData(val route: UnstoppableAPI.Response.Route) : SwapQuote.ExtraData
}

// Chains where we consume the server-built tx from `execution` (so we send sourceAddress
// on /v3/preview). Everything else builds its own transfer to the deposit address.
private val Token.needsServerBuiltTx: Boolean
    get() = blockchainType.isEvm || blockchainType in setOf(
        BlockchainType.Tron,
        BlockchainType.Ton,
        BlockchainType.Solana,
    )

// Whether a provider's route for [token] is a server-built tx we sign (so /v3/preview gets the
// source address). Per chain by default; the Stellar DEX card is server-built on a chain that
// otherwise builds its own deposit transfers.
private fun UProvider.consumesServerBuiltTx(token: Token): Boolean = when (this) {
    UProvider.Stellar -> true
    else -> token.needsServerBuiltTx
}
