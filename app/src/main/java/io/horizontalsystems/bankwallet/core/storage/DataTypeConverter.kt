package io.horizontalsystems.bankwallet.core.storage

import androidx.room.TypeConverter
import io.horizontalsystems.bankwallet.entities.SyncMode

class DataTypeConverter {
    @TypeConverter
    fun toString(syncMode: SyncMode): String? {
        return syncMode.value
    }

    @TypeConverter
    fun toSyncMode(string: String): SyncMode {
        return SyncMode.fromString(string)
    }
}
