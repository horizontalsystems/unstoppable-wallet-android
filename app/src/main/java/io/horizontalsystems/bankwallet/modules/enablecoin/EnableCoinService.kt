package io.horizontalsystems.bankwallet.modules.enablecoin

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class EnableCoinService(
    private val coinTokensService: CoinTokensService,
    private val restoreSettingsService: RestoreSettingsService,
    private val coinSettingsService: CoinSettingsService
) {
    private val disposable = CompositeDisposable()

    val enableCoinObservable = PublishSubject.create<Pair<List<ConfiguredToken>, RestoreSettings>>()
    val cancelEnableCoinObservable = PublishSubject.create<FullCoin>()

    init {
        coinTokensService.approveTokensObservable
            .subscribeIO { handleApproveCoinTokens(it.coin, it.tokens) }
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

    private fun handleApproveCoinTokens(coin: Coin, tokens: List<Token>) {
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

    fun enable(fullCoin: FullCoin, account: Account? = null) {
        val supportedTokens = fullCoin.supportedTokens
        if (supportedTokens.size == 1) {
            val token = supportedTokens.first()
            when {
                token.blockchainType.restoreSettingTypes.isNotEmpty() -> {
                    restoreSettingsService.approveSettings(token, account)
                }
                token.blockchainType.coinSettingTypes.isNotEmpty() -> {
                    coinSettingsService.approveSettings(token, token.blockchainType.defaultSettingsArray)
                }
                else -> {
                    enableCoinObservable.onNext(Pair(listOf(ConfiguredToken(token)), RestoreSettings()))
                }
            }
        } else {
            coinTokensService.approveTokens(fullCoin)
        }
    }

    fun configure(fullCoin: FullCoin, configuredTokens: List<ConfiguredToken>) {
        val supportedTokens = fullCoin.supportedTokens
        if (supportedTokens.size == 1) {
            val token = supportedTokens.first()
            if (token.blockchainType.coinSettingTypes.isNotEmpty()) {
                val settings = configuredTokens.map { it.coinSettings }
                coinSettingsService.approveSettings(token, settings)
            }
        } else {
            val currentTokens = configuredTokens.map { it.token }
            coinTokensService.approveTokens(fullCoin, currentTokens)
        }
    }

    fun save(restoreSettings: RestoreSettings, account: Account, blockchainType: BlockchainType) {
        restoreSettingsService.save(restoreSettings, account, blockchainType)
    }
}
