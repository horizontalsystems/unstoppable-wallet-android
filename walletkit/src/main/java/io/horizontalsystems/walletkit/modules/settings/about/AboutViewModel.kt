package io.horizontalsystems.walletkit.modules.settings.about

import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.ISystemInfoManager
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider

class AboutViewModel(
    private val appConfigProvider: IAppConfigProvider,
    private val systemInfoManager: ISystemInfoManager,
) : ViewModel() {

    val githubLink = appConfigProvider.appGithubLink
    val appWebPageLink = appConfigProvider.appWebPageLink
    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (io.horizontalsystems.walletkit.BuildConfig.DEBUG) {
                appVersion += " (${appConfigProvider.appBuild})"
            }

            return appVersion
        }
}
