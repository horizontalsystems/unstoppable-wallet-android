package io.horizontalsystems.walletkit.modules.addtoken

import com.google.gson.JsonObject
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.network.Network
import io.horizontalsystems.nearkit.network.RpcProvider
import io.horizontalsystems.walletkit.core.customCoinUid

/**
 * Adds a NEP-141 token by its contract account id (`usdt.tether-token.near`) or a NearBlocks
 * token URL. Name, symbol and decimals come from the contract's `ft_metadata`.
 */
class AddNearTokenBlockchainService(private val blockchain: Blockchain) : AddTokenModule.IAddTokenBlockchainService {

    private val rpcProvider by lazy { RpcProvider.create(Network.MainNet.rpcUrls) }

    override fun isValid(reference: String) = parse(reference) != null

    override fun tokenQuery(reference: String): TokenQuery {
        val contractId = parse(reference) ?: throw IllegalArgumentException("Invalid NEAR token contract")
        return TokenQuery(BlockchainType.Near, TokenType.Nep141(contractId))
    }

    override suspend fun token(reference: String): Token {
        val tokenQuery = tokenQuery(reference)
        val contractId = (tokenQuery.tokenType as TokenType.Nep141).contractId
        val metadata = rpcProvider.callFunctionJson(contractId, "ft_metadata") as? JsonObject
            ?: throw IllegalArgumentException("$contractId is not a NEP-141 token")
        val symbol = metadata.get("symbol")?.takeIf { it.isJsonPrimitive }?.asString?.take(MAX_TEXT)
            ?: throw IllegalArgumentException("$contractId has no token symbol")
        val decimals = metadata.get("decimals")?.takeIf { it.isJsonPrimitive }?.asInt?.takeIf { it in 0..MAX_DECIMALS }
            ?: throw IllegalArgumentException("$contractId has invalid decimals")
        val name = metadata.get("name")?.takeIf { it.isJsonPrimitive }?.asString?.take(MAX_TEXT)?.takeIf { it.isNotBlank() } ?: symbol

        return Token(
            coin = Coin(
                uid = tokenQuery.customCoinUid,
                name = name,
                code = symbol,
                image = null,
            ),
            blockchain = blockchain,
            type = tokenQuery.tokenType,
            decimals = decimals,
        )
    }

    private fun parse(input: String): String? {
        var text = input.trim()
        if (text.startsWith("http")) {
            text = text.substringAfter("/token/").substringBefore('?').trimEnd('/')
        }
        text = text.lowercase()
        return text.takeIf { NearKit.isValidAccountId(it) }
    }

    companion object {
        private const val MAX_TEXT = 64
        private const val MAX_DECIMALS = 64
    }
}
