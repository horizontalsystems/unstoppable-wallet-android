package io.horizontalsystems.bankwallet.modules.nav3

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntryDecorator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Local for receiving results in a [ResultEventBus]
 */
object LocalResultEventBus {
    private val LocalResultEventBus: ProvidableCompositionLocal<ResultEventBus?> =
        compositionLocalOf { null }

    /**
     * The current [ResultEventBus]
     */
    val current: ResultEventBus
        @Composable
        get() = LocalResultEventBus.current ?: error("No ResultEventBus has been provided")

    /**
     * Provides a [ResultEventBus] to the composition
     */
    infix fun provides(
        bus: ResultEventBus
    ): ProvidedValue<ResultEventBus?> {
        return LocalResultEventBus.provides(bus)
    }
}
/**
 * An EventBus for passing results between multiple sets of screens.
 *
 * It provides a solution for event based results.
 */
class ResultEventBus {
    /**
     * Map from the result key to a channel of results.
     */
    val channelMap = mutableStateMapOf<String, Channel<Any?>>()

    /**
     * Provides a flow for the given resultKey.
     */
    inline fun <reified T> getResultFlow(resultKey: String = T::class.toString()): Flow<Any?>? {
        return channelMap[resultKey]?.receiveAsFlow()
    }

    /**
     * Sends a result into the channel associated with the given resultKey.
     */
    inline fun <reified T> sendResult(result: T, resultKey: String = T::class.toString()) {
        Log.e("AAA", "sendResult, resultKey: $resultKey")
        if (!channelMap.contains(resultKey)) {
            channelMap[resultKey] = Channel(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
        }
        channelMap[resultKey]?.trySend(result)
    }

    /**
     * Removes all results associated with the given key from the store.
     */
    inline fun <reified T> removeResult(resultKey: String = T::class.toString()) {
        channelMap.remove(resultKey)
    }
}

@Composable
fun <T : Any> rememberResultEventBusNavEntryDecorator(): ResultEventBusNavEntryDecorator<T> {
    return remember {
        ResultEventBusNavEntryDecorator(ResultEventBus())
    }
}

class ResultEventBusNavEntryDecorator<T : Any>(val resultEventBus: ResultEventBus) : NavEntryDecorator<T>(
    onPop = { },
    decorate = { entry ->
        CompositionLocalProvider(LocalResultEventBus provides resultEventBus) {
            entry.Content()
        }
    },
)

