package io.horizontalsystems.bankwallet.modules.intro

import android.content.Context
import android.content.Intent

object IntroModule {

    fun start(context: Context) {
        val intent = Intent(context, IntroActivity::class.java)
        context.startActivity(intent)
    }

}
