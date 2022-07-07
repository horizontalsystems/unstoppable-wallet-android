package io.horizontalsystems.bankwallet.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import io.horizontalsystems.bankwallet.core.App

class MarketWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MarketWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        App.marketWidgetManager.start()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)

        App.marketWidgetManager.stop()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        appWidgetIds.forEach {
            MarketWidgetWorker.cancel(context, it)
        }
    }

}
