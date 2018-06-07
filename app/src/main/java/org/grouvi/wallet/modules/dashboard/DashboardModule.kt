package org.grouvi.wallet.modules.dashboard

import android.content.Context
import android.content.Intent

object DashboardModule {
    fun start(context: Context) {
        val intent = Intent(context, DashboardActivity::class.java)
        context.startActivity(intent)
    }
}