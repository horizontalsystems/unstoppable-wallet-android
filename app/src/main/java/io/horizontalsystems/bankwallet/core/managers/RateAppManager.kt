package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.IWalletManager


class RateAppManager(
        private val context: Context,
        private val walletManager: IWalletManager,
        private val adapterManager: AdapterManager,
        private val storage: ILocalStorage) : IRateAppManager {

    private val MIN_LAUNCH_COUNT = 5
    private val COUNTDOWN_TIME_INTERVAL = 10 // 10 seconds
    private val repeatedRequestTimeInterval = 90 * 24 * 60 * 60


    private fun showDialog(){
    }

    override fun onBalancePageActive() {
    }

    override fun onBalancePageInactive() {
    }

}