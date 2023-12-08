package io.horizontalsystems.bankwallet.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class MarketWidgetManager {

    private var coroutineScope: CoroutineScope? = CoroutineScope(Dispatchers.Default)

    fun updateWatchListWidgets() {
        coroutineScope?.launch {
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

    fun refresh(glanceId: GlanceId) {
        coroutineScope?.launch {
            val context = App.instance
            try {
                executeWithRetry {
                    updateData(glanceId)
                }
            } catch (exception: Exception) {
                var state = getAppWidgetState(context, MarketWidgetStateDefinition, glanceId)

                val errorText = if (exception is UnknownHostException)
                    context.getString(R.string.Hud_Text_NoInternet)
                else {
                    context.getString(R.string.SyncError) + "\n\n\n" + "[ ${state.error} ]"
                }

                state = state.copy(loading = false, error = errorText)
                setWidgetState(context, glanceId, state)
            }
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
        marketItems =
            marketItems.map { it.copy(imageLocalPath = imagePathCache[it.imageRemoteUrl]) }

        state = state.copy(items = marketItems, loading = false, error = null)
        setWidgetState(context, glanceId, state)

        marketItems = marketItems.map { item ->
            item.copy(
                imageLocalPath = item.imageLocalPath ?: getImage(
                    context,
                    item.imageRemoteUrl
                )
            )
        }

        state =
            state.copy(
                items = marketItems,
                updateTimestampMillis = System.currentTimeMillis()
            )
        setWidgetState(context, glanceId, state)
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun getImage(context: Context, url: String): String? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        with(context.imageLoader) {
            val result = execute(request)
            if (result is ErrorResult) {
                return null
            }
        }

        val localPath = context.imageLoader.diskCache?.get(url)?.use { snapshot ->
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

    private val MAX_RETRIES = 5

    private suspend inline fun executeWithRetry(call: () -> Unit){
        for (i in 0..MAX_RETRIES) {
            try {
                call.invoke()
                break
            } catch (e: Exception) {
                delay(2000)

                if (i == MAX_RETRIES) {
                    throw e
                }
            }
        }
    }

    companion object {
        fun getMarketWidgetTypes(): List<MarketWidgetType> {
            val types = MarketWidgetType.values().toMutableList()
            // TopNfts type is hidden for now. It will be removed in next sprints
            types.remove(MarketWidgetType.TopNfts)
            if (!App.localStorage.marketsTabEnabled) {
                types.remove(MarketWidgetType.Watchlist)
            }

            return types
        }
    }

}
