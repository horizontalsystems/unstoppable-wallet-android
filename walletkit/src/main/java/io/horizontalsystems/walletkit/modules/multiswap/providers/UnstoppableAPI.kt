package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.modules.multiswap.SwapCommitFailure
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SwapNetworkFee
import io.horizontalsystems.walletkit.core.isEvm
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.math.BigDecimal

interface UnstoppableAPI {
    @GET("providers")
    suspend fun providers(): List<Response.Provider>

    @GET("tokens")
    suspend fun tokens(
        @Query("provider") provider: String
    ): Response.Tokens

    // /v3/rate — read-only, prices the swap across the requested providers. Returns
    // { routes: [...] } with economics only — no execution, no uuid.
    @POST("rate")
    suspend fun rate(
        @Body request: Request.Rate,
    ): Response.Rate

    // /v3/preview — builds the committed route shape for ONE provider without creating anything,
    // so it can be re-run on every refresh, fee edit or slippage change. Returns the route with a
    // `previewToken` and no `uuid`. For transfer providers the route carries no `execution` yet
    // (the deposit address only exists once the order does), only an optional `stubDepositAddress`.
    @POST("preview")
    suspend fun preview(
        @Body request: Request.Preview,
    ): Response.Route

    // /v3/commit — the only call with side effects: creates the order for a previewed route and
    // returns its tracking `uuid`, plus the `execution` (deposit address) for transfer providers.
    // Idempotent on the token: resending it replays the first answer. A non-2xx carries a `status`
    // discriminator (see [CommitException]).
    @POST("commit")
    suspend fun commit(
        @Body request: Request.Commit,
    ): Response.Commit

    // /v3/signed — reports the inbound tx hash of a committed swap whose transfer the app built
    // itself. The same hash again is a no-op; a different one replaces it while nothing is
    // observed yet.
    @POST("signed")
    suspend fun signed(
        @Body request: Request.Signed,
    ): Response.Signed

    // /v3/track — our recorded swaps, tracked by the route's uuid ALONE (the server resolves the
    // provider and every swap detail from the record; any other field in the body is a 400).
    // Before the deposit is observed the answer is the record's own state (`not_started` or
    // `signed`) with empty legs.
    @POST("track")
    suspend fun track(
        @Body request: Request.Track,
    ): Response.Track

    // /v3/split/rate — prices one deposit fanned out to several outputs, each a share of the
    // deposit. Only split-capable providers answer; naming any other is `invalidParams`. A
    // refusal is a non-2xx whose body carries the same { providerErrors } shape.
    @POST("split/rate")
    suspend fun splitRate(
        @Body request: Request.SplitRate,
    ): Response.SplitRate

    // /v3/split/preview — the split route for ONE provider with a destination per output, plus
    // the `previewToken` and the stand-in deposit address. Never carries an execution: the
    // deposit address only exists once the order does.
    @POST("split/preview")
    suspend fun splitPreview(
        @Body request: Request.SplitPreview,
    ): Response.SplitRoute

    // /v3/split/commit — creates the split order for a previewed split route. Answers the
    // `splitUuid`, one `uuid` per output and the single transfer that funds every output. A
    // simple preview token here, or a split token on /v3/commit, is a 400.
    @POST("split/commit")
    suspend fun splitCommit(
        @Body request: Request.Commit,
    ): Response.SplitCommit

    // /v3/split/signed — reports the deposit hash to every output of the split at once.
    @POST("split/signed")
    suspend fun splitSigned(
        @Body request: Request.SplitSigned,
    ): Response.SplitSigned

    // /v3/split/track — the split as one swap in the /v3/track shape: one deposit leg, then a
    // payout leg per output tagged with `meta.outputIndex`.
    @POST("split/track")
    suspend fun splitTrack(
        @Body request: Request.SplitTrack,
    ): Response.Track

    // /v3/track/evm — stateless on-chain reader for native EVM swaps (1inch/Uniswap/Pancake)
    // that were not created through /v3/commit, so there is no record to look up by uuid.
    @POST("track/evm")
    suspend fun trackEvm(
        @Body request: Request.Track,
    ): Response.Track

    // /v3/track/thorchain — stateless on-chain reader for native THORChain/Mayachain swaps.
    @POST("track/thorchain")
    suspend fun trackThorchain(
        @Body request: Request.Track,
    ): Response.Track

