package org.grouvi.wallet.modules.dashboard

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.grouvi.wallet.MainActivity
import org.grouvi.wallet.R
import org.grouvi.wallet.lib.WalletDataManager

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        buttonRemoveWallet.setOnClickListener {
            WalletDataManager.mnemonicWords = listOf()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            finish()
        }
    }
}