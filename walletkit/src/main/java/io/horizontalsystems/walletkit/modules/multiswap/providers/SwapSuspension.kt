package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import java.time.Instant

/**
 * A scoped restriction published by uswap-server on `GET /providers`.
 *
 * Two kinds of provider need this and for different reasons:
 *  - Providers uswap-server quotes: it already refuses a suspended request, so honouring the rule
 *    here is purely so the user never sees a card that is going to fail.
 *  - Providers the APP quotes itself (Uniswap, PancakeSwap, AllBridge, and 1inch/THORChain/Maya,
 *    which have native implementations here): no server call happens at all, so this filter is the
 *    ONLY enforcement that exists. Dropping it would make those providers unsuspendable.
 *
 * Every field is nullable with a default so the class round-trips through Gson in both directions
 * — it is parsed straight off the wire AND out of the local cache, where an older or truncated
 * payload must degrade to "matches nothing" rather than throw.
 */
data class SwapSuspension(
    val kind: Kind? = null,
    val side: Side? = null,
    /**
     * The CANONICAL asset ID (`ETH.0XA0B86991…`, `BTC.BTC`, `ETH-ETH`) — an ordinary asset
     * identifier, the same vocabulary a quote request uses, but exactly ONE spelling per asset.
     *
     * The server normalizes here because an asset has several valid identifiers and which one we
     * send depends on the sub-provider — this app spells USDC `ETH.USDC-0XA0B8…` via the asset map,
     * `ETH.0xa0b8…` for LI.FI and a bare `0xa0b8…` for Barter. Comparing our request string against
     * whichever spelling an operator typed would catch one and silently miss the rest, so both
     * sides normalize first ([CanonicalAssetId.of]) and compare the result.
     */
    val asset: String? = null,
    val chain: String? = null,
    val sellAsset: String? = null,
    val buyAsset: String? = null,
    val expiresAt: String? = null,
) {
    enum class Kind {
        @SerializedName("asset")
        Asset,

        @SerializedName("chain")
        Chain,

        @SerializedName("pair")
        Pair,
    }

    /**
     * Which side of the swap the rule covers. Provider deposit and payout capability break
     * independently, so "either side" is a default, not the only option.
     */
    enum class Side {
        @SerializedName("any")
        Any,

        @SerializedName("sell")
        Sell,

        @SerializedName("buy")
        Buy,
    }

    fun matches(sell: String?, buy: String?, now: Instant): Boolean = isLive(now) && when (kind) {
        Kind.Asset -> matchesSided(sell, buy) { it == asset }
        Kind.Chain -> matchesSided(sell, buy) { CanonicalAssetId.chainOf(it) == chain }
        // Directed — the reverse direction is a separate rule.
        Kind.Pair -> sell != null && buy != null && sell == sellAsset && buy == buyAsset
        // A kind this build does not know is inert rather than treated as one of the kinds it
        // does: a rule we cannot evaluate must not disable a provider on a guess.
        null -> false
    }

    /**
     * The server already filters expired rules out, but a cached list outlives its own contents —
     * a rule that expires during the hour we hold the response must stop applying on its own.
     *
     * An unparseable timestamp reads as no expiry, keeping the rule in force: a malformed date is
     * not a reason to start serving a pair the operator switched off.
     */
    private fun isLive(now: Instant): Boolean {
        val raw = expiresAt ?: return true
        val expiry = runCatching { Instant.parse(raw) }.getOrNull() ?: return true
        return expiry.isAfter(now)
    }

    private inline fun matchesSided(sell: String?, buy: String?, test: (String) -> Boolean): Boolean {
        // An absent side is `any`, which is also what an unrecognized one degrades to — the
        // widest reading of a rule the server did mean to publish.
        if (side != Side.Buy && sell != null && test(sell)) return true
        if (side != Side.Sell && buy != null && test(buy)) return true
        return false
    }
}

/**
 * Builds uswap-server's canonical asset ID for a local [Token].
 *
 * This is the client half of a two-implementation contract (the other is the server's
 * `canonicalAsset`). It is deliberately the SMALLER half: the server has to parse every identifier
 * spelling a client might send, whereas here the token is already structured — a blockchain and a
 * token type — so this only has to FORMAT, never parse. Keep it that way; if this ever starts
 * string-splitting identifiers, the two will drift.
 */
object CanonicalAssetId {
    /**
     * Chain codes as uswap-server spells them, which is what the ID is built from. A blockchain
     * absent here yields no ID, so no rule can match it — the safe direction: a provider stays
     * quotable rather than being silently suspended by a mapping gap.
     */
    private val chainCodes = mapOf(
        BlockchainType.Bitcoin to "BTC",
        BlockchainType.BitcoinCash to "BCH",
        BlockchainType.ECash to "XEC",
        BlockchainType.Litecoin to "LTC",
        BlockchainType.Dash to "DASH",
        BlockchainType.Zcash to "ZEC",
        BlockchainType.Monero to "XMR",
        BlockchainType.Zano to "ZANO",
        BlockchainType.Ethereum to "ETH",
        BlockchainType.BinanceSmartChain to "BSC",
        BlockchainType.Polygon to "POL",
        BlockchainType.Avalanche to "AVAX",
        BlockchainType.Optimism to "OP",
        BlockchainType.ArbitrumOne to "ARB",
        BlockchainType.Gnosis to "GNO",
        BlockchainType.Base to "BASE",
        BlockchainType.Tron to "TRON",
        BlockchainType.Solana to "SOL",
        BlockchainType.Ton to "TON",
        BlockchainType.Stellar to "XLM",
        BlockchainType.Thorchain to "THOR",
    )

