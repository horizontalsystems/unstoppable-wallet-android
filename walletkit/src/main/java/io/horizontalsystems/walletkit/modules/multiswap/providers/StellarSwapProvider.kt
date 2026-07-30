package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.adapters.StellarAssetAdapter
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import io.horizontalsystems.walletkit.modules.multiswap.action.ActionActivateStellarAsset
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldRecipient
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldSlippage
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.room.StellarAsset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/// Stellar-native swaps through uswap-server's Stellar providers, with the grant's
/// StellarBroker-first waterfall: /v2/rate fans out to the requested set, the SB route is
/// preferred whenever present, the best-priced fallback (SOROSWAP / AQUARIUS / STELLAR_DEX)
/// serves otherwise. One provider card in the UI, one route out.
///
/// Execution: signed_transaction (kind `stellar`) — the server built the XDR (Soroswap
/// /quote/build, Aquarius swap_chained assembly, Horizon path payment); we sign + submit
/// via StellarKit (SendTransactionData.Stellar.WithTransactionEnvelope). Tracking is
/// server-recorded: the committed route's uuid rides providerSwapId, and the "u_" id
/// prefix routes SwapTrackRequestBuilder to POST /v2/track { uuid, inboundTxHash }.
class StellarSwapProvider : IMultiSwapProvider {
    // "u_" prefix is load-bearing: SwapTrackRequestBuilder tracks any u_* provider's record
    // by uuid alone. The server id (STELLARBROKER) is what /v2/rate and /v2/swap consume.
    override val id = "u_$SERVER_ID"
    override val title = "StellarBroker"
    override val type = SwapProviderType.DEX
    override val requireTerms = true
    override val riskLevel = RiskLevel.EXCELLENT

    override fun isSingleTransactionSwap(tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String) = true