    // /v3/check-addresses — the AML precheck of a route that carries `amlProviderId`; the value is
    // passed through as the route gave it.
    @GET("check-addresses")
    suspend fun checkAddresses(
        @Query("addresses") addresses: String,
        @Query("amlProviderId") amlProviderId: String,
    ): Response.CheckAddresses

    // /v3/tokens/all — every catalog token with the providers that list it. Several hundred KB
    // that only change when the server restarts, so the stored ETag turns a repeat into a 304.
    @GET("tokens/all")
    suspend fun allTokens(
        @Header("If-None-Match") etag: String?,
    ): retrofit2.Response<List<Response.CatalogToken>>

    object Request {
        // /v3/rate request — compare routes across the providers that can serve the pair.
        // Exactly one of sellAmount / buyAmount must be set — neither or both is a 400.
        // buyAmount asks the route to price backwards from an exact output.
        data class Rate(
            val sellAsset: String,
            val buyAsset: String,
            val sellAmount: String? = null,
            val slippage: BigDecimal,
            // Null leaves the fan-out to the server: it calls every provider that can serve the
            // pair, and the ones that cannot appear neither in `routes` nor in `providerErrors`.
            val providers: Set<String>? = null,
            // The privacy levels to include (`none`, `basic`, ...). Absent means every level.
            val privacy: List<String>? = null,
            val chainId: String? = null,
            val buyAmount: String? = null,
        )

        // /v3/preview request — the route to show on the confirmation screen, for the single
        // provider. `sourceAddress` is the build signal: supply it and the server returns a
        // ready-to-sign tx; omit it and we build the tx ourselves (UTXO/Zcash/Monero/Zano/Stellar).
        // Same one-of contract for sellAmount / buyAmount as on Rate.
        data class Preview(
            val sellAsset: String,
            val buyAsset: String,
            val sellAmount: String? = null,
            val slippage: BigDecimal,
            val provider: String,
            val destinationAddress: String,
            val refundAddress: String? = null,
            val sourceAddress: String? = null,
            val chainId: String? = null,
            val buyAmount: String? = null,
            // The user's fee setting, so the returned transaction and the route's inbound fee line
            // are priced as the transfer will be sent. Omitted, the server prices the tx itself.
            val networkFee: NetworkFee? = null,
        )

        // One object discriminated by `kind`; it must match the sell asset's chain (a mismatch is
        // a 400), so [NetworkFee.of] only builds it for the chains the server names.
        data class NetworkFee(
            val kind: String,            // legacy | eip1559 | utxo
            val gasPrice: String? = null,            // legacy: wei
            val maxFeePerGas: String? = null,        // eip1559: wei
            val maxPriorityFeePerGas: String? = null,// eip1559: wei
            val feeRate: Int? = null,                // utxo: sat/vB
        ) {
            companion object {
                private val utxoFeeRateChains = setOf(
                    BlockchainType.Bitcoin,
                    BlockchainType.Litecoin,
                    BlockchainType.BitcoinCash,
                    BlockchainType.Dash,
                )

                fun of(tokenIn: Token, settings: SendTransactionSettings?): NetworkFee? =
                    when (val fee = settings?.networkFee) {
                        is SwapNetworkFee.Legacy -> if (tokenIn.blockchainType.isEvm) {
                            NetworkFee(kind = "legacy", gasPrice = fee.gasPrice.toString())
                        } else {
                            null
                        }

                        is SwapNetworkFee.Eip1559 -> if (tokenIn.blockchainType.isEvm) {
                            NetworkFee(
                                kind = "eip1559",
                                maxFeePerGas = fee.maxFeePerGas.toString(),
                                maxPriorityFeePerGas = fee.maxPriorityFeePerGas.toString(),
                            )
                        } else {
                            null
                        }

                        is SwapNetworkFee.Utxo -> if (tokenIn.blockchainType in utxoFeeRateChains) {
                            NetworkFee(kind = "utxo", feeRate = fee.feeRate)
                        } else {
                            null
                        }

                        null -> null
                    }
            }
        }

        // /v3/commit request. `inboundTxHash` only when the preview's transaction was signed
        // before committing; otherwise the hash is reported through /v3/signed.
        data class Commit(
            val previewToken: String,
            val inboundTxHash: String? = null,
        )

        data class Signed(
            val uuid: String,
            val inboundTxHash: String,
        )

