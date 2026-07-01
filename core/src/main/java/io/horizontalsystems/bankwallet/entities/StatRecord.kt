package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class StatRecord(val json: String) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}
