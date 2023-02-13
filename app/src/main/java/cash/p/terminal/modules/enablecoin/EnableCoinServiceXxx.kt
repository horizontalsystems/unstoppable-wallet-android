package cash.p.terminal.modules.enablecoin

import cash.p.terminal.core.*
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.CoinSettings
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.modules.enablecoin.coinplatforms.CoinTokensService
import cash.p.terminal.modules.enablecoin.coinsettings.CoinSettingsService
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class EnableCoinServiceXxx(
    private val coinTokensService: CoinTokensService,
    private val restoreSettingsService: RestoreSettingsService,
    private val coinSettingsService: CoinSettingsService
) {
    private val disposable = CompositeDisposable()

    val enableCoinObservable = PublishSubject.create<Pair<List<ConfiguredToken>, RestoreSettings>>()
    val enableSingleCoinObservable = PublishSubject.create<Pair<ConfiguredToken, RestoreSettings>>()
    val cancelEnableCoinObservable = PublishSubject.create<FullCoin>()

    init {
        coinTokensService.approveTokensObservable
            .subscribeIO { handleApproveCoinTokens(it.tokens) }
            .let { disposable.add(it) }

        coinTokensService.rejectApproveTokensObservable
            .subscribeIO { handleRejectApprovePlatformSettings(it) }
            .let { disposable.add(it) }

        restoreSettingsService.approveSettingsObservable
            .subscribeIO { handleApproveRestoreSettings(it.token, it.settings) }
            .let { disposable.add(it) }

        restoreSettingsService.rejectApproveSettingsObservable
            .subscribeIO { handleRejectApproveRestoreSettings(it) }
            .let { disposable.add(it) }

        coinSettingsService.approveSettingsObservable
            .subscribeIO { handleApproveCoinSettings(it.token, it.settingsList) }
            .let { disposable.add(it) }

        coinSettingsService.rejectApproveSettingsObservable
            .subscribeIO { handleRejectApproveCoinSettings(it) }
            .let { disposable.add(it) }
    }

    private fun handleApproveCoinTokens(tokens: List<Token>) {
        val configuredTokens = tokens.map { ConfiguredToken(it) }
        enableCoinObservable.onNext(Pair(configuredTokens, RestoreSettings()))
    }

    private fun handleRejectApprovePlatformSettings(fullCoin: FullCoin) {
        cancelEnableCoinObservable.onNext(fullCoin)
    }

    private fun handleApproveRestoreSettings(
        token: Token,
        settings: RestoreSettings = RestoreSettings()
    ) {
        enableCoinObservable.onNext(Pair(listOf(ConfiguredToken(token)), settings))
    }

    private fun handleRejectApproveRestoreSettings(token: Token) {
        cancelEnableCoinObservable.onNext(token.fullCoin)
    }

    private fun handleApproveCoinSettings(token: Token, settingsList: List<CoinSettings> = listOf()) {
        val configuredTokens = settingsList.map { ConfiguredToken(token, it) }
        enableCoinObservable.onNext(Pair(configuredTokens, RestoreSettings()))
    }

    private fun handleRejectApproveCoinSettings(token: Token) {
        cancelEnableCoinObservable.onNext(token.fullCoin)
    }

    fun enable(token: Token, accountType: AccountType, account: Account? = null) {
        when {
            token.blockchainType.restoreSettingTypes.isNotEmpty() -> {
                restoreSettingsService.approveSettings(token, account)
            }
            token.blockchainType.coinSettingType != null -> {
                coinSettingsService.approveSettings(token, accountType, token.blockchainType.defaultSettingsArray(accountType))
            }
            else -> {
                enableSingleCoinObservable.onNext(Pair(ConfiguredToken(token), RestoreSettings()))
            }
        }
    }

    fun configure(fullCoin: FullCoin, accountType: AccountType, configuredTokens: List<ConfiguredToken>) {
        val singleToken = fullCoin.supportedTokens.singleOrNull()

        if (singleToken != null && singleToken.blockchainType.coinSettingType != null) {
            val settings = configuredTokens.map { it.coinSettings }
            coinSettingsService.approveSettings(singleToken, accountType, settings, true)
        } else {
            val currentTokens = configuredTokens.map { it.token }
            coinTokensService.approveTokens(fullCoin, currentTokens, true)
        }
    }

    fun save(restoreSettings: RestoreSettings, account: Account, blockchainType: BlockchainType) {
        restoreSettingsService.save(restoreSettings, account, blockchainType)
    }
}
