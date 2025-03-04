package io.horizontalsystems.solanakit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class InitialSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val initial: Boolean
)
