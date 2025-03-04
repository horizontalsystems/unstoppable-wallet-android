package io.horizontalsystems.solanakit.models

import androidx.room.*
import java.math.BigDecimal

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = arrayOf("hash"),
            childColumns = arrayOf("transactionHash"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = arrayOf("transactionHash"))
    ]
)
class TokenTransfer(
    val transactionHash: String,
    val mintAddress: String,
    val incoming: Boolean,
    val amount: BigDecimal,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)
