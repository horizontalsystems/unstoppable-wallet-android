package io.horizontalsystems.walletkit.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.UnknownHostException

class MarketWidgetManager {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun updateWatchListWidgets() {
        launchContained {
            val context = App.instance
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(MarketWidget::class.java)

            for (glanceId in glanceIds) {
                val state = getAppWidgetState(context, MarketWidgetStateDefinition, glanceId)
                if (state.type == MarketWidgetType.Watchlist) {
                    refresh(glanceId)
                }
            }
        }
    }

    /**
     * Fire-and-forget refresh for callers that cannot suspend. Prefer [refresh] from a worker or
     * an action callback: the process may be killed as soon as those return, and a refresh left
     * running on a detached scope would die with it.
     */
    fun refreshInBackground(glanceId: GlanceId) {
        launchContained { refresh(glanceId) }
    }

    // refresh() catches fetch errors itself, but its failure path (widget state IO, work
    // scheduling) can throw too; nothing above this scope handles that, so it would crash
    // the app. Contain everything except cancellation.
    private fun launchContained(block: suspend () -> Unit) {
        coroutineScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Market widget update failed")
            }
        }
    }

    /**
     * Fetches fresh data for the widget and re-renders it.
     *
     * @return true when the data was updated, false when every attempt failed. On failure the
     * previously shown items are kept; the error is only shown when there is nothing to show.
     */
    suspend fun refresh(glanceId: GlanceId): Boolean {
        val context = App.instance
        return try {
            executeWithRetry {
                updateData(glanceId)
            }
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.e(exception, "Market widget refresh failed")
            var state = getAppWidgetState(context, MarketWidgetStateDefinition, glanceId)

            val errorText = if (exception is UnknownHostException) {
                context.getString(R.string.Hud_Text_NoInternet)
            } else {
                context.getString(R.string.SyncError) + "\n\n\n" + "[ ${exception.message} ]"
            }

            state = state.afterRefreshFailure(errorText)
            setWidgetState(context, glanceId, state)
            // Doze or airplane mode: sync as soon as connectivity is back rather than waiting
            // for the next periodic slot.
            MarketWidgetWorker.enqueueSyncWhenOnline(context)
            false
        }
    }

    private suspend fun updateData(glanceId: GlanceId) {
        val context = App.instance
        val marketRepository = App.marketWidgetRepository
        var state = getAppWidgetState(context, MarketWidgetStateDefinition, glanceId)
        val imagePathCache = buildMap {
            state.items.forEach { item ->
                item.imageLocalPath?.let { set(item.imageRemoteUrl, it) }
            }
        }
        var marketItems = marketRepository.getMarketItems(state.type)
        marketItems = marketItems.map { it.copy(imageLocalPath = imagePathCache[it.imageRemoteUrl]) }

        state = state.copy(
            items = marketItems,
            loading = false,
            error = null,
            updateTimestampMillis = System.currentTimeMillis()
        )
        setWidgetState(context, glanceId, state)

        // Icons are best effort: a missing image must not fail the rates update.
        marketItems = marketItems.map { item ->
            item.copy(
                imageLocalPath = item.imageLocalPath ?: getImage(
                    context,
                    item.imageRemoteUrl,
                    item.alternativeRemoteUrl
                )
            )
        }

        if (marketItems != state.items) {
            state = state.copy(items = marketItems)
            setWidgetState(context, glanceId, state)
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun getImage(
        context: Context,
        url: String,
        alternativeUrl: String?
    ): String? {
        var downloadedUrl = url
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        with(context.imageLoader) {
            var result = execute(request)
            if (result is ErrorResult && alternativeUrl != null) {
                val fallbackRequest = ImageRequest.Builder(context)
                    .data(alternativeUrl)
                    .build()
                result = execute(fallbackRequest)
                if (result is ErrorResult) {
                    return null
                }
                downloadedUrl = alternativeUrl
            }
        }

        val localPath = context.imageLoader.diskCache?.openSnapshot(downloadedUrl)?.use { snapshot ->
            snapshot.data.toFile().path
        }

        return localPath
    }

    private suspend fun setWidgetState(context: Context, glanceId: GlanceId, state: MarketWidgetState) {
        updateAppWidgetState(context, MarketWidgetStateDefinition, glanceId) {
            state
        }
        MarketWidget().update(context, glanceId)
    }

    private suspend inline fun executeWithRetry(call: () -> Unit) {
        for (i in 0..MAX_RETRIES) {
            try {
                call.invoke()
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnknownHostException) {
                // No network: retrying in-process is pointless, the online sync job handles it.
                throw e
            } catch (e: Exception) {
                if (i == MAX_RETRIES) {
                    throw e
                }
                delay(RETRY_DELAY_MILLIS)
            }
        }
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MILLIS = 2000L

        fun getMarketWidgetTypes(): List<MarketWidgetType> {
            val types = MarketWidgetType.values().toMutableList()
            if (!App.localStorage.marketsTabEnabled) {
                types.remove(MarketWidgetType.Watchlist)
            }

            return types
        }
    }

}
