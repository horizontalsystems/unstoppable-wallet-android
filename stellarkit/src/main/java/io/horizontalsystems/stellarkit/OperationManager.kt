package io.horizontalsystems.stellarkit

import android.util.Log
import io.horizontalsystems.stellarkit.room.TxOperation
import org.stellar.sdk.Server
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.responses.operations.OperationResponse


class OperationManager(
    private val server: Server,
    private val accountId: String,
) {
    fun sync() {
        Log.e("AAA", "==================== DESC")

        val operationsRequest = server.operations().forAccount(accountId)
            .limit(10)
//            .order(RequestBuilder.Order.DESC)

        val execute = operationsRequest.execute()
        execute.records.forEach(::handleOperation)

        Log.e("AAA", "==================== ASC")

        val operationsRequest1 = server.operations().forAccount(accountId)
            .limit(10)
            .order(RequestBuilder.Order.ASC)

        val execute1 = operationsRequest1.execute()
        execute1.records.forEach(::handleOperation)


//        operationsRequest.stream(object : EventListener<OperationResponse> {
//            override fun onEvent(operationResponse: OperationResponse) {
//
//                handleOperation(operationResponse)
//            }
//
//
//            override fun onFailure(error: Optional<Throwable>, responseCode: Optional<Int>) {
//                TODO("Not yet implemented")
//            }
//        })
    }

    private fun handleOperation(operationResponse: OperationResponse) {
        val txOperation = TxOperation(
            id = operationResponse.id,
            createdAt = operationResponse.createdAt,
            pagingToken = operationResponse.pagingToken,
            sourceAccount = operationResponse.sourceAccount,
            transactionHash = operationResponse.transactionHash,
            transactionSuccessful = operationResponse.transactionSuccessful,
            type = operationResponse.type,
        )

        Log.e("AAA", "operation: $txOperation")
    }

}


