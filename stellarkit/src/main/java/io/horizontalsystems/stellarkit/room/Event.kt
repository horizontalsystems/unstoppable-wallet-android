package io.horizontalsystems.stellarkit.room

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.stellarkit.room.Tag.Type
import kotlinx.datetime.Instant
import org.stellar.sdk.MemoText
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse

@Entity
data class Event(
    @PrimaryKey
    val id: Long,
    val timestamp: Long,
    val pagingToken: String,
    val sourceAccount: String,
    val transactionHash: String,
    val transactionSuccessful: Boolean,
    val memo: String?,
    val type: String,
    @Embedded
    val  payment: Payment?,
    @Embedded
    val accountCreated: AccountCreated?,
) {
    data class Payment(val amount: String, val asset: StellarAsset, val from: String, val to: String)
    data class AccountCreated(val startingBalance: String, val funder: String, val account: String)

    fun tags(accountId: String): List<Tag> {
        val tags = mutableListOf<Tag>()

        accountCreated?.let { accountCreated ->
            if (accountCreated.funder == accountId) {
                tags.add(Tag(id, Type.Outgoing, StellarAsset.Native.id, listOf(accountCreated.account)))
            }

            if (accountCreated.account == accountId) {
                tags.add(Tag(id, Type.Incoming, StellarAsset.Native.id, listOf(accountCreated.funder)))
            }
        }

        payment?.let { data ->
            if (data.from == accountId) {
                tags.add(Tag(id, Type.Outgoing, data.asset.id, listOf(data.to)))
            }

            if (data.to == accountId) {
                tags.add(Tag(id, Type.Incoming, data.asset.id, listOf(data.from)))
            }
        }

        return tags
    }

    companion object {
        fun fromApi(operationResponse: OperationResponse): Event {
            var payment: Payment? = null
            var accountCreated: AccountCreated? = null

            when (operationResponse) {
                is PaymentOperationResponse -> {
                    payment = Payment(
                        amount = operationResponse.amount,
                        asset = StellarAsset.fromSdkModel(operationResponse.asset),
                        from = operationResponse.from,
                        to = operationResponse.to,
                    )
                }
                is CreateAccountOperationResponse -> {
                    accountCreated = AccountCreated(
                        startingBalance = operationResponse.startingBalance,
                        funder = operationResponse.funder,
                        account = operationResponse.account,
                    )
                }
            }

            return Event(
                id = operationResponse.id,
                timestamp = Instant.parse(operationResponse.createdAt).epochSeconds,
                pagingToken = operationResponse.pagingToken,
                sourceAccount = operationResponse.sourceAccount,
                transactionHash = operationResponse.transactionHash,
                transactionSuccessful = operationResponse.transactionSuccessful,
                memo = (operationResponse.transaction?.memo as? MemoText)?.text,
                type = operationResponse.type,
                payment = payment,
                accountCreated = accountCreated,
            )
        }
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
