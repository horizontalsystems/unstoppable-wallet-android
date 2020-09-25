package io.horizontalsystems.bankwallet.modules.guideview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.GuidesManager

object GuideModule {

    class Factory(private val guideUrl: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GuideViewModel(guideUrl, GuidesManager, App.connectivityManager) as T
        }
    }
}
