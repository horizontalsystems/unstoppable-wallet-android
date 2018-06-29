package bitcoin.wallet

import android.support.v7.app.AppCompatActivity
import android.util.Log
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.modules.guest.GuestModule
import bitcoin.wallet.modules.main.MainModule
import io.realm.SyncUser

class LauncherActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()

        if (Factory.preferencesManager.savedWords != null && SyncUser.current() != null) {
            MainModule.start(this)
        } else {
            GuestModule.start(this)
        }

        finish()
    }

}

fun Any?.log(label: String = "") {

    Log.e("AAA", "$label: $this")

}