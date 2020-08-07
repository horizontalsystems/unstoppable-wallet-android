package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LogEntry(
        var date: Long,
        var level: Int,
        var actionId: String,
        var message: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}
