package io.horizontalsystems.bankwallet.modules.releasenotes

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager

class ReleaseNotesViewModel(
        appConfigProvider: IAppConfigProvider,
        releaseNotesManager: ReleaseNotesManager
) : ViewModel() {

    val releaseNotesUrl = releaseNotesManager.releaseNotesUrl
    val twitterUrl = appConfigProvider.appTwitterLink
    val telegramUrl = appConfigProvider.appTelegramLink
    val redditUrl = appConfigProvider.appRedditLink
}