        data class Track(
            // Recorded swaps (/v3/track): the route's uuid resolves provider + all details, and
            // must be the only field sent.
            val uuid: String? = null,
            // Stateless readers (/v3/track/evm, /v3/track/thorchain) carry full context.
            val inboundTxHash: String? = null,
            val provider: String? = null,
            val hash: String? = null,
            val chainId: String? = null,
            val fromAsset: String? = null,
            val fromAddress: String? = null,
            val fromAmount: String? = null,
            val toAsset: String? = null,
            val toAddress: String? = null,
            val toAmount: String? = null,
            val depositAddress: String? = null,
            // Debug-only: forces the stateless readers to return an action_required swap. Null in
            // release builds, and never sent to /v3/track, whose body is the uuid alone.
            val testActionRequired: Boolean? = null,
        )

        // One output of a split request: what it receives and its share of the deposit, as a
        // percent (the only form this app sends) that must sum to 100 over the outputs. The
        // destination is required on a preview and refused on a rate.
        data class SplitOutput(
            val buyAsset: String,
            val amount: String,
            val destinationAddress: String? = null,
        )

        // /v3/split/rate request — the rate body with the buy side pluralised. Exact input only.
        data class SplitRate(
            val sellAsset: String,
            val sellAmount: String,
            val amountType: String = SPLIT_AMOUNT_TYPE_PERCENT,
            val slippage: BigDecimal,
            val outputs: List<SplitOutput>,
            // Null leaves the fan-out to the server: every split-capable provider answers.
            val providers: Set<String>? = null,
            val chainId: String? = null,
        )

        // /v3/split/preview request — the split rate body for one provider, with a destination
        // per output. `refundAddress` is required here; `sourceAddress` is only recorded as the
        // sender, since a split is always a transfer the app builds itself.
        data class SplitPreview(
            val sellAsset: String,
            val sellAmount: String,
            val amountType: String = SPLIT_AMOUNT_TYPE_PERCENT,
            val slippage: BigDecimal,
            val provider: String,
            val refundAddress: String,
            val sourceAddress: String? = null,
            val chainId: String? = null,
            val networkFee: NetworkFee? = null,
            val outputs: List<SplitOutput>,
        )

        data class SplitSigned(
            val splitUuid: String,
            val inboundTxHash: String,
        )

        data class SplitTrack(
            val splitUuid: String,
            val inboundTxHash: String? = null,
        )

        const val SPLIT_AMOUNT_TYPE_PERCENT = "percent"
    }

    object Response {
        // The floor of a split as a whole: the outputs' floors summed, or null while any output
        // floats or there are no outputs.
        fun minBuyAmountTotal(mins: List<BigDecimal?>): BigDecimal? {
            if (mins.isEmpty() || mins.any { it == null }) return null
            return mins.fold(BigDecimal.ZERO) { sum, min -> sum + min!! }
        }

        data class Provider(
            val provider: String,
            val name: String? = null,
            val supportedChainIds: List<String> = emptyList(),
            val amlPolicy: String? = null,
            val amlPolicyDescription: String? = null,
            val contacts: Contacts? = null,
            // Whole-provider kill switch.
            val suspended: Boolean = false,
            // How this provider's routes execute: signed_transaction | transfer | …
            // Read by the private send stack, which only accepts plain-transfer providers.
            val executionType: String? = null,
            // Marks a provider routing through a confidential rail; such providers are never
            // offered on the ordinary swap screen. The live server sends a string level
            // ("none", "basic"); the originally documented shape was an object
            // ({"confidential": true}). Modeled as a JsonElement and interpreted in
            // [isConfidential] so either wire form works and neither can break the shared
            // /providers parse (this DTO also serves the suspensions sync).
            val privacy: JsonElement? = null,
            // Whether the provider serves the /v3/split endpoints. Split swaps are offered for
            // exactly the providers that say so; absent reads as false.
            val splitSwaps: Boolean = false,
            // Whether the server quotes this provider's single-output swaps. False for a
            // split-only provider, which must never be named on /v3/rate. Nullable rather than
            // defaulted: Gson skips the constructor, so an absent field would read as false.
            val quotes: Boolean? = null,
        ) {
            data class Contacts(
                val email: String? = null,
                val telegram: String? = null,
                val twitter: String? = null,
                val website: String? = null,
            )

            // The privacy level as a string: the wire form, or `basic` / `none` for the object form.
            val privacyLevel: String?
                get() {
                    val privacy = privacy ?: return null
                    return when {
                        privacy.isJsonPrimitive && privacy.asJsonPrimitive.isString -> privacy.asString.takeIf { it.isNotEmpty() }
                        privacy.isJsonObject -> if (isConfidential) "basic" else "none"
                        else -> null
                    }
                }

            // Any privacy level other than "none" marks the rail as confidential; the object
            // form reports it explicitly.
            val isConfidential: Boolean
                get() {
                    val privacy = privacy ?: return false
                    return when {
                        privacy.isJsonPrimitive ->
                            privacy.asJsonPrimitive.isString &&
                                    privacy.asString.isNotEmpty() &&
                                    privacy.asString != "none"

                        privacy.isJsonObject ->
                            privacy.asJsonObject.get("confidential")
                                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
                                ?.asBoolean == true

                        else -> false
                    }
                }
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
            // Asset code for chains whose assets are identified by code + issuer (Stellar:
            // `address` carries the issuer, `ticker` the code).
            val ticker: String? = null,
        )

