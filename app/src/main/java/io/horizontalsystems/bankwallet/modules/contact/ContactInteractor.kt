package io.horizontalsystems.bankwallet.modules.contact

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IClipboardManager

class ContactInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private var clipboardManager: IClipboardManager
) : ContactModule.IInteractor {

    override val email get() = appConfigProvider.reportEmail
    override val telegramGroup get() = appConfigProvider.telegramGroupForWalletHelp

    override fun copyToClipboard(value: String) {
        clipboardManager.copyText(value)
    }

}
