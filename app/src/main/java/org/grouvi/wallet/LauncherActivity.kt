package org.grouvi.wallet

import android.support.v7.app.AppCompatActivity
import android.util.Log
import org.grouvi.wallet.lib.WalletDataManager
import org.grouvi.wallet.modules.addWallet.AddWalletModule
import org.grouvi.wallet.modules.dashboard.DashboardModule


class LauncherActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()

        if (WalletDataManager.hasWallet()) {
            DashboardModule.start(this)
        } else {
            AddWalletModule.start(this)
        }
    }

}

fun Any?.log(label: String = "") {

    Log.e("AAA", "$label: $this")
}