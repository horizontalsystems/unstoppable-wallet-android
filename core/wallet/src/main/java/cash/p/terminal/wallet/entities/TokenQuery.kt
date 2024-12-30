package cash.p.terminal.wallet.entities

import io.horizontalsystems.core.entities.BlockchainType
import java.util.Objects

data class TokenQuery(
    val blockchainType: BlockchainType,
    val tokenType: TokenType
) {

    val id: String
        get() = listOf(blockchainType.uid, tokenType.id).joinToString("|")

    override fun equals(other: Any?): Boolean =
        other is TokenQuery && other.blockchainType == blockchainType && other.tokenType == tokenType

    override fun hashCode(): Int =
        Objects.hash(blockchainType, tokenType)

    companion object {

        fun fromId(id: String): TokenQuery? {
            val chunks = id.split("|")
            if (chunks.size != 2) return null

            val tokenType = TokenType.fromId(chunks[1]) ?: return null

            return TokenQuery(
                BlockchainType.fromUid(chunks[0]),
                tokenType
            )
        }
    }
}
