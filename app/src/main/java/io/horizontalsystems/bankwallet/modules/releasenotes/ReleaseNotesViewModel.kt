package io.horizontalsystems.bankwallet.modules.releasenotes

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider

class ReleaseNotesViewModel(
    appConfigProvider: AppConfigProvider,
    releaseNotesManager: ReleaseNotesManager
) : ViewModel() {

    val releaseNotesUrl = releaseNotesManager.releaseNotesUrl
    val twitterUrl = appConfigProvider.appTwitterLink
    val telegramUrl = appConfigProvider.appTelegramLink
    val redditUrl = appConfigProvider.appRedditLink
}
