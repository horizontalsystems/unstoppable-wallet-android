package io.horizontalsystems.bankwallet.modules.reportproblem

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IClipboardManager

class ReportProblemInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private var clipboardManager: IClipboardManager
) : ReportProblemModule.IInteractor {

    override val email get() = appConfigProvider.reportEmail
    override val telegramGroup get() = appConfigProvider.reportTelegramGroup

    override fun copyToClipboard(value: String) {
        clipboardManager.copyText(value)
    }

}
