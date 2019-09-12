package io.horizontalsystems.bankwallet.modules.createwallet

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.modules.restore.RestoreActivity

object CreateWalletModule {

    fun start(context: Context) {
        context.startActivity(Intent(context, CreateWalletActivity::class.java))
    }
}
