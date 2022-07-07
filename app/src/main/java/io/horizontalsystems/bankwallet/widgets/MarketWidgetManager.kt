package io.horizontalsystems.bankwallet.widgets

import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import io.horizontalsystems.bankwallet.core.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MarketWidgetManager {

    private var coroutineScope: CoroutineScope? = CoroutineScope(Dispatchers.Default)

    fun start() {
        coroutineScope = CoroutineScope(Dispatchers.Default)
    }

    fun stop() {
        coroutineScope?.cancel()
        coroutineScope = null
    }

    fun updateWatchListWidgets() {
        coroutineScope?.launch {
            val context = App.instance
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(MarketWidget::class.java)

            for (glanceId in glanceIds) {
                val state = getAppWidgetState(context, MarketWidgetStateDefinition, glanceId)
                if (state.type == MarketWidgetType.Watchlist) {
                    MarketWidgetWorker.enqueue(context, state.widgetId)
                }
            }
        }
    }

}
