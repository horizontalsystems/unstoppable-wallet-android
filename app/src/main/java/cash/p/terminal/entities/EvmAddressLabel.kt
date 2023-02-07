package cash.p.terminal.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EvmAddressLabel(
    @PrimaryKey
    val address: String,
    val label: String
)