        // One `/v3/tokens/all` row: the catalog identifier a request names the asset by, and the
        // providers with a token list that quote it (the DEXes and LI.FI keep none, so they are
        // never named). `address` is null on a native coin.
        data class CatalogToken(
            val identifier: String,
            val chain: String? = null,
            val chainId: String? = null,
            val address: String? = null,
            val ticker: String? = null,
            val providers: List<String>? = null,
        )

        // A route's Zcash address rules: for each address it needs, whether to send a unified
        // address (and which receivers it must carry) or a transparent one. The rule for every
        // side alike: `unified` present means send a unified address, otherwise transparent.
        data class ZcashAddressRules(
            val destination: Rule? = null,
            val refund: Rule? = null,
            val source: Rule? = null,
        ) {
            data class Rule(
                val unified: Unified? = null,
                val transparent: Boolean? = null,
                val tex: Boolean? = null,
                // `sourceAddress`, when sent, must equal `refundAddress`.
                val sameAsRefund: Boolean? = null,
            )

            data class Unified(
                // The receivers to build the unified address from.
                val receivers: List<String>? = null,
                // The address must carry at least one of these receivers.
                val requiresOneOf: List<String>? = null,
            )
        }

        // /v3/rate response — a list of routes to compare. Each route carries economics
        // only (no execution, no uuid — those appear after previewing and committing).
        // A provider that declined to route lands in providerErrors instead of routes.
        data class Rate(
            // Absent entirely (not just empty) when every provider refuses — the response
            // then carries only providerErrors. Nullable so Gson's missing-field null is an
            // honest type instead of a latent NPE.
            val routes: List<Route>? = null,
            val providerErrors: List<ProviderError>? = null,
        )

        // A per-provider refusal. A failed /v3/rate answers 200 with { routes, providerErrors };
        // a failed /v3/preview, and a `provider_error` /v3/commit, answer a non-2xx with this same
        // field set as the body itself ({ error, provider, errorCode?, minimumAmount?,
        // maximumAmount? }). Tracking / diagnostics only — never shown in the UI verbatim.
        data class ProviderError(
            val provider: String? = null,
            val providerId: String? = null,
            // The documented key is "error" on both surfaces; "message" is what some
            // providers actually send, so both are accepted.
            val message: String? = null,
            val error: String? = null,
            val errorCode: String? = null,
            val minimumAmount: BigDecimal? = null,
            val maximumAmount: BigDecimal? = null,
        ) {
            val resolvedProviderId: String?
                get() = provider ?: providerId

            val resolvedMessage: String?
                get() = message ?: error
        }

