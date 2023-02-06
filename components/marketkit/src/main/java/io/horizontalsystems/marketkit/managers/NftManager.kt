package io.horizontalsystems.marketkit.managers

import io.horizontalsystems.marketkit.models.*
import io.horizontalsystems.marketkit.providers.HsNftProvider
import io.horizontalsystems.marketkit.providers.TopCollectionRaw
import java.math.BigDecimal

class NftManager(
    private val coinManager: CoinManager,
    private val provider: HsNftProvider
) {

    suspend fun topCollections(): List<NftTopCollection> = topCollections(provider.topCollections())

    fun topCollections(responses: List<TopCollectionRaw>): List<NftTopCollection> {
        val blockchainUids = responses.map { it.blockchainUid }
        val blockchainTypes = blockchainUids.map { BlockchainType.fromUid(it) }
        val baseTokenMap = baseTokenMap(blockchainTypes)

        return responses.map { collection(it, baseTokenMap) }
    }

    private fun nftPrice(token: Token?, value: BigDecimal?): NftPrice? {
        if (token != null && value != null) {
            return NftPrice(token, value)
        }
        return null
    }

    private fun baseTokenMap(blockchainTypes: List<BlockchainType>): Map<BlockchainType, Token> {
        val map = mutableMapOf<BlockchainType, Token>()
        val tokens = coinManager.tokens(blockchainTypes.map { TokenQuery(it, TokenType.Native) })
        tokens.forEach { token ->
            map[token.blockchainType] = token
        }

        return map
    }

    private fun collection(
        response: TopCollectionRaw,
        baseTokenMap: Map<BlockchainType, Token>
    ): NftTopCollection {
        val blockchainType = BlockchainType.fromUid(response.blockchainUid)
        val baseToken = baseTokenMap[blockchainType]

        val volumes: Map<HsTimePeriod, NftPrice?> = mapOf(
            HsTimePeriod.Day1 to nftPrice(baseToken, response.volume1d),
            HsTimePeriod.Week1 to nftPrice(baseToken, response.volume7d),
            HsTimePeriod.Month1 to nftPrice(baseToken, response.volume30d)
        )

        val changes: Map<HsTimePeriod, BigDecimal?> = mapOf(
            HsTimePeriod.Day1 to response.change1d,
            HsTimePeriod.Week1 to response.change7d,
            HsTimePeriod.Month1 to response.change30d
        )

        return NftTopCollection(
            blockchainType = blockchainType,
            providerUid = response.providerUid,
            name = response.name,
            thumbnailImageUrl = response.thumbnailImageUrl,
            floorPrice = nftPrice(baseToken, response.floorPrice),
            volumes = volumes.filterNotNullValues(),
            changes = changes.filterNotNullValues()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
        filterValues { it != null } as Map<K, V>

}
