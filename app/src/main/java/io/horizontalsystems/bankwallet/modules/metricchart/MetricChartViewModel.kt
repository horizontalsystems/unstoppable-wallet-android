package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.lifecycle.ViewModel

class MetricChartViewModel(
    private val service: MetricChartService,
) : ViewModel() {
    val title by service::title
    val description by service::description
    val poweredBy by service::poweredBy
}
