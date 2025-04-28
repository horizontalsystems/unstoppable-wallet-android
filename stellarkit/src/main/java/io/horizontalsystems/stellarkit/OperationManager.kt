package io.horizontalsystems.stellarkit

import android.util.Log
import io.horizontalsystems.stellarkit.room.Operation
import io.horizontalsystems.stellarkit.room.OperationDao
import io.horizontalsystems.stellarkit.room.OperationInfo
import io.horizontalsystems.stellarkit.room.OperationSyncState
import io.horizontalsystems.stellarkit.room.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.stellar.sdk.Server
import org.stellar.sdk.requests.RequestBuilder

class OperationManager(
    private val server: Server,
    private val dao: OperationDao,
    private val address: String,
) {
    private val operationFlow = MutableSharedFlow<OperationInfoWithTags>()

    private val _syncStateFlow =
        MutableStateFlow<SyncState>(SyncState.NotSynced(StellarKit.SyncError.NotStarted))
    val syncStateFlow = _syncStateFlow.asStateFlow()

    fun operations(tagQuery: TagQuery, beforeId: Long?, limit: Int?): List<Operation> {
        return dao.operations(tagQuery, beforeId ?: Long.MAX_VALUE, limit ?: 100)
    }

    fun operationFlow(tagQuery: TagQuery): Flow<OperationInfo> {
        var filteredOperationFlow: Flow<OperationInfoWithTags> = operationFlow.asSharedFlow()

        if (!tagQuery.isEmpty) {
            filteredOperationFlow = filteredOperationFlow.filter { info: OperationInfoWithTags ->
                info.operations.any { operationWithTags ->
                    operationWithTags.tags.any { it.conforms(tagQuery) }
                }
            }
        }

        return filteredOperationFlow.map { info ->
            OperationInfo(
                info.operations.map { it.operation },
                info.initial
            )
        }
    }

    suspend fun sync() {
        Log.d("AAA", "Syncing operations...")

        if (_syncStateFlow.value is SyncState.Syncing) {
            Log.d("AAA", "Syncing operations is in progress")
            return
        }

        _syncStateFlow.update {
            SyncState.Syncing
        }

        try {
            val latestOperation = dao.latestOperation()
            if (latestOperation != null) {
                Log.d("AAA", "Fetching latest operations...")

                var pagingToken = latestOperation.pagingToken

                do {
                    val operations = getOperations(address, pagingToken, limit, RequestBuilder.Order.ASC)
                    Log.d("AAA", "Got latest operations: ${operations.size}, pagingToken: $pagingToken")

                    handle(operations, false)

                    if (operations.size < limit) {
                        break
                    }

                    pagingToken = operations.last().pagingToken

                } while (true)
            }

            val operationSyncState = dao.operationSyncState()
            val allSynced = operationSyncState?.allSynced ?: false
            if (!allSynced) {
                Log.d("AAA", "Fetching history operations...")

                val oldestOperation = dao.oldestOperation()
                var pagingToken = oldestOperation?.pagingToken
                do {
                    val operations = getOperations(address, pagingToken, limit, RequestBuilder.Order.DESC)
                    Log.d("AAA", "Got history operations: ${operations.size}, pagingToken: $pagingToken")

                    handle(operations, true)

                    if (operations.size < limit) {
                        break
                    }

                    pagingToken = operations.last().pagingToken

                } while (true)

                val newOldestOperation = dao.oldestOperation()

                if (newOldestOperation != null) {
                    dao.save(OperationSyncState(allSynced = true))
                }
            }

            _syncStateFlow.update {
                SyncState.Synced
            }
        } catch (e: Throwable) {
            _syncStateFlow.update {
                SyncState.NotSynced(e)
            }
        }
    }

    private fun getOperations(
        address: String, pagingToken: String?, limit: Int, order: RequestBuilder.Order
    ): List<Operation> {
        Log.e("AAA", "order: $order")
        val operationsRequest = server.operations()
            .forAccount(address)
            .limit(limit)
            .order(order)
            .cursor(pagingToken)

        val execute = operationsRequest.execute()

        return execute.records.map {
            Operation.fromApi(it)
        }
    }

    private suspend fun handle(operations: List<Operation>, initial: Boolean) {
        if (operations.isEmpty()) return

        dao.save(operations)
        val operationWithTags = operations.map { operation ->
            OperationWithTags(operation, operation.tags(address))
        }

        val tags = operationWithTags.map { it.tags }.flatten()
        dao.resave(tags, operations.map { it.id })

        operationFlow.emit(OperationInfoWithTags(operationWithTags, initial))
    }

    companion object {
        private const val limit = 200
    }

    private data class OperationWithTags(
        val operation: Operation,
        val tags: List<Tag>,
    )

    private data class OperationInfoWithTags(
        val operations: List<OperationWithTags>,
        val initial: Boolean,
    )
}