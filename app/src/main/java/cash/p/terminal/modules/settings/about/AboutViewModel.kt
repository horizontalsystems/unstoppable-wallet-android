package cash.p.terminal.modules.settings.about

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.ITermsManager
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.strings.helpers.Translator
import io.horizontalsystems.core.ISystemInfoManager
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
            if (cash.p.terminal.strings.helpers.Translator.getString(R.string.is_release) == "false") {
                appVersion += " (${appConfigProvider.appBuild})"
            }

            return appVersion
        }

    var termsShowAlert by mutableStateOf(!termsManager.allTermsAccepted)
        private set

    init {
        viewModelScope.launch {
            termsManager.termsAcceptedSignalFlow.collect {
                termsShowAlert = !it
            }
        }
    }

}
