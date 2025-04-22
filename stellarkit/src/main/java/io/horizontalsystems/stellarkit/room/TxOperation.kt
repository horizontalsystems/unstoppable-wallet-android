package io.horizontalsystems.stellarkit.room

data class TxOperation(
    val id: Long,
    val createdAt: String,
    val pagingToken: String,
    val sourceAccount: String,
    val transactionHash: String,
    val transactionSuccessful: Boolean,
    val type: String,
)