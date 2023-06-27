package cash.p.terminal.core.managers

import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexAssetRaw
import cash.p.terminal.core.providers.CexNetwork
import cash.p.terminal.core.providers.CexNetworkRaw
import cash.p.terminal.core.storage.CexAssetsDao
import cash.p.terminal.entities.Account

class CexAssetManager(marketKit: MarketKitWrapper, private val cexAssetsDao: CexAssetsDao) {

    private val coins = marketKit.allCoins().map { it.uid to it }.toMap()
    private val allBlockchains = marketKit.allBlockchains().map { it.uid to it }.toMap()

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

    fun saveAllForAccount(cexAssetRaws: List<CexAssetRaw>, account: Account) {
        cexAssetsDao.delete(account.id)
        cexAssetsDao.insert(cexAssetRaws)
    }

    fun get(account: Account, assetId: String): CexAsset? {
        return cexAssetsDao.get(account.id, assetId)
            ?.let { buildCexAsset(it) }
    }

    fun getAllForAccount(account: Account): List<CexAsset> {
        return cexAssetsDao.getAllForAccount(account.id)
            .map {
                buildCexAsset(it)
            }
    }

}