package io.horizontalsystems.stellarkit.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import org.stellar.sdk.responses.operations.OperationResponse

@Entity
data class Event(
    @PrimaryKey
    val id: Long,
    val timestamp: Long,
    val pagingToken: String,
    val sourceAccount: String,
    val transactionHash: String,
    val transactionSuccessful: Boolean,
    val type: String,
) {
    fun tags(address: String): List<Tag> {
        val tags = mutableListOf<Tag>()

        if (sourceAccount == address) {
            tags.add(
                Tag(
                    id,
                    Tag.Type.Outgoing,
                    Tag.Platform.Native,
//                    addresses = listOf(tonTransfer.recipient.address)
                )
            )
        } else {
            tags.add(
                Tag(
                    id,
                    Tag.Type.Incoming,
                    Tag.Platform.Native,
//                    addresses = listOf(tonTransfer.recipient.address)
                )
            )

        }

        return tags
    }

    companion object {
        fun fromApi(operationResponse: OperationResponse) = Event(
            id = operationResponse.id,
            timestamp = Instant.parse(operationResponse.createdAt).epochSeconds,
            pagingToken = operationResponse.pagingToken,
            sourceAccount = operationResponse.sourceAccount,
            transactionHash = operationResponse.transactionHash,
            transactionSuccessful = operationResponse.transactionSuccessful,
            type = operationResponse.type,
        )
    }
}

data class EventInfo(
    val events: List<Event>,
    val initial: Boolean,
)

@Entity
data class EventSyncState(
    @PrimaryKey
    val id: String,
    val allSynced: Boolean,
) {
    constructor(allSynced: Boolean) : this("unique_id", allSynced)
}
