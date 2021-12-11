package io.horizontalsystems.bankwallet.modules.enablecoin

import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.marketkit.models.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class EnableCoinService(
    private val coinPlatformsService: CoinPlatformsService,
    private val restoreSettingsService: RestoreSettingsService,
    private val coinSettingsService: CoinSettingsService
) {
    private val disposable = CompositeDisposable()

    val enableCoinObservable = PublishSubject.create<Pair<List<ConfiguredPlatformCoin>, RestoreSettings>>()
    val cancelEnableCoinObservable = PublishSubject.create<Coin>()

    init {
        coinPlatformsService.approvePlatformsObservable
            .subscribeIO { handleApproveCoinPlatforms(it.coin, it.platforms) }
            .let { disposable.add(it) }

        coinPlatformsService.rejectApprovePlatformsObservable
            .subscribeIO { handleRejectApprovePlatformSettings(it) }
            .let { disposable.add(it) }

        restoreSettingsService.approveSettingsObservable
            .subscribeIO { handleApproveRestoreSettings(it.platformCoin, it.settings) }
            .let { disposable.add(it) }

        restoreSettingsService.rejectApproveSettingsObservable
            .subscribeIO { handleRejectApproveRestoreSettings(it) }
            .let { disposable.add(it) }

        coinSettingsService.approveSettingsObservable
            .subscribeIO { handleApproveCoinSettings(it.coin, it.settingsList) }
            .let { disposable.add(it) }

        coinSettingsService.rejectApproveSettingsObservable
            .subscribeIO { handleRejectApproveCoinSettings(it) }
            .let { disposable.add(it) }
    }

    private fun handleApproveCoinPlatforms(coin: Coin, platforms: List<Platform>) {
        val configuredPlatformCoins = platforms.map { ConfiguredPlatformCoin(PlatformCoin(it, coin)) }
        enableCoinObservable.onNext(Pair(configuredPlatformCoins, RestoreSettings()))
    }

    private fun handleRejectApprovePlatformSettings(coin: Coin) {
        cancelEnableCoinObservable.onNext(coin)
    }

    private fun handleApproveRestoreSettings(
        platformCoin: PlatformCoin,
        settings: RestoreSettings = RestoreSettings()
    ) {
        enableCoinObservable.onNext(Pair(listOf(ConfiguredPlatformCoin(platformCoin)), settings))
    }

    private fun handleRejectApproveRestoreSettings(coin: Coin) {
        cancelEnableCoinObservable.onNext(coin)
    }

    private fun handleApproveCoinSettings(platformCoin: PlatformCoin, settingsList: List<CoinSettings> = listOf()) {
        val configuredPlatformCoins = settingsList.map { ConfiguredPlatformCoin(platformCoin, it) }
        enableCoinObservable.onNext(Pair(configuredPlatformCoins, RestoreSettings()))
    }

    private fun handleRejectApproveCoinSettings(coin: Coin) {
        cancelEnableCoinObservable.onNext(coin)
    }

    fun enable(fullCoin: FullCoin, account: Account? = null) {
        if (fullCoin.platforms.size == 1) {
            val platformCoin = PlatformCoin(fullCoin.platforms.first(), fullCoin.coin)
            when {
                platformCoin.coinType.restoreSettingTypes.isNotEmpty() -> {
                    restoreSettingsService.approveSettings(platformCoin, account)
                }
                platformCoin.coinType.coinSettingTypes.isNotEmpty() -> {
                    coinSettingsService.approveSettings(platformCoin, platformCoin.coinType.defaultSettingsArray)
                }
                else -> {
                    enableCoinObservable.onNext(Pair(listOf(ConfiguredPlatformCoin(platformCoin)), RestoreSettings()))
                }
            }
        } else {
            coinPlatformsService.approvePlatforms(fullCoin)
        }
    }

    fun configure(fullCoin: FullCoin, configuredPlatformCoins: List<ConfiguredPlatformCoin>) {
        if (fullCoin.platforms.size == 1) {
            val platform = fullCoin.platforms.first()
            if (platform.coinType.coinSettingTypes.isNotEmpty()) {
                val settings = configuredPlatformCoins.map { it.coinSettings }
                val platformCoin = PlatformCoin(platform, fullCoin.coin)
                coinSettingsService.approveSettings(platformCoin, settings)
            }
        } else {
            val currentPlatforms = configuredPlatformCoins.map { it.platformCoin.platform }
            coinPlatformsService.approvePlatforms(fullCoin, currentPlatforms)
        }
    }

    fun save(restoreSettings: RestoreSettings, account: Account, coinType: CoinType) {
        restoreSettingsService.save(restoreSettings, account, coinType)
    }
}
