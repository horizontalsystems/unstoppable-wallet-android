package io.horizontalsystems.core.core.storage

import io.horizontalsystems.core.core.IRestoreSettingsStorage
import io.horizontalsystems.core.entities.RestoreSettingRecord

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
