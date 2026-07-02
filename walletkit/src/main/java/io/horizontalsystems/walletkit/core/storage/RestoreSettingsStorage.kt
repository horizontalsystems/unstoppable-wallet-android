package io.horizontalsystems.walletkit.core.storage

import io.horizontalsystems.walletkit.core.IRestoreSettingsStorage
import io.horizontalsystems.walletkit.entities.RestoreSettingRecord

class RestoreSettingsStorage(appDatabase: AppDatabase) : IRestoreSettingsStorage {
    private val dao: RestoreSettingDao by lazy {
        appDatabase.restoreSettingDao()
    }

    override fun restoreSettings(accountId: String, blockchainTypeUid: String): List<RestoreSettingRecord> {
        return dao.get(accountId, blockchainTypeUid)
    }

    override fun restoreSettings(accountId: String): List<RestoreSettingRecord> {
        return dao.get(accountId)
    }

    override fun save(restoreSettingRecords: List<RestoreSettingRecord>) {
        dao.insert(restoreSettingRecords)
    }

    override fun deleteAllRestoreSettings(accountId: String) {
        dao.delete(accountId)
    }
}
