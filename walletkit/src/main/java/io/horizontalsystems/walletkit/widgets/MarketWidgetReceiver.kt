package io.horizontalsystems.walletkit.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MarketWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MarketWidget()

    // The system calls this on placement, reboot, app update and every updatePeriodMillis.
    // Re-enqueueing (UPDATE policy) keeps the periodic refresh alive even if the app itself
    // was never launched since, e.g. after a restore or a force stop.
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        MarketWidgetWorker.enqueueWork(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        MarketWidgetWorker.cancel(context)
    }

}
