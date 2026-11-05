package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.derivation
import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.core.nativeTokenQueries

/**
 * How a server asset and a wallet token name each other: a catalog row resolves to the token
 * queries it stands for, and a token the catalog lacks is named by its address, chain-prefixed,
 * which is how the DEXes and LI.FI (which keep no token list) take an asset.
 */
object USwapAssetMapping {
    /** The server's chain identifiers, as `/v3/tokens/all` and the chain hint of a request spell them. */
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
    )

    private val chainIdByBlockchainType = chainIdBlockchainTypes.entries.associate { (id, type) -> type to id }

    // The chain code that prefixes a chain-addressed EVM asset (`ETH.0x…`). Chains absent here
    // have no addressable form, so a token of theirs the catalog lacks cannot be quoted.
    private val evmChainCodes = mapOf(
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

    // The EVM native sentinel, and the wrapped-SOL mint the server reads as native SOL.
    private const val EVM_NATIVE_SENTINEL = "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
    const val WSOL_MINT = "So11111111111111111111111111111111111111112"

    /** The chain hint a request carries beside a chain-addressed asset; the numeric id of an EVM chain. */
    fun chainId(blockchainType: BlockchainType): String? =
        if (blockchainType.isEvm) chainIdByBlockchainType[blockchainType] else null

    /**
     * The key a catalog entry is stored and looked up under. An EVM contract address is spelled
     * two ways, checksummed by the server and lowercase by the coin database, so it is keyed in
     * one case; every other token query is keyed by its id as is.
     */
    fun catalogKey(query: TokenQuery): String {
        val type = query.tokenType
        if (!query.blockchainType.isEvm || type !is TokenType.Eip20) return query.id
        return TokenQuery(query.blockchainType, TokenType.Eip20(type.address.lowercase())).id
    }

    /**
     * The wallet tokens a catalog row names. A UTXO coin resolves to every derivation the wallet
     * may hold it under; a row on a chain the app does not know resolves to nothing.
     */
    fun tokenQueries(token: UnstoppableAPI.Response.CatalogToken): List<TokenQuery> {
        val blockchainType = token.chainId?.let { chainIdBlockchainTypes[it] } ?: return emptyList()
        val address = token.address?.takeIf { it.isNotBlank() }

        return when (blockchainType) {
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
                -> listOf(TokenQuery(blockchainType, address?.let { TokenType.Eip20(it) } ?: TokenType.Native))

            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
            BlockchainType.Zcash,
            BlockchainType.Dash,
            BlockchainType.ECash,
                -> {
                var queries = blockchainType.nativeTokenQueries
                // The server's LTC is not a taproot coin.
                if (blockchainType == BlockchainType.Litecoin) {
                    queries = queries.filterNot { it.tokenType.derivation == TokenType.Derivation.Bip86 }
                }
                queries
            }

            BlockchainType.Solana -> listOf(TokenQuery(blockchainType, address?.let { TokenType.Spl(it) } ?: TokenType.Native))

            BlockchainType.Stellar -> {
                // Classic asset: the server sends the issuer in `address` and the asset code in `ticker`.
                val tokenType = if (address != null) {
                    token.ticker?.takeIf { it.isNotBlank() }?.let { TokenType.Asset(it, address) }
                } else {
                    TokenType.Native
                }
                listOfNotNull(tokenType?.let { TokenQuery(blockchainType, it) })
            }

            BlockchainType.Ton -> listOf(TokenQuery(blockchainType, address?.let { TokenType.Jetton(it) } ?: TokenType.Native))

            BlockchainType.Monero -> listOf(TokenQuery(blockchainType, TokenType.Native))

            BlockchainType.Thorchain,
            BlockchainType.Mayachain,
                -> {
                // No address on these chains: the identifier is the asset notation itself
                // (THOR.RUNE, THOR.RUJI, BTC-BTC, MAYA.CACAO), which the chain's plugin resolves
                // to the bank denom the token is keyed on.
                listOfNotNull(
                    ChainRegistry[blockchainType]?.swapAssetTokenType(token.identifier)?.let { TokenQuery(blockchainType, it) }
                )
            }

            BlockchainType.Zano -> listOf(TokenQuery(blockchainType, address?.let { TokenType.ZanoAsset(it) } ?: TokenType.Native))

            // Native XRP only: the server lists no XRPL issued token.
            BlockchainType.Xrp -> if (address == null) listOf(TokenQuery(blockchainType, TokenType.Native)) else emptyList()

            // `address` is the NEP-141 contract; native NEAR has none.
            BlockchainType.Near -> listOf(TokenQuery(blockchainType, address?.let { TokenType.Nep141(it) } ?: TokenType.Native))

            else -> emptyList()
        }
    }

    /**
     * The chain-prefixed address a token the catalog lacks is named by, or null when the token
     * has no such form: `<CHAIN>.<contract>` (EVM, native as the `0xeee…` sentinel), `SOL.<mint>`
     * (wSOL meaning native SOL), `TRON.TRX` / `TRON.<contract>`, `XLM.XLM` / `XLM.<CODE>-<ISSUER>`.
     * Base58 and Stellar codes are case-sensitive and travel verbatim.
     */
    fun derivedIdentifier(token: Token): String? = when (token.blockchainType) {
        BlockchainType.Solana -> when (val type = token.type) {
            TokenType.Native -> "SOL.$WSOL_MINT"
            // The wSOL TOKEN is not swappable: the server reads its mint as native SOL, so
            // SOL -> wSOL degenerates to "same asset" and X -> wSOL would deliver native SOL
            // while the user watches the wSOL token balance.
            is TokenType.Spl -> type.address.takeIf { it != WSOL_MINT }?.let { "SOL.$it" }
            else -> null
        }

        // Tron is TVM (base58 addresses, not `0x`); TRC20 contracts ride the `Eip20` type.
        BlockchainType.Tron -> when (val type = token.type) {
            TokenType.Native -> "TRON.TRX"
            is TokenType.Eip20 -> "TRON.${type.address}"
            else -> null
        }

        // The issuer is a `G…` account; a Soroban contract token (`C…`) is not a classic asset.
        BlockchainType.Stellar -> when (val type = token.type) {
            TokenType.Native -> "XLM.XLM"
            is TokenType.Asset -> type.issuer.takeIf { it.startsWith("G") }?.let { "XLM.${type.code}-$it" }
            else -> null
        }

        else -> evmChainCodes[token.blockchainType]?.let { code ->
            when (val type = token.type) {
                is TokenType.Eip20 -> "$code.${type.address}"
                TokenType.Native -> "$code.$EVM_NATIVE_SENTINEL"
                else -> null
            }
        }
    }
}