    private val unstoppableAPI = APIClient.build(
        App.appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to App.appConfigProvider.uswapApiKey)
    ).create(UnstoppableAPI::class.java)

    override fun supports(blockchainType: BlockchainType) = blockchainType == BlockchainType.Stellar

    override fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
        return asset(tokenFrom) != null && asset(tokenTo) != null
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
    ): SwapQuote {
        val (route, providerId) = bestRoute(tokenIn, tokenOut, amountIn, IMultiSwapProvider.DEFAULT_SLIPPAGE, allProviders)

        return SwapQuote(
            amountOut = route.expectedBuyAmountOrZero,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = activationAction(tokenOut),
            estimationTime = route.estimatedTime?.total,
            extraData = StellarSwapQuoteExtraData(providerId)
        )
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: io.horizontalsystems.walletkit.entities.Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote {
        // sourceAddress is the build signal: the server returns the ready-to-sign XDR only
        // when it knows the trading account.
        val source = SwapHelper.getSendingAddressForToken(tokenIn)
            ?: throw IllegalStateException("No Stellar sending address")
        val destination = recipient?.hex ?: source

        var selectedProvider = (swapQuote.extraData as? StellarSwapQuoteExtraData)?.providerId

        // SB and Aquarius settle on the trader's own account — a third-party recipient can
        // only be served by the providers whose execution supports a distinct destination.
        // Re-run the waterfall over the recipient-capable set and take its best route.
        if (destination != source && selectedProvider != null && selectedProvider !in recipientCapableProviders) {
            selectedProvider = bestRoute(tokenIn, tokenOut, amountIn, slippage, recipientCapableProviders).second
        }

        val provider = selectedProvider ?: throw IllegalStateException("No Stellar route selected")

        val route = unstoppableAPI.swap(
            UnstoppableAPI.Request.Swap(
                sellAsset = asset(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn"),
                buyAsset = asset(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut"),
                sellAmount = amountIn.toPlainString(),
                slippage = slippage,
                provider = provider,
                destinationAddress = destination,
                sourceAddress = source,
                chainId = CHAIN_ID,
            )
        )

        // A committed /v2/swap must carry the tracking handle — it is what /v2/track receives
        // as `uuid`, and the server resolves the record from it alone. Fail before any funds
        // move rather than create an untrackable swap.
        if (route.uuid.isNullOrEmpty()) {
            throw IllegalStateException("Swap is not trackable (no uuid)")
        }

        val amountOut = route.expectedBuyAmountOrZero

        // The server's minBuyAmount is the enforced on-chain floor — shown directly as the
        // "Guaranteed" amount, matching USwapProvider. null (StellarBroker) means nothing
        // client-verifiable guarantees the amount — hide the "Guaranteed" and slippage rows.
        val amountOutMin = route.minBuyAmount
        val effectiveSlippage = if (route.minBuyAmount != null) slippage else null

        val fields = buildList {
            recipient?.let { add(DataFieldRecipient(it)) }
            DataFieldSlippage.getField(effectiveSlippage)?.let { add(it) }
        }

        return SwapFinalQuote(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = amountOut,
            amountOutMin = amountOutMin,
            sendTransactionData = sendTransactionData(route),
            priceImpact = null,
            fields = fields,
            estimatedTime = route.estimatedTime?.total,
            slippage = effectiveSlippage,
            providerSwapId = route.uuid,
            fromAsset = asset(tokenIn),
            toAsset = asset(tokenOut),
            depositAddress = null,
        )
    }

    private fun sendTransactionData(route: UnstoppableAPI.Response.Route): SendTransactionData {
        val execution = route.execution ?: throw IllegalStateException("No execution found")

        return when (execution.method) {
            "signed_transaction" -> {
                val xdr = execution.transactions
                    ?.firstOrNull { it.kind == "stellar" }
                    ?.xdr
                    ?: throw IllegalStateException("No stellar tx found")

                SendTransactionData.Stellar.WithTransactionEnvelope(xdr)
            }

            // SB executes over its own WebSocket session (the broker builds and submits,
            // the client signs each streamed tx) — reachable only with stellarBrokerEnabled,
            // and the session client is not ported to Android yet. Fails at confirmation,
            // before any funds move.
            "stellar_broker" -> throw IllegalStateException("StellarBroker session execution is not supported yet")

            else -> throw IllegalStateException("Unsupported execution method: ${execution.method}")
        }
    }

    /// /v2/rate over the given provider set, applying the waterfall: STELLARBROKER when it
    /// quoted (the grant's primary), otherwise the best-priced fallback.
    private suspend fun bestRoute(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        providers: List<String>,
    ): Pair<UnstoppableAPI.Response.Route, String> {
        val routes = unstoppableAPI.rate(
            UnstoppableAPI.Request.Rate(
                sellAsset = asset(tokenIn) ?: throw IllegalStateException("No identifier for tokenIn"),
                buyAsset = asset(tokenOut) ?: throw IllegalStateException("No identifier for tokenOut"),
                sellAmount = amountIn.toPlainString(),
                slippage = slippage,
                providers = providers.toSet(),
                chainId = CHAIN_ID,
            )
        ).routes

        // Only reachable while stellarBrokerEnabled — SB is otherwise never requested, so
        // the server never returns it. Kept live for the flag flip (see stellarBrokerEnabled).
        routes.firstOrNull { it.providerId == SERVER_ID }?.let {
            return it to SERVER_ID
        }
        


        val fallback = routes.maxByOrNull { it.expectedBuyAmountOrZero }
        val providerId = fallback?.providerId
        if (fallback == null || providerId == null) {
            throw IllegalStateException("No Stellar routes")
        }

        return fallback to providerId
    }

    /// The trustline pre-swap step (the EIP-20 approve analogue): a classic-asset buy needs
    /// the account's trustline BEFORE the swap. The trustline belongs to the ACCOUNT, not
    /// the wallet list — when the buy token was never added as a wallet (no adapter), it is
    /// resolved through the account-level kit, so the user gets the Activate button instead
    /// of a raw server 4xx at confirmation. An unresolvable state does NOT block (null):
    /// the server preflight remains the authority; this only drives the inline button UX.
    private suspend fun activationAction(tokenOut: Token): ISwapProviderAction? {
        val tokenType = tokenOut.type as? TokenType.Asset ?: return null // native XLM needs no trustline
        val account = App.accountManager.activeAccount ?: return null

        val established = try {
            App.adapterManager.getAdapterForToken<StellarAssetAdapter>(tokenOut)?.isTrustlineEstablished()
                ?: withContext(Dispatchers.IO) {
                    App.stellarKitManager.getStellarKitWrapper(account).stellarKit
                        .isAssetEnabled(StellarAsset.Asset(tokenType.code, tokenType.issuer))
                }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            return null
        }
        if (established) return null

        return ActionActivateStellarAsset(Wallet(tokenOut, account))
    }

    /// uswap-server's self-describing Stellar identifiers (parseStellarAssetIdentifier):
    /// native → `XLM.XLM`, classic asset → `XLM.CODE-GISSUER…`.
    private fun asset(token: Token): String? {
        if (token.blockchainType != BlockchainType.Stellar) return null

        return when (val type = token.type) {
            TokenType.Native -> "XLM.XLM"
            is TokenType.Asset -> "XLM.${type.code}-${type.issuer}"
            else -> null
        }
    }

    data class StellarSwapQuoteExtraData(val providerId: String) : SwapQuote.ExtraData

    companion object {
        // Registered under the server's STELLARBROKER provider id; the fallback ids stay
        // hidden behind this single card.
        private const val SERVER_ID = "STELLARBROKER"
        private const val CHAIN_ID = "stellar"

        // StellarBroker participation switch for THIS app — the ONE place SB is turned on or
        // off. false = SB is never requested from /v2/rate nor picked, and the waterfall runs
        // over the fallbacks only.
        //
        // Why false (2026-07-24 decision, mirrored from iOS): SB has no service-fee mechanism,
        // and we don't ship a fee-less provider in the app. SB stays live on the SERVER for
        // the web SDK. Flipping this to true also requires porting the SB WebSocket session
        // client (see sendTransactionData's stellar_broker arm).
        private const val stellarBrokerEnabled = false

        // The full waterfall set requested from /v2/rate.
        private val allProviders = if (stellarBrokerEnabled) {
            listOf(SERVER_ID, "SOROSWAP", "AQUARIUS", "STELLAR_DEX")
        } else {
            listOf("SOROSWAP", "AQUARIUS", "STELLAR_DEX")
        }

        // SB and Aquarius settle on the trader's own account — a third-party recipient can
        // only be served by the providers whose execution supports a distinct destination.
        private val recipientCapableProviders = listOf("SOROSWAP", "STELLAR_DEX")
    }
}
