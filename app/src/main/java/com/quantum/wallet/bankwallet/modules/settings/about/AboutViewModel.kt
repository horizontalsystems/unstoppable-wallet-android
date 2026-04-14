package com.quantum.wallet.bankwallet.modules.settings.about

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.ITermsManager
import com.quantum.wallet.bankwallet.core.providers.AppConfigProvider
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.core.ISystemInfoManager
import kotlinx.coroutines.launch

class AboutViewModel(
    private val appConfigProvider: AppConfigProvider,
    private val termsManager: ITermsManager,
    private val systemInfoManager: ISystemInfoManager,
) : ViewModel() {

    val githubLink = appConfigProvider.appGithubLink
    val appWebPageLink = appConfigProvider.appWebPageLink
    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (Translator.getString(R.string.is_release) == "false") {
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
