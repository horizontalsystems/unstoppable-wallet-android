package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.AccountSettingRecordStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountSettingRecord
import io.horizontalsystems.bankwallet.entities.EvmBlockchain

class AccountSettingManager(val storage: AccountSettingRecordStorage) {

    private fun evmSyncSourceKey(blockchain: EvmBlockchain) =
        "evm-sync-source-${blockchain.uid}"

    fun getEvmSyncSourceName(account: Account, blockchain: EvmBlockchain) =
        storage.accountSetting(account.id, evmSyncSourceKey(blockchain))?.value

    fun save(evmSyncSourceName: String, account: Account, blockchain: EvmBlockchain) {
        val record = AccountSettingRecord(account.id, evmSyncSourceKey(blockchain), evmSyncSourceName)
        storage.save(record)
    }

}