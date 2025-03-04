package io.horizontalsystems.solanakit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class LastSyncedTransaction(@PrimaryKey var syncSourceName: String, val hash: String)
