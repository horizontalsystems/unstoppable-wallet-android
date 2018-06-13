package bitcoin.wallet

import android.support.v7.app.AppCompatActivity
import android.util.Log
import bitcoin.wallet.lib.WalletDataManager
import bitcoin.wallet.modules.addWallet.AddWalletModule
import bitcoin.wallet.modules.dashboard.DashboardModule

class LauncherActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()

        if (WalletDataManager.hasWallet()) {
            DashboardModule.start(this)
        } else {
            AddWalletModule.start(this)
        }

        finish()
    }

}

fun Any?.log(label: String = "") {

    Log.e("AAA", "$label: $this")

}