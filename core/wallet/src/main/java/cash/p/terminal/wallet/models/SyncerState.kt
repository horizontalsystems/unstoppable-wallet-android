package cash.p.terminal.wallet.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SyncerState(
    @PrimaryKey
    val key: String,
    val value: String,
)
