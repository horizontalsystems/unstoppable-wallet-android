package io.horizontalsystems.solanakit.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity
class TokenAccount(
    @PrimaryKey
    val address: String,
    val mintAddress: String,
    val balance: BigDecimal,
    val decimals: Int
) {

    override fun equals(other: Any?): Boolean {
        return (other as? TokenAccount)?.let { it.address == address } ?: false
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

}
