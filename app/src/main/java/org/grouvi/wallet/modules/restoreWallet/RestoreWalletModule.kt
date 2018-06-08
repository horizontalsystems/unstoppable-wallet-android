package org.grouvi.wallet.modules.restoreWallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.grouvi.wallet.R

object RestoreWalletModule {

    fun start(context: Context) {
        val intent = Intent(context, RestoreWalletActivity::class.java)
        context.startActivity(intent)
    }

}

class RestoreWalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_wallet)
    }
}