        // A single route. From /rate it is economics-only; from /preview it additionally carries
        // a `previewToken` and, except for transfer providers, the `execution` block; the `uuid`
        // tracking handle arrives from /commit.
        data class Route(
            val sellAsset: String?,
            val buyAsset: String?,
            val expectedBuyAmount: BigDecimal?,
            // The ENFORCED floor the route can deliver (an explicit `null` when the amount is only
            // an estimate — floating-rate P2P, re-priced at deposit). null ⇒ no guarantee: the
            // confirm page must not render a "Guaranteed" row.
            val minBuyAmount: BigDecimal?,
            val estimatedTime: EstimatedTime?,
            // EVM ERC20 spender to approve before swapping (1inch/Barter/Circle). On a rate
            // route it is top-level; on a previewed route it rides execution.approval.spender.
            val approvalSpender: String?,
            // How to send funds. Present on a preview for signed_transaction / thorchain_deposit
            // providers; for transfer providers it arrives only with the commit.
            val execution: Execution?,
            // Tracking handle (swap_records.uuid). Absent on a preview, filled in by [merging].
            val uuid: String?,
            // The provider(s) that produced this route. On a /v3/rate fan-out each route
            // carries exactly one — how a multi-provider request (the Stellar card's
            // waterfall) knows which provider to preview with.
            val providers: List<String>? = null,
            // In exact-output mode this is the answer: the amount to deposit, carrying the
            // slippage buffer over meta.near.minSellAmount.
            val sellAmount: BigDecimal? = null,
            val meta: Meta? = null,
            // Opaque handle of a previewed route, passed to /v3/commit. Valid for about ten
            // minutes, less when `expiresAt` comes first. Null on a rate route and once consumed.
            val previewToken: String? = null,
            // Transfer providers only, on a preview without `execution`: a ready-to-receive account
            // on the sell chain to build the fee estimate against. Never a destination to send to.
            val stubDepositAddress: String? = null,
            // Millisecond epoch after which the route (and its preview token) is no longer honoured.
            val expiresAt: Long? = null,
            // The route's own AML policy (`excellent` | `good` | `fair`); the Route Score of this quote.
            val amlPolicy: String? = null,
            // Present when the route has an address precheck: the value `/v3/check-addresses` takes.
            val amlProviderId: String? = null,
            // On a route with ZEC on either side: which Zcash address each side takes.
            val addressRules: ZcashAddressRules? = null,
        ) {
            // should be getter, otherwise it will be null when restored from json
            val expectedBuyAmountOrZero: BigDecimal
                get() = expectedBuyAmount ?: BigDecimal.ZERO

            val providerId: String?
                get() = providers?.firstOrNull()

            // The ERC20 spender used to compute allowance, wherever it lives on the route.
            val approvalSpenderOrExecution: String?
                get() = approvalSpender ?: execution?.approvalSpender

            // The previewed route completed by its commit: the order's uuid, and for transfer
            // providers the execution (deposit address) and the committed amounts, which may differ
            // from the preview. The token and the stub are dropped: the token is consumed and the
            // stub must never be mistaken for the deposit address once the execution carries it.
            fun merging(commit: Commit): Route = copy(
                expectedBuyAmount = commit.expectedBuyAmount ?: expectedBuyAmount,
                minBuyAmount = commit.minBuyAmount ?: minBuyAmount,
                execution = commit.execution ?: execution,
                uuid = commit.uuid,
                sellAmount = commit.sellAmount ?: sellAmount,
                previewToken = null,
                stubDepositAddress = null,
                expiresAt = commit.expiresAt ?: expiresAt,
            )

            data class EstimatedTime(
                val total: Long
            )

            // Exact-output metadata. minSellAmount is the floor below which the deposit is
            // refunded whole and no swap happens.
            data class Meta(
                val near: Near? = null,
            ) {
                data class Near(
                    val minSellAmount: BigDecimal? = null,
                    val exactOutput: Boolean? = null,
                )
            }

            val minSellAmount: BigDecimal?
                get() = meta?.near?.minSellAmount
        }

        // /v3/commit 200 body. Transfer providers: the committed order is a fresh quote, so the
        // amounts may differ from the preview, and `execution` carries the deposit address that
        // did not exist at preview. Signed / thorchain routes: `execution` is absent — the
        // preview's transaction is the one to sign.
        data class Commit(
            val status: String,
            val uuid: String?,
            val sellAmount: BigDecimal? = null,
            val expectedBuyAmount: BigDecimal? = null,
            val minBuyAmount: BigDecimal? = null,
            val expiresAt: Long? = null,
            // true when this token was committed before (a retry replayed the first answer)
            val replayed: Boolean = false,
            val execution: Execution? = null,
        )

        data class Signed(
            val uuid: String,
            val status: String,
            val hash: String? = null,
        )

        // /v3/split/rate response. As on /v3/rate, `routes` is absent when every provider refuses.
        data class SplitRate(
            val routes: List<SplitRoute>? = null,
            val providerErrors: List<ProviderError>? = null,
        )