    /**
     * The gas asset's ticker per chain — NOT always the chain code (`BASE.ETH`, `BSC.BNB`,
     * `GNO.XDAI`), which is exactly why this is a table and not a derivation.
     */
    private val nativeTickers = mapOf(
        BlockchainType.Bitcoin to "BTC",
        BlockchainType.BitcoinCash to "BCH",
        BlockchainType.ECash to "XEC",
        BlockchainType.Litecoin to "LTC",
        BlockchainType.Dash to "DASH",
        BlockchainType.Zcash to "ZEC",
        BlockchainType.Monero to "XMR",
        BlockchainType.Zano to "ZANO",
        BlockchainType.Ethereum to "ETH",
        BlockchainType.BinanceSmartChain to "BNB",
        BlockchainType.Polygon to "POL",
        BlockchainType.Avalanche to "AVAX",
        BlockchainType.Optimism to "ETH",
        BlockchainType.ArbitrumOne to "ETH",
        BlockchainType.Gnosis to "XDAI",
        BlockchainType.Base to "ETH",
        BlockchainType.Tron to "TRX",
        BlockchainType.Solana to "SOL",
        BlockchainType.Ton to "TON",
        BlockchainType.Stellar to "XLM",
        BlockchainType.Thorchain to "RUNE",
    )

    private const val WSOL_MINT = "So11111111111111111111111111111111111111112"

    fun of(token: Token): String? {
        val chain = chainCodes[token.blockchainType] ?: return null

        return when (val type = token.type) {
            // Derivation / address type are wallet-side concerns; on the server they are all one
            // asset (`BTC.BTC`).
            TokenType.Native,
            is TokenType.Derived,
            is TokenType.AddressTyped,
                -> nativeTickers[token.blockchainType]?.let { "$chain.$it" }

            // Covers EVM hex and Tron base58 alike — both are upper-cased, matching the catalog
            // identifier's own convention (`ETH.USDC-0XA0B8…`).
            is TokenType.Eip20 -> "$chain.${type.address.uppercase()}"

            // Wrapped SOL is native SOL everywhere on the server.
            is TokenType.Spl -> if (type.address == WSOL_MINT) "$chain.SOL" else "$chain.${type.address.uppercase()}"

            is TokenType.Jetton -> "$chain.${type.address.uppercase()}"

            // The CODE stays verbatim — Stellar codes are case-sensitive (`yXLM` ≠ `YXLM`). The
            // issuer is StrKey, uppercase-only, so normalizing it is safe.
            is TokenType.Asset -> "$chain.${type.code}-${type.issuer.uppercase()}"

            // THORChain secured (`eth-eth` in x/bank). Deliberately NOT chain-prefixed: the
            // canonical id is the identifier a quote itself uses, and [chainOf] treats a dot-less
            // id as THORChain — which is where the asset actually lives.
            is TokenType.ThorchainAsset -> type.denom.uppercase()

            is TokenType.ZanoAsset,
            is TokenType.Unsupported,
                -> null
        }
    }

    /**
     * The chain a canonical ID belongs to — what a chain-scoped rule matches on. A dot-less id can
     * only be a THORChain secured asset (`ETH-ETH`), which lives in THORChain's x/bank — so a
     * `THOR` chain rule covers it and an `ETH` one correctly does not.
     */
    fun chainOf(id: String): String = id.substringBefore('.', "THOR")
}

/**
 * The provider-id → rules index, plus the one question the swap screen asks.
 *
 * Both properties are keyed by the server's spelling of a provider id, so any lookup goes through
 * [serverId] first.
 */
data class SwapSuspensionIndex(
    /** Whole-provider kill switch: no pair at all, so it never reaches the per-pair rules. */
    val suspendedProviders: Set<String> = setOf(),
    val rulesByProvider: Map<String, List<SwapSuspension>> = mapOf(),
) {
    /**
     * May this provider be offered for this pair?
     *
     * A token we cannot normalize (an unmapped blockchain) yields `null` and therefore never
     * matches — deliberately failing OPEN. The alternative, treating "unknown" as suspended, would
     * let one missing chain-code entry silently disable providers with no visible cause.
     */
    fun isSuspended(providerId: String, tokenIn: Token, tokenOut: Token): Boolean {
        val key = serverId(providerId)

        if (key in suspendedProviders) return true

        val rules = rulesByProvider[key]
        if (rules.isNullOrEmpty()) return false

        val sell = CanonicalAssetId.of(tokenIn)
        val buy = CanonicalAssetId.of(tokenOut)
        val now = Instant.now()

        return rules.any { it.matches(sell, buy, now) }
    }

    companion object {
        /**
         * The server names a provider bare and upper-case (`NEAR`); the app prefixes the ids it
         * routes through uswap-server (`u_NEAR`) and lower-cases the ones it implements natively
         * (`oneinch`).
         */
        fun serverId(providerId: String) = providerId.removePrefix("u_").uppercase()

        fun from(responses: List<UnstoppableAPI.Response.Provider>) = SwapSuspensionIndex(
            suspendedProviders = responses.filter { it.suspended }.map { serverId(it.provider) }.toSet(),
            rulesByProvider = responses
                .filter { !it.suspended && !it.suspensions.isNullOrEmpty() }
                .associate { serverId(it.provider) to it.suspensions.orEmpty() },
        )
    }
}
