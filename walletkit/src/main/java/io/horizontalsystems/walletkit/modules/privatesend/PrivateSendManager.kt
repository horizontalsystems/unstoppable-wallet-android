package io.horizontalsystems.walletkit.modules.privatesend

import android.util.Log
import com.google.gson.Gson
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.MultiSwapProviderRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapHelper
import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.math.BigDecimal

/**
 * The private send stack: which tokens can be sent privately, and committing an order.
 *
 * A private send routes a transfer through a confidential USwap provider so the recipient
 * cannot be linked on-chain to the sender. Execution is a plain transfer to a deposit
 * address, so the multiswap send-transaction machinery does the actual work.
 *
 * Which providers are confidential comes from GET /providers (privacy.confidential), not a
 * hardcoded id — the server, not a release, decides availability. Only providers that also
 * exist in [MultiSwapProviderRegistry.confidentialProviders] are usable: a brand-new server
 * id still needs an app update to be resolvable for tracking and history rendering.
 */
class PrivateSendManager(
    appConfigProvider: IAppConfigProvider,
    private val localStorage: ILocalStorage,
) {
    private val unstoppableAPI = APIClient.build(
        appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to appConfigProvider.uswapApiKey),
    ).create(UnstoppableAPI::class.java)

    private val gson = Gson()
    private val syncMutex = Mutex()

    // A confidential provider is only usable here if it executes as a plain transfer:
    // anything else (signed_transaction, thorchain_deposit, …) needs real transaction
    // building and must not silently join the private send flow.
    private val supportedExecutionType = "transfer"

    @Volatile
    private var confidentialServerIds: List<String> = restoredServerIds()

    // Guarded by syncMutex. Tracks per-provider token-list starts so a send screen opening
    // repeatedly doesn't re-run the provider's cache reads each time.
    private val providerStartedAt = mutableMapOf<String, Long>()

    // Bumped whenever availability data changes (provider list or a token list sync), so an
    // open send screen reveals or hides the toggle without a reload.
    private val _availabilityFlow = MutableStateFlow(0L)
    val availabilityFlow = _availabilityFlow.asStateFlow()

    /**
     * Pure in-memory lookup over already-synced state. Called from the render path: it must
     * never trigger a fetch, block, or become suspend.
     */
    fun isSupported(token: Token): Boolean = candidates().any { it.supportsPrivateSend(token) }

    /**
     * Refreshes the confidential provider list (TTL-guarded) and each candidate's token list.
     * Cheap to call on every send screen open: everything runs on IO — provider.start() does
     * blocking Room and marketKit reads — and a successfully started provider is not
     * re-started for a whole interval.
     */
    suspend fun sync() = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            syncProviderIds()

            for (provider in candidates()) {
                val startedAt = providerStartedAt[provider.serverProviderId]
                if (startedAt != null && System.currentTimeMillis() - startedAt in 0 until SYNC_INTERVAL) {
                    continue
                }

                try {
                    provider.start()
                    providerStartedAt[provider.serverProviderId] = System.currentTimeMillis()
                    _availabilityFlow.value++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to sync tokens of ${provider.serverProviderId}", e)
                }
            }
        }
    }

    /**
     * Quote and commit in one call: /v2/rate exists here only to *choose* a provider, so with
     * a single candidate the round trip is pure latency on a screen where the user is already
     * waiting — the fan-out returns the moment a second confidential provider maps the token.
     * Every economic value below prefers the committed (/v2/swap) response.
     *
     * Throws [PrivateSendError] only — raw server text never leaves this method.
     */
    suspend fun commit(request: PrivateSendRequest): PrivateSendOrder {
        val token = request.token
        val providers = candidates().filter { it.supportsPrivateSend(token) }

        if (providers.isEmpty()) throw PrivateSendError.TokenUnsupported

        val provider = if (providers.size > 1) {
            selectCheapestProvider(providers, token, request.amountOut) ?: providers.first()
        } else {
            providers.first()
        }

        // Omitting the refund address forfeits the buffer refund, which lands on the success
        // path too — so a missing one fails the commit rather than proceeding without.
        val refundAddress = try {
            SwapHelper.getReceiveAddressForToken(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw PrivateSendError.CommitFailed(e)
        }

        val route = try {
            provider.privateSendCommit(
                token = token,
                amountOut = request.amountOut,
                destinationAddress = request.recipient,
                refundAddress = refundAddress,
                slippage = IMultiSwapProvider.DEFAULT_SLIPPAGE,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw mapCommitError(e)
        }

        val uuid = route.uuid
        if (uuid.isNullOrEmpty()) throw PrivateSendError.CommitFailed()

        val execution = route.execution
        if (execution == null || execution.method != supportedExecutionType) {
            throw PrivateSendError.CommitFailed()
        }

        // `execution.chain` is sometimes a chain id ("56") and sometimes a name ("bsc"), so
        // only a recognised chain that resolves to a *different* blockchain is treated as a
        // mismatch. An unrecognised string is not evidence of disagreement.
        val executionBlockchainType = execution.chain?.let { USwapProvider.chainIdBlockchainTypes[it] }
        if (executionBlockchainType != null && executionBlockchainType != token.blockchainType) {
            throw PrivateSendError.CommitFailed()
        }

        val depositAddress = execution.depositAddress ?: throw PrivateSendError.CommitFailed()
        val depositAmount = execution.amount?.toBigDecimalOrNull() ?: throw PrivateSendError.CommitFailed()

        val minSellAmount = route.minSellAmount

        // A deposit below the floor is refunded whole and no swap happens.
        if (minSellAmount != null && depositAmount < minSellAmount) {
            throw PrivateSendError.CommitFailed()
        }

        // Never falls back to the entered amount: that is the *output* the user asked for,
        // not a value the provider has agreed to deliver.
        val amountOut = route.expectedBuyAmount
        if (amountOut == null || amountOut <= BigDecimal.ZERO) {
            throw PrivateSendError.CommitFailed()
        }

        // Attachment deliverability is decided here, not left to the deposit build: the order
        // is already committed either way, and an undeliverable attachment must surface once
        // as an authored commit error rather than fail on every re-entry into the build.
        PrivateSendDepositBuilder.deliverableMemo(execution.attachment, token.blockchainType)

        return PrivateSendOrder(
            request = request,
            depositAmount = depositAmount,
            minSellAmount = minSellAmount,
            amountOut = amountOut,
            providerId = provider.id,
            depositAddress = depositAddress,
            attachment = execution.attachment,
            providerSwapId = uuid,
            refundAddress = refundAddress,
            estimatedTime = route.estimatedTime?.total,
            committedAt = System.currentTimeMillis(),
        )
    }

    private fun candidates(): List<USwapProvider> {
        val serverIds = confidentialServerIds
        return MultiSwapProviderRegistry.confidentialProviders.filter { it.serverProviderId in serverIds }
    }

    private fun restoredServerIds(): List<String> {
        // The fallback seed only covers first launch / offline. A successful sync always
        // replaces it, including removing a provider the server stops marking confidential.
        val json = localStorage.privateSendProviderIds ?: return FALLBACK_SERVER_IDS

        return try {
            gson.fromJson(json, Array<String>::class.java)?.toList() ?: FALLBACK_SERVER_IDS
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restore confidential provider ids", e)
            FALLBACK_SERVER_IDS
        }
    }

    private suspend fun syncProviderIds() {
        val stored = localStorage.privateSendProviderIds
        val age = System.currentTimeMillis() - localStorage.privateSendProviderIdsSyncTime
        if (stored != null && age in 0 until SYNC_INTERVAL) return

        try {
            val providerIds = unstoppableAPI.providers()
                .filter {
                    it.isConfidential &&
                            !it.suspended &&
                            it.executionType == supportedExecutionType
                }
                .map { it.provider }

            confidentialServerIds = providerIds

            if (providerIds.isNotEmpty()) {
                localStorage.privateSendProviderIds = gson.toJson(providerIds)
                localStorage.privateSendProviderIdsSyncTime = System.currentTimeMillis()
            } else {
                // An empty filtered list is honoured for this session but neither persisted
                // nor timestamped: it can also mean a server that doesn't send executionType
                // or the privacy marker yet, and locking "[]" in for the whole sync interval
                // (and across restarts) would keep the feature dark long after the server
                // starts sending them. Clearing storage keeps the next sync retrying.
                localStorage.privateSendProviderIds = null
                localStorage.privateSendProviderIdsSyncTime = 0
            }

            _availabilityFlow.value++
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to sync confidential providers", e)
        }
    }

    // Every route delivers the identical requested output, so the cheapest deposit wins.
    private suspend fun selectCheapestProvider(
        providers: List<USwapProvider>,
        token: Token,
        amountOut: BigDecimal,
    ): USwapProvider? = coroutineScope {
        val rated = providers.map { provider ->
            async {
                try {
                    val rate = provider.privateSendRate(token, amountOut, IMultiSwapProvider.DEFAULT_SLIPPAGE)
                    val sellAmount = rate.routes.mapNotNull { it.sellAmount }.minOrNull()
                    Triple(provider, sellAmount, rate.providerErrors.orEmpty())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Triple(provider, null, emptyList())
                }
            }
        }.map { it.await() }

        val best = rated.filter { it.second != null }.minByOrNull { it.second!! }
        if (best != null) return@coroutineScope best.first

        // No routes anywhere: surface an authored reason if any provider named one.
        reason(rated.flatMap { it.third })?.let { throw it }

        null
    }

    private fun mapCommitError(error: Throwable): PrivateSendError {
        if (error is HttpException) {
            if (error.code() == 503) return PrivateSendError.ProviderSuspended

            // A failed /v2/swap answers with the provider error as its body, carrying the same
            // minimumAmount / maximumAmount / errorCode fields /v2/rate reports in
            // `providerErrors`. Only the parsed fields are read — the body itself is never
            // rendered, because it is raw server text.
            parseProviderError(error)?.let { providerError ->
                reason(listOf(providerError))?.let { return it }
            }

            if (error.code() == 404) return PrivateSendError.NoRoute

            return PrivateSendError.NetworkError(error)
        }

        if (error is java.io.IOException) return PrivateSendError.NetworkError(error)

        return PrivateSendError.CommitFailed(error)
    }

    private fun parseProviderError(error: HttpException): UnstoppableAPI.Response.ProviderError? {
        val body = try {
            error.response()?.errorBody()?.string()
        } catch (e: Throwable) {
            null
        } ?: return null

        val parsed = try {
            gson.fromJson(body, UnstoppableAPI.Response.ProviderError::class.java)
        } catch (e: Throwable) {
            null
        } ?: return null

        // A body with none of the provider-error markers is some other failure wearing the
        // same envelope (an auth rejection, a proxy page). Returning null lets the caller
        // fall back on the status code instead of inventing a route-level reason from it.
        if (parsed.errorCode == null && parsed.minimumAmount == null && parsed.maximumAmount == null) {
            return null
        }

        return parsed
    }

    // The one mapping from provider errors to authored reasons, shared by the /v2/rate
    // providerErrors array and the single error body of a failed /v2/swap. Returns null when
    // nothing is recognisable, so each caller decides its own fallback.
    private fun reason(providerErrors: List<UnstoppableAPI.Response.ProviderError>): PrivateSendError? {
        val outOfRange = providerErrors.filter { it.errorCode == "amountOutOfRange" }

        outOfRange.mapNotNull { it.minimumAmount }.minOrNull()?.let {
            return PrivateSendError.BelowMinimum(it)
        }

        outOfRange.mapNotNull { it.maximumAmount }.maxOrNull()?.let {
            return PrivateSendError.AboveMaximum(it)
        }

        val exactOutputDeclined = providerErrors.any {
            it.errorCode == "routeNotFound" &&
                    it.resolvedMessage?.lowercase()?.contains("exact output") == true
        }
        if (exactOutputDeclined) return PrivateSendError.ExactOutputUnsupported

        // An amountOutOfRange that carries no figures is still a route-level refusal, and "no
        // private route for this token and amount" is honest about it. Anything else is not
        // ours to interpret.
        val routeLevelCodes = setOf("amountOutOfRange", "routeNotFound")
        val recognised = providerErrors.any { it.errorCode in routeLevelCodes }

        return if (recognised) PrivateSendError.NoRoute else null
    }

    companion object {
        private const val TAG = "PrivateSendManager"
        private const val SYNC_INTERVAL = 6 * 60 * 60 * 1000L // 6 hours
        private val FALLBACK_SERVER_IDS = listOf("NEAR_CONFIDENTIAL")
    }
}
