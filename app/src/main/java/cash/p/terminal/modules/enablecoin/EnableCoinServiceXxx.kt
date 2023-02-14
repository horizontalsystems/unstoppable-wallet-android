package cash.p.terminal.modules.enablecoin

import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.restoreSettingTypes
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class EnableCoinServiceXxx(
    private val restoreSettingsService: RestoreSettingsService
) {
    private val disposable = CompositeDisposable()

    val enableCoinObservable = PublishSubject.create<Pair<List<ConfiguredToken>, RestoreSettings>>()
    val enableSingleCoinObservable = PublishSubject.create<Pair<ConfiguredToken, RestoreSettings>>()

    init {
        restoreSettingsService.approveSettingsObservable
            .subscribeIO { handleApproveRestoreSettings(it.token, it.settings) }
            .let { disposable.add(it) }
    }

    private fun handleApproveRestoreSettings(
        token: Token,
        settings: RestoreSettings = RestoreSettings()
    ) {
        enableCoinObservable.onNext(Pair(listOf(ConfiguredToken(token)), settings))
    }

    fun enable(configuredToken: ConfiguredToken, account: Account) {
        when {
            configuredToken.token.blockchainType.restoreSettingTypes.isNotEmpty() -> {
                restoreSettingsService.approveSettings(configuredToken.token, account)
            }
            else -> {
                enableSingleCoinObservable.onNext(Pair(configuredToken, RestoreSettings()))
            }
        }
    }

    fun save(restoreSettings: RestoreSettings, account: Account, blockchainType: BlockchainType) {
        restoreSettingsService.save(restoreSettings, account, blockchainType)
    }
}
