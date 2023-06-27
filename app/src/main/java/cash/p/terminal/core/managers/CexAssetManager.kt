package cash.p.terminal.core.managers

import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexAssetRaw
import cash.p.terminal.core.providers.CexNetwork
import cash.p.terminal.core.providers.CexNetworkRaw
import cash.p.terminal.entities.Account

class CexAssetManager(marketKit: MarketKitWrapper) {

    private val coins = marketKit.allCoins().map { it.uid to it }.toMap()
    private val allBlockchains = marketKit.allBlockchains().map { it.uid to it }.toMap()
    private val storage = mutableMapOf<String, List<CexAssetRaw>>()

    private fun buildCexAsset(cexAssetRaw: CexAssetRaw): CexAsset {
        return CexAsset(
            id = cexAssetRaw.id,
            name = cexAssetRaw.name,
            freeBalance = cexAssetRaw.freeBalance,
            lockedBalance = cexAssetRaw.lockedBalance,
            depositEnabled = cexAssetRaw.depositEnabled,
            withdrawEnabled = cexAssetRaw.withdrawEnabled,
            networks = cexAssetRaw.networks.map { buildCexNetwork(it) },
            coin = coins[cexAssetRaw.coinUid],
            decimals = cexAssetRaw.decimals
        )
    }

    private fun buildCexNetwork(cexNetworkRaw: CexNetworkRaw): CexNetwork {
        return CexNetwork(
            network = cexNetworkRaw.network,
            name = cexNetworkRaw.name,
            isDefault = cexNetworkRaw.isDefault,
            depositEnabled = cexNetworkRaw.depositEnabled,
            withdrawEnabled = cexNetworkRaw.withdrawEnabled,
            blockchain = allBlockchains[cexNetworkRaw.blockchainUid],
        )
    }

    fun saveAll(cexAssetRaws: List<CexAssetRaw>, account: Account) {
        storage.set(account.id, cexAssetRaws)
    }

    fun get(account: Account, assetId: String): CexAsset? {
        return storage[account.id]
            ?.find { it.id == assetId }
            ?.let { buildCexAsset(it) }
    }

    fun getAll(account: Account): List<CexAsset> {
        return storage[account.id]?.map {
            buildCexAsset(it)
        } ?: listOf()
    }

}