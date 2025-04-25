package io.horizontalsystems.stellarkit

import android.util.Log
import io.horizontalsystems.stellarkit.room.Event
import io.horizontalsystems.stellarkit.room.EventSyncState
import io.horizontalsystems.stellarkit.room.OperationDao
import io.horizontalsystems.stellarkit.room.Tag
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.stellar.sdk.Server
import org.stellar.sdk.requests.RequestBuilder

class EventManager(
    private val server: Server,
    private val dao: OperationDao,
    private val address: String,
) {
    private val eventFlow = MutableSharedFlow<EventInfoWithTags>()

    private val _syncStateFlow =
        MutableStateFlow<SyncState>(SyncState.NotSynced(StellarKit.SyncError.NotStarted))
    val syncStateFlow = _syncStateFlow.asStateFlow()

    fun operations(tagQuery: TagQuery, beforeId: Long?, limit: Int?): List<Event> {
        return dao.operations(beforeId ?: Long.MAX_VALUE, limit ?: 100)
    }

//    fun eventFlow(tagQuery: TagQuery): Flow<EventInfo> {
//        var filteredEventFlow: Flow<EventInfoWithTags> = eventFlow.asSharedFlow()
//
//        if (!tagQuery.isEmpty) {
//            filteredEventFlow = filteredEventFlow.filter { info: EventInfoWithTags ->
//                info.events.any { eventWithTags ->
//                    eventWithTags.tags.any { it.conforms(tagQuery) }
//                }
//            }
//        }
//
//        return filteredEventFlow.map { info ->
//            EventInfo(
//                info.events.map { it.event },
//                info.initial
//            )
//        }
//    }

//    fun tagTokens(): List<TagToken> {
//        return dao.tagTokens()
//    }

    suspend fun sync() {
        Log.d("AAA", "Syncing events...")

        if (_syncStateFlow.value is SyncState.Syncing) {
            Log.d("AAA", "Syncing events is in progress")
            return
        }

        _syncStateFlow.update {
            SyncState.Syncing
        }

        try {
            val latestEvent = dao.latestEvent()
            if (latestEvent != null) {
                Log.d("AAA", "Fetching latest events...")

                var pagingToken = latestEvent.pagingToken

                do {
                    val events = getEvents(address, pagingToken, limit, RequestBuilder.Order.ASC)
                    Log.d("AAA", "Got latest events: ${events.size}, pagingToken: $pagingToken")

                    handle(events, false)

                    if (events.size < limit) {
                        break
                    }

                    pagingToken = events.last().pagingToken

                } while (true)
            }

            val eventSyncState = dao.eventSyncState()
            val allSynced = eventSyncState?.allSynced ?: false
            if (!allSynced) {
                Log.d("AAA", "Fetching history events...")

                val oldestEvent = dao.oldestEvent()
                var pagingToken = oldestEvent?.pagingToken
                do {
                    val events = getEvents(address, pagingToken, limit, RequestBuilder.Order.DESC)
                    Log.d("AAA", "Got history events: ${events.size}, pagingToken: $pagingToken")

                    handle(events, true)

                    if (events.size < limit) {
                        break
                    }

                    pagingToken = events.last().pagingToken

                } while (true)

                val newOldestEvent = dao.oldestEvent()

                if (newOldestEvent != null) {
                    dao.save(EventSyncState(allSynced = true))
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

    private fun getEvents(
        address: String, pagingToken: String?, limit: Int, order: RequestBuilder.Order
    ): List<Event> {
        Log.e("AAA", "order: $order")
        val operationsRequest = server.operations()
            .forAccount(address)
            .limit(limit)
            .order(order)
            .cursor(pagingToken)

        val execute = operationsRequest.execute()

        return execute.records.map {
            Event.fromApi(it)
        }
    }

    private suspend fun handle(events: List<Event>, initial: Boolean) {
        if (events.isEmpty()) return

        dao.save(events)
        val eventsWithTags = events.map { event ->
            EventWithTags(event, event.tags(address))
        }

        val tags = eventsWithTags.map { it.tags }.flatten()
        dao.resave(tags, events.map { it.id })

        eventFlow.emit(EventInfoWithTags(eventsWithTags, initial))
    }

    companion object {
        private const val limit = 200
    }

    private data class EventWithTags(
        val event: Event,
        val tags: List<Tag>,
    )

    private data class EventInfoWithTags(
        val events: List<EventWithTags>,
        val initial: Boolean,
    )
}