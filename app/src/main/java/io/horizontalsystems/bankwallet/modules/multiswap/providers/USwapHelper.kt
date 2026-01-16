package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.derivation
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.nativeTokenQueries
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal

class USwapHelper {

    private val api = APIClient.build(
        App.appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to App.appConfigProvider.uswapApiKey)
    ).create(UnstoppableAPI::class.java)

    suspend fun getAssetsMap(providerId: String): Map<Token, String> {
        val assetsMap = mutableMapOf<Token, String>()
        val tokens = api.tokens(providerId).tokens

        for (token in tokens) {
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
                    val tokenType = if (!token.address.isNullOrBlank()) {
                        null
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

                is BlockchainType.Unsupported -> Unit
            }
        }

        return assetsMap
    }

    suspend fun quote(
        assetIn: String,
        assetOut: String,
        amountIn: BigDecimal,
        slippage: BigDecimal,
        destinationAddress: String,
        sourceAddress: String?,
        refundAddress: String,
        providerId: String,
        dry: Boolean
    ): UnstoppableAPI.Response.Quote = api.quote(
        UnstoppableAPI.Request.Quote(
            sellAsset = assetIn,
            buyAsset = assetOut,
            sellAmount = amountIn.toPlainString(),
            providers = setOf(providerId),
            slippage = slippage.toInt(),
            destinationAddress = destinationAddress,
            sourceAddress = sourceAddress,
            refundAddress = refundAddress,
            dry = dry
        )
    )

    companion object {
        private val blockchainTypes = mapOf(
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
            "ton" to BlockchainType.Ton,
            "dash" to BlockchainType.Dash,
            "ecash" to BlockchainType.ECash,
            "monero" to BlockchainType.Monero,
            "100" to BlockchainType.Gnosis,
        )
    }
}
