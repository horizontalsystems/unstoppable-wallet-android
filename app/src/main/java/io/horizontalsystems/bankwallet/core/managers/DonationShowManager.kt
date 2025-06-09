package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.ILocalStorage

class DonationShowManager(
    private val localStorage: ILocalStorage,
) {

    fun shouldShow(): Boolean {
        //show only for FDroid builds
        val isFDroidBuild = BuildConfig.FDROID_BUILD
        if (!isFDroidBuild) {
            return false
        }

        val donateUsLastShown = localStorage.donateUsLastShownDate

        //check last shown date is null or more than 30 days ago
        //else return false
        val oneMonthPeriod = 30 * 24 * 60 * 60 * 1000L
        val currentTimeMillis = System.currentTimeMillis()
        if (donateUsLastShown == null || (currentTimeMillis - donateUsLastShown) > oneMonthPeriod) {
            localStorage.donateUsLastShownDate = currentTimeMillis
            return true
        }

        return false
    }

    fun updateDonatePageShownDate() {
        localStorage.donateUsLastShownDate = System.currentTimeMillis()
    }

}
