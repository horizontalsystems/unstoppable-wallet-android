package io.horizontalsystems.bankwallet.modules.enablecoin

import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.restoreSettingTypes
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.supportedTokens
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class EnableCoinService(
    private val coinTokensService: CoinTokensService,
    private val restoreSettingsService: RestoreSettingsService
) {
    private val disposable = CompositeDisposable()

    val enableCoinObservable = PublishSubject.create<Pair<List<ConfiguredToken>, RestoreSettings>>()
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

    fun enable(fullCoin: FullCoin, account: Account? = null) {
        val supportedTokens = fullCoin.supportedTokens
        if (supportedTokens.size == 1) {
            val token = supportedTokens.first()
            when {
                token.blockchainType.restoreSettingTypes.isNotEmpty() -> {
                    restoreSettingsService.approveSettings(token, account)
                }
                token.type != TokenType.Native -> {
                    coinTokensService.approveTokens(fullCoin)
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
        val currentTokens = configuredTokens.map { it.token }
        coinTokensService.approveTokens(fullCoin, currentTokens, true)
    }

    fun save(restoreSettings: RestoreSettings, account: Account, blockchainType: BlockchainType) {
        restoreSettingsService.save(restoreSettings, account, blockchainType)
    }
}
