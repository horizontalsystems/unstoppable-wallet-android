package io.horizontalsystems.bankwallet.modules.enablecoin

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
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

    init {
        coinTokensService.approveTokensObservable
            .subscribeIO { handleApproveCoinTokens(it.tokens) }
            .let { disposable.add(it) }

        restoreSettingsService.approveSettingsObservable
            .subscribeIO { handleApproveRestoreSettings(it.token, it.settings) }
            .let { disposable.add(it) }

        coinSettingsService.approveSettingsObservable
            .subscribeIO { handleApproveCoinSettings(it.token, it.settingsList) }
            .let { disposable.add(it) }
    }

    private fun handleApproveCoinTokens(tokens: List<Token>) {
        val configuredTokens = tokens.map { ConfiguredToken(it) }
        enableCoinObservable.onNext(Pair(configuredTokens, RestoreSettings()))
    }

    private fun handleApproveRestoreSettings(
        token: Token,
        settings: RestoreSettings = RestoreSettings()
    ) {
        enableCoinObservable.onNext(Pair(listOf(ConfiguredToken(token)), settings))
    }

    private fun handleApproveCoinSettings(token: Token, settingsList: List<CoinSettings> = listOf()) {
        val configuredTokens = settingsList.map { ConfiguredToken(token, it) }
        enableCoinObservable.onNext(Pair(configuredTokens, RestoreSettings()))
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
