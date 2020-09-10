package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.managewallets.ui.main.ManageWalletsFragment

class ManageWalletsActivityMvvm : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_wallets_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, ManageWalletsFragment.newInstance())
                    .commitNow()
        }
    }
}