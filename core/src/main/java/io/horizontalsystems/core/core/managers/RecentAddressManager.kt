package io.horizontalsystems.core.core.managers

import io.horizontalsystems.core.core.IAccountManager
import io.horizontalsystems.core.core.storage.RecentAddressDao
import io.horizontalsystems.core.entities.Address
import io.horizontalsystems.core.entities.RecentAddress
import io.horizontalsystems.marketkit.models.BlockchainType

class RecentAddressManager(
    private val accountManager: IAccountManager,
    private val dao: RecentAddressDao,
    private val actionCompletedDelegate: ActionCompletedDelegate
) {

    fun setRecentAddress(address: Address, blockchainType: BlockchainType) {
        accountManager.activeAccount?.let { activeAccount ->
            dao.insert(RecentAddress(activeAccount.id, blockchainType, address.hex))
            actionCompletedDelegate.notifyContactAdded()
        }
    }

    fun getRecentAddress(blockchainType: BlockchainType): String? {
        return accountManager.activeAccount?.let { activeAccount ->
            dao.get(activeAccount.id, blockchainType)?.address
        }
    }

}