        // A split route: the deposit side at the top level, the buy side per output. From
        // /split/rate it is economics only; from /split/preview it carries the token and the
        // stand-in deposit address, never an execution.
        data class SplitRoute(
            val providers: List<String>? = null,
            val sellAsset: String? = null,
            val sellAmount: BigDecimal? = null,
            val amountType: String? = null,
            val outputs: List<SplitRouteOutput>? = null,
            val estimatedTime: Route.EstimatedTime? = null,
            val amlPolicy: String? = null,
            val previewToken: String? = null,
            val stubDepositAddress: String? = null,
            val expiresAt: Long? = null,
        ) {
            val providerId: String?
                get() = providers?.firstOrNull()

            // Outputs in request order. Matched by `index`, never by asset or address: several
            // outputs may share both.
            val outputsByIndex: List<SplitRouteOutput>
                get() = outputs.orEmpty().sortedBy { it.index }

            // What the deposit buys in total; every output is priced in the same buy asset here.
            val expectedBuyAmountTotal: BigDecimal
                get() = outputsByIndex.fold(BigDecimal.ZERO) { sum, output -> sum + (output.expectedBuyAmount ?: BigDecimal.ZERO) }

            // The enforced floor of the whole split; null while any output floats, since a
            // total that guarantees only part of the payout would guarantee nothing.
            val minBuyAmountTotal: BigDecimal?
                get() = minBuyAmountTotal(outputsByIndex.map { it.minBuyAmount })
        }

        data class SplitRouteOutput(
            val index: Int,
            val buyAsset: String? = null,
            val destinationAddress: String? = null,
            // This output's share of the deposit, in the sell asset, resolved from the percent.
            val sellAmount: BigDecimal? = null,
            val expectedBuyAmount: BigDecimal? = null,
            // The floor the route enforces for this output; null for a floating rate.
            val minBuyAmount: BigDecimal? = null,
            val estimatedTime: Route.EstimatedTime? = null,
        )

        // /v3/split/commit 200 body: the order's handles and the one transfer funding every output.
        data class SplitCommit(
            val status: String,
            val splitUuid: String? = null,
            val sellAmount: BigDecimal? = null,
            val expiresAt: Long? = null,
            val replayed: Boolean = false,
            val execution: Execution? = null,
            val outputs: List<SplitCommitOutput>? = null,
        ) {
            val outputsByIndex: List<SplitCommitOutput>
                get() = outputs.orEmpty().sortedBy { it.index }

            val minBuyAmountTotal: BigDecimal?
                get() = minBuyAmountTotal(outputsByIndex.map { it.minBuyAmount })
        }

        data class SplitCommitOutput(
            val index: Int,
            // The output's own swap record; /v3/track and /v3/signed accept it like any swap's.
            val uuid: String? = null,
            val buyAsset: String? = null,
            val destinationAddress: String? = null,
            val sellAmount: BigDecimal? = null,
            val expectedBuyAmount: BigDecimal? = null,
            val minBuyAmount: BigDecimal? = null,
        )

        data class SplitSigned(
            val splitUuid: String,
            val status: String,
            val hash: String? = null,
        )

