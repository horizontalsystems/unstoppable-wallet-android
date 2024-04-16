package io.horizontalsystems.bankwallet.core.stats

import android.util.Log

fun stat(page: StatPage, section: StatSection? = null, event: StatEvent) {
    Log.e("e", "PAGE: $page  ${section ?: ""}, event: ${event.name}, ${event.params?.let { ", PARAMS: $it" } ?: ""}")
//    App.statManager.logStat(page, section, event)
}

class StatManager {

    fun logStat(eventPage: StatPage, eventSection: StatSection? = null, event: StatEvent) {

    }

}
