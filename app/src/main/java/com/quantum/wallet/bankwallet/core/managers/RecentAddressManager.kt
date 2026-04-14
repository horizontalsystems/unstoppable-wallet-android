package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.IAccountManager
import com.quantum.wallet.bankwallet.core.storage.RecentAddressDao
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.RecentAddress
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