        // `execution` discriminated union — switch on `method`. Modeled as one flat
        // class (Gson-friendly) with accessors that read only the fields the method uses.
        data class Execution(
            val method: String,        // signed_transaction | transfer | thorchain_deposit
            val chain: String?,
            // signed_transaction
            val transactions: List<SignableTx>?,
            val approval: Approval?,
            // transfer
            val depositAddress: String?,
            val amount: String?,
            val asset: String?,
            val attachment: Attachment?,
            val unsignedTx: SignableTx?,
            // thorchain_deposit
            val protocol: String?,
            val inboundAddress: String?,
            val memo: String?,
            val delivery: Delivery?,
        ) {
            // The single tx a client signs, if any: signed_transaction's first, or the
            // optional unsignedTx on transfer / thorchain delivery (sent only when we
            // supplied a sourceAddress).
            val primarySignable: SignableTx?
                get() = when (method) {
                    "signed_transaction" -> transactions?.firstOrNull()
                    "transfer" -> unsignedTx
                    ThorChainDepositRoute.METHOD -> delivery?.unsignedTx
                    else -> null
                }

            // The deposit address for the address-transfer methods. signed_transaction is
            // tx-only and has none.
            fun resolvedDepositAddress(): String? = when (method) {
                "transfer" -> depositAddress
                // Absent (or empty) for a settlement-native sell: a MsgDeposit has no vault.
                ThorChainDepositRoute.METHOD -> inboundAddress?.takeIf { it.isNotBlank() }
                else -> null
            }

            // The binding memo for an address transfer — the order identifier the provider
            // uses to credit the deposit, which we must echo back or the funds are lost.
            // Every chain we build a transfer for here (Stellar/Zcash/Monero/Zano/UTXO) puts
            // it in the memo field, whether the server typed it `text` (RUNE/GAIA/TON/NEAR)
            // or `destination_tag` (a numeric tag, e.g. a Stellar memo-id), so accept both.
            // XRP is the one chain where a tag is NOT a memo — it reads the same attachment
            // through [resolvedDestinationTag] instead, and never through this.
            fun resolvedMemo(): String? = when (method) {
                ThorChainDepositRoute.METHOD -> memo
                "transfer" -> attachment?.value
                else -> null
            }

            /**
             * The binding identifier for an XRP transfer, as the Payment's DestinationTag.
             *
             * Only a numeric `destination_tag` fits that field: a `text` attachment would have
             * to ride an XRPL memo, which providers do not read, so it fails the route instead
             * of being silently dropped — an unmatched deposit is typically unrecoverable.
             * No attachment at all is a route the provider credits by deposit address alone.
             */
            fun resolvedDestinationTag(): Long? {
                if (method != "transfer") return null
                val attachment = attachment ?: return null

                if (attachment.type != "destination_tag") {
                    throw IllegalStateException("XRP cannot deliver a `${attachment.type}` attachment")
                }

                return XrpDestinationTag.parse(attachment.value)
                    ?: throw IllegalStateException("Destination tag out of range")
            }

            val approvalSpender: String?
                get() = when (method) {
                    "signed_transaction" -> approval?.spender
                    ThorChainDepositRoute.METHOD -> delivery?.approval?.spender
                    else -> null
                }
        }

        // A signable transaction the server built. `kind` tags the shape; each per-chain
        // builder reads the matching field (evm: to/value/data/gas; solana: message;
        // stellar: xdr; utxo: psbt; tron/ton/cosmos/ripple/near: tx).
        data class SignableTx(
            val kind: String,
            // evm
            val to: String?,
            val from: String?,
            val value: String?,
            val data: String?,
            val gas: String?,
            val gasPrice: String?,
            // base64 forms
            val psbt: String?,
            val message: String?,
            val xdr: String?,
            // object forms (cosmos / ripple / ton / tron / near)
            val tx: JsonElement?,
        )

        data class Approval(
            val token: String?,
            val spender: String,
            val amount: String?,
        )

        // transfer.attachment — an order identifier the provider uses to credit the deposit.
        data class Attachment(
            val type: String,   // destination_tag | text
            val value: String,
        )

        // thorchain_deposit.delivery — chain-specific memo binding.
        data class Delivery(
            val kind: String,   // evm_contract_call | utxo_op_return | cosmos_memo
            val router: String?,
            val approval: Approval?,
            val shieldedMemoAddress: String?,
            val unsignedTx: SignableTx?,
        )

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
            val status: String, // not_started, signed, pending, swapping, completed, refunded, unknown, failed, expired, action_required
            val type: String?,
            val hash: String?,
            val chainId: String?,
            val fromAsset: String?,
            val fromAmount: String?,
            val fromAddress: String?,
            val toAsset: String?,
            val toAmount: String?,
            val toAddress: String?,
            // Empty before the deposit is observed.
            val legs: List<Leg>?,
            val meta: Meta? = null,
            // /v3/split/track only: stands in for `uuid`.
            val splitUuid: String? = null,
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
                val meta: LegMeta? = null,
            ) {
                // A split's payout (or refund) leg for the output at `outputIndex`; the deposit
                // leg carries no index.
                val outputIndex: Int?
                    get() = meta?.outputIndex

                val isRefund: Boolean
                    get() = meta?.refund == true
            }

            // Split legs: which output a payout belongs to, and whether it is a refund.
            data class LegMeta(
                val outputIndex: Int? = null,
                val refund: Boolean? = null,
            )

