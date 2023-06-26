package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexAssetRaw
import io.horizontalsystems.bankwallet.core.providers.CexNetwork
import io.horizontalsystems.bankwallet.core.providers.CexNetworkRaw
import io.horizontalsystems.bankwallet.entities.Account

class CexAssetManager(marketKit: MarketKitWrapper) {

    private val coins = marketKit.allCoins().map { it.uid to it }.toMap()
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
            blockchain = null,
        )
    }

    fun saveAll(cexAssetRaws: List<CexAssetRaw>, account: Account) {
        storage.set(account.id, cexAssetRaws)
    }

    fun getAll(account: Account): List<CexAsset> {
        return storage.get(account.id)?.map {
            buildCexAsset(it)
        } ?: listOf()
    }

}