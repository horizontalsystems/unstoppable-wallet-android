package cash.p.terminal.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import cash.p.terminal.wallet.entities.AccountRecord

@Entity(
    tableName = "LocallyCreatedTransaction",
    primaryKeys = ["accountId", "blockchainTypeUid", "transactionHash"],
    indices = [
        Index(value = ["accountId", "createdAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ]
)
data class LocallyCreatedTransactionRecord(
    val accountId: String,
    val blockchainTypeUid: String,
    val transactionHash: String,
    val createdAt: Long,
)
