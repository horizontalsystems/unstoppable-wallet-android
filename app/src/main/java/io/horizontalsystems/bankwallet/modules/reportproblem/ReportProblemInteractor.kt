package io.horizontalsystems.bankwallet.modules.reportproblem

import io.horizontalsystems.bankwallet.core.IAppConfigProvider

class ReportProblemInteractor(private val appConfigProvider: IAppConfigProvider) : ReportProblemModule.IInteractor {
    override val email get() = appConfigProvider.reportEmail
    override val telegramGroup get() = appConfigProvider.reportTelegramGroup
}
