package io.horizontalsystems.bankwallet.modules.contact.appstatus

import io.horizontalsystems.bankwallet.core.IAppStatusManager
import io.horizontalsystems.bankwallet.core.IClipboardManager

class AppStatusInteractor(
        private val appStatusManager: IAppStatusManager,
        private val clipboardManager: IClipboardManager
) : AppStatusModule.IInteractor {

    override val status: Map<String, Any>
        get() = appStatusManager.status

    override fun copyToClipboard(text: String) {
        clipboardManager.copyText(text)
    }

}
