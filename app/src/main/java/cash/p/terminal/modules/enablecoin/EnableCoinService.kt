package cash.p.terminal.modules.enablecoin

import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.restoreSettingTypes
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.core.supportedTokens
import cash.p.terminal.entities.Account
import cash.p.terminal.modules.enablecoin.coinplatforms.CoinTokensService
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
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

    val enableCoinObservable = PublishSubject.create<Pair<List<Token>, RestoreSettings>>()
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
        enableCoinObservable.onNext(Pair(tokens, RestoreSettings()))
    }

    private fun handleRejectApprovePlatformSettings(fullCoin: FullCoin) {
        cancelEnableCoinObservable.onNext(fullCoin)
    }

    private fun handleApproveRestoreSettings(
        token: Token,
        settings: RestoreSettings = RestoreSettings()
    ) {
        enableCoinObservable.onNext(Pair(listOf(token), settings))
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
                    enableCoinObservable.onNext(Pair(listOf(token), RestoreSettings()))
                }
            }
        } else {
            coinTokensService.approveTokens(fullCoin)
        }
    }

    fun configure(fullCoin: FullCoin, tokens: List<Token>) {
        coinTokensService.approveTokens(fullCoin, tokens, true)
    }

    fun save(restoreSettings: RestoreSettings, account: Account, blockchainType: BlockchainType) {
        restoreSettingsService.save(restoreSettings, account, blockchainType)
    }
}
