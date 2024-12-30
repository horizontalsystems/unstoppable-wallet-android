package cash.p.terminal.core.managers

import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexAssetRaw
import cash.p.terminal.core.storage.CexAssetsDao
import cash.p.terminal.wallet.MarketKitWrapper
import java.math.BigDecimal

class CexAssetManager(marketKit: MarketKitWrapper, private val cexAssetsDao: CexAssetsDao) {

    private val coins by lazy { marketKit.allCoins().map { it.uid to it }.toMap() }
    private val allBlockchains by lazy { marketKit.allBlockchains().map { it.uid to it }.toMap() }

    private fun buildCexAsset(cexAssetRaw: CexAssetRaw): CexAsset {
        return CexAsset(
            id = cexAssetRaw.id,
            name = cexAssetRaw.name,
            freeBalance = cexAssetRaw.freeBalance,
            lockedBalance = cexAssetRaw.lockedBalance,
            depositEnabled = cexAssetRaw.depositEnabled,
            withdrawEnabled = cexAssetRaw.withdrawEnabled,
            depositNetworks = cexAssetRaw.depositNetworks.map { it.cexDepositNetwork(allBlockchains[it.blockchainUid]) },
            withdrawNetworks = cexAssetRaw.withdrawNetworks.map { it.cexWithdrawNetwork(allBlockchains[it.blockchainUid]) },
            coin = coins[cexAssetRaw.coinUid],
            decimals = cexAssetRaw.decimals
        )
    }

    fun saveAllForAccount(cexAssetRaws: List<CexAssetRaw>, account: cash.p.terminal.wallet.Account) {
        cexAssetsDao.delete(account.id)
        cexAssetsDao.insert(cexAssetRaws)
    }

    fun get(account: cash.p.terminal.wallet.Account, assetId: String): CexAsset? {
        return cexAssetsDao.get(account.id, assetId)
            ?.let { buildCexAsset(it) }
    }

    fun getAllForAccount(account: cash.p.terminal.wallet.Account): List<CexAsset> {
        return cexAssetsDao.getAllForAccount(account.id)
            .map {
                buildCexAsset(it)
            }
    }

    fun getWithBalance(account: cash.p.terminal.wallet.Account): List<CexAsset> {
        return cexAssetsDao.getAllForAccount(account.id)
            .filter { it.freeBalance > BigDecimal.ZERO }
            .map {
                buildCexAsset(it)
            }
    }

}