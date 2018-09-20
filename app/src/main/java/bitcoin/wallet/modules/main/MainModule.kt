package bitcoin.wallet.modules.main

import android.content.Context
import android.content.Intent

object MainModule {
    fun start(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
    }
}