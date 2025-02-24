package io.horizontalsystems.solanakit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class BalanceEntity(val lamports: Long, @PrimaryKey val id: String = "" )
