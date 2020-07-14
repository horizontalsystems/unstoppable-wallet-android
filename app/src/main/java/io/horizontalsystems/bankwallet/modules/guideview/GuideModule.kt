package io.horizontalsystems.bankwallet.modules.guideview

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.GuidesManager

object GuideModule {
    const val GuideUrlKey = "GuideUrlKey"

    fun start(context: Context, guideUrl: String) {
        val intent = Intent(context, GuideActivity::class.java)
        intent.putExtra(GuideUrlKey, guideUrl)

        context.startActivity(intent)
    }

    class Factory(private val guideUrl: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GuideViewModel(guideUrl, GuidesManager, App.connectivityManager) as T
        }
    }
}
