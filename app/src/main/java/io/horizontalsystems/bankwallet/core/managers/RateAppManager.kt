package io.horizontalsystems.bankwallet.core.managers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.time.Instant


class RateAppManager(
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val localStorage: ILocalStorage) : IRateAppManager {

    private val _showRateFlow = MutableStateFlow(false)
    override val showRateAppFlow = _showRateFlow.filterNotNull()

    private val MIN_LAUNCH_COUNT = 5
    private val MIN_COINS_COUNT = 2
    private val COUNTDOWN_TIME_INTERVAL: Long = 10 * 1000 // 10 seconds
    private val REQUEST_TIME_INTERVAL = 40 * 24 * 60 * 60 // 40 Days

    private var isCountdownPassed = false
    private var isRequestAllowed = false
    private var isOnBalancePage = false

    private fun onCountdownPass() {
        var balance: BigDecimal = BigDecimal.ZERO

        for (wallet in walletManager.activeWallets) {
            val adapter = adapterManager.getBalanceAdapterForWallet(wallet)
            adapter?.let {
                balance = it.balanceData.available
            }

            if (balance > BigDecimal.ZERO)
                break
        }

        if (walletManager.activeWallets.size >= MIN_COINS_COUNT && balance > BigDecimal.ZERO) {
            isRequestAllowed = true
            showIfAllowed()
        }
    }

    override fun onBalancePageActive() {
        isOnBalancePage = true
        showIfAllowed()
    }

    override fun onBalancePageInactive() {
        isOnBalancePage = false
    }

    override fun onAppLaunch() {
        val launchCount = localStorage.appLaunchCount
        if (launchCount < MIN_LAUNCH_COUNT) {
            localStorage.appLaunchCount = launchCount + 1
            return
        }

        val lastRequestTime = localStorage.rateAppLastRequestTime
        if (lastRequestTime > 0 && (Instant.now().epochSecond - lastRequestTime) < REQUEST_TIME_INTERVAL) {
            return
        }

        if(!isCountdownPassed){
            startCountdownChecker()
        }
    }

    private fun showIfAllowed() {
        if (isOnBalancePage && isRequestAllowed) {
            localStorage.rateAppLastRequestTime = Instant.now().epochSecond
            isRequestAllowed = false
            _showRateFlow.update { true }
        }
    }

    private fun startCountdownChecker() {
        Handler(Looper.getMainLooper()).postDelayed({
            isCountdownPassed = true
            onCountdownPass()
        }, COUNTDOWN_TIME_INTERVAL)
    }

    companion object {

        fun openPlayMarket(context: Context) {
            try {
                ContextCompat.startActivity(context, getPlayMarketAppIntent(), null)
            } catch (e: ActivityNotFoundException) {
                val appPlayStoreLink =
                    "http://play.google.com/store/apps/details?id=io.horizontalsystems.bankwallet"
                LinkHelper.openLinkInAppBrowser(context, appPlayStoreLink)
            }
        }

        private fun getPlayMarketAppIntent(): Intent {
            val uri =
                Uri.parse("market://details?id=io.horizontalsystems.bankwallet")  //context.packageName
            val goToMarketIntent = Intent(Intent.ACTION_VIEW, uri)
            goToMarketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            return goToMarketIntent
        }

    }

}