            data class Meta(
                val provider: String?,
                val pauseReason: String?, // "overdue_with_funds" | "aml" | "frozen"
            )
        }
    }

    /**
     * A `/v3/preview` refused for a Zcash address the route does not take: `400 { message, field,
     * addressRules }`. [addressRules] are the rules the server checked against, so the caller can
     * preview again with the addresses they point to.
     */
    class PreviewAddressException(
        override val message: String,
        val field: String,
        val addressRules: Response.ZcashAddressRules?,
        cause: Throwable,
    ) : Exception(message, cause) {
        companion object {
            fun from(error: Throwable): PreviewAddressException? {
                if (error !is HttpException || error.code() != 400) return null
                val json = errorJson(error) ?: return null
                val field = json.get("field")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
                val message = json.get("message")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
                val rules = json.get("addressRules")?.takeIf { it.isJsonObject }?.let {
                    try {
                        Gson().fromJson(it, Response.ZcashAddressRules::class.java)
                    } catch (e: Throwable) {
                        null
                    }
                }
                return PreviewAddressException(message, field, rules, error)
            }
        }
    }

    /**
     * A `/v3/preview` or `/v3/commit` refused because a source, destination or refund address is
     * blacklisted (`403`, `errorCode: addressBlacklisted`). [message] is the server's text for the user.
     */
    class AddressBlacklistedException(override val message: String, cause: Throwable) : Exception(message, cause) {
        companion object {
            const val ERROR_CODE = "addressBlacklisted"

            fun from(error: Throwable): AddressBlacklistedException? {
                if (error !is HttpException || error.code() != 403) return null
                val json = errorJson(error) ?: return null
                if (json.get("errorCode")?.takeIf { it.isJsonPrimitive }?.asString != ERROR_CODE) return null
                val message = listOf("message", "error")
                    .firstNotNullOfOrNull { key -> json.get(key)?.takeIf { it.isJsonPrimitive }?.asString }
                    .orEmpty()
                return AddressBlacklistedException(message, error)
            }
        }
    }

    /**
     * A failed /v3/commit. [status] is the server's discriminator when the body carried one:
     * `in_progress` (the same token is being committed right now), `refresh_required` (token
     * expired or invalid, provider suspended), `rate_changed` (the order drifted against the user
     * beyond the request's slippage; the record is failed) or `provider_error` (the provider
     * refused; [providerError] carries the same fields a /v3/rate providerError does). Null for a
     * transport failure, a 2xx that was not `ok`, or an unrecognisable body.
     */
    class CommitException(
        val httpStatus: Int?,
        val status: String?,
        val reason: String?,
        val providerError: Response.ProviderError?,
        cause: Throwable? = null,
    ) : Exception(reason ?: status ?: cause?.message, cause) {
        companion object {
            private val gson = Gson()

            /**
             * Turns whatever [UnstoppableAPI.commit] threw into a [CommitException]: the non-2xx
             * body is `{ status, reason | error | message, provider?, errorCode?, ... }`, and an
             * unusable body falls back to the HTTP status alone. A [CommitException] passes through.
             */
            fun from(error: Throwable): CommitException {
                if (error is CommitException) return error

                if (error !is HttpException) {
                    return CommitException(null, null, null, null, error)
                }

                val json = errorJson(error)

                val status = json?.get("status")?.takeIf { it.isJsonPrimitive }?.asString
                val reason = listOf("reason", "error", "message")
                    .firstNotNullOfOrNull { key -> json?.get(key)?.takeIf { it.isJsonPrimitive }?.asString }

                // `provider_error` carries the provider refusal as the body itself.
                val providerError = if (status == "provider_error" && json != null) {
                    try {
                        gson.fromJson(json, Response.ProviderError::class.java)
                    } catch (e: Throwable) {
                        null
                    }
                } else {
                    null
                }

                return CommitException(error.code(), status, reason, providerError, error)
            }
        }
    }
}

/** The commit outcome as the confirmation screens present it, from the server's discriminator. */
fun UnstoppableAPI.CommitException.toSwapCommitFailure(): SwapCommitFailure = when (status) {
    "rate_changed" -> SwapCommitFailure.RateChanged
    "refresh_required" -> SwapCommitFailure.RefreshRequired
    else -> SwapCommitFailure.Failed
}

/** The JSON object of a non-2xx body, or null when the body is missing or not an object. */
private fun errorJson(error: HttpException): com.google.gson.JsonObject? {
    val body = try {
        error.response()?.errorBody()?.string()
    } catch (e: Throwable) {
        null
    }
    return try {
        body?.let { JsonParser.parseString(it) }?.takeIf { it.isJsonObject }?.asJsonObject
    } catch (e: Throwable) {
        null
    }
}
