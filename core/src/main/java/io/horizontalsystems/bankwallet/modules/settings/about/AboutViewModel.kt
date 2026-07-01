package io.horizontalsystems.bankwallet.modules.settings.about

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.providers.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.core.ISystemInfoManager
import kotlinx.coroutines.launch

class AboutViewModel(
    private val appConfigProvider: IAppConfigProvider,
    private val termsManager: ITermsManager,
    private val systemInfoManager: ISystemInfoManager,
) : ViewModel() {

    val githubLink = appConfigProvider.appGithubLink
    val appWebPageLink = appConfigProvider.appWebPageLink
    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (io.horizontalsystems.core.BuildConfig.DEBUG) {
                appVersion += " (${appConfigProvider.appBuild})"
            }

            return appVersion
        }

    var termsShowAlert by mutableStateOf(!termsManager.allTermsAccepted)
        private set

    init {
        viewModelScope.launch {
            termsManager.termsAcceptedSharedFlow.collect {
                termsShowAlert = !it
            }
        }
    }

}
