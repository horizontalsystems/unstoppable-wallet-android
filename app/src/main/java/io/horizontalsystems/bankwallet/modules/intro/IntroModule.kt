package io.horizontalsystems.bankwallet.modules.intro

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object IntroModule {
    interface IRouter {
        fun navigateToWelcome()
    }

    fun start(context: Context) {
        val intent = Intent(context, IntroActivity::class.java)
        context.startActivity(intent)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val presenter = IntroPresenter(IntroRouter())

            return presenter as T
        }
    }

}
