package io.horizontalsystems.solanakit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class LastBlockHeightEntity(val height: Long, @PrimaryKey val id: String = "")
