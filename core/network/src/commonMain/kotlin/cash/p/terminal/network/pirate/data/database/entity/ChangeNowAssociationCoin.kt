package cash.p.terminal.network.pirate.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cash.p.terminal.network.pirate.domain.enity.ChangeNowAssociatedCoin

@Entity(tableName = "change_now_coins")
internal data class ChangeNowAssociationCoin(
    @PrimaryKey
    val uid: String,
    val coinData: List<ChangeNowAssociatedCoin>,
    val timestamp: Long
)
