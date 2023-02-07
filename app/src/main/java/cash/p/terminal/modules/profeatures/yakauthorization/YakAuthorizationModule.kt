package cash.p.terminal.modules.profeatures.yakauthorization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.profeatures.HSProFeaturesAdapter

object YakAuthorizationModule {

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val configProvider = App.appConfigProvider
            val adapter = HSProFeaturesAdapter(configProvider.marketApiBaseUrl, configProvider.marketApiKey)
            val service = YakAuthorizationService(App.proFeatureAuthorizationManager, adapter)

            return YakAuthorizationViewModel(service) as T
        }
    }

}
