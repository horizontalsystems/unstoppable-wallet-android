package io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.restoreSettingTypes
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.subjects.PublishSubject

class RestoreSettingsService(private val manager: RestoreSettingsManager) : Clearable {

    val approveSettingsObservable = PublishSubject.create<CoinWithSettings>()
    val rejectApproveSettingsObservable = PublishSubject.create<Coin>()
    val requestObservable = PublishSubject.create<Request>()

    fun approveSettings(platformCoin: PlatformCoin, account: Account? = null) {
        val coinType = platformCoin.coinType

        if (account != null && account.origin == AccountOrigin.Created) {
            val settings = RestoreSettings()
            coinType.restoreSettingTypes.forEach { settingType ->
                manager.getSettingValueForCreatedAccount(settingType, coinType)?.let {
                    settings[settingType] = it
                }
            }
            approveSettingsObservable.onNext(CoinWithSettings(platformCoin, settings))
            return
        }

        val existingSettings = account?.let { manager.settings(it, coinType) } ?: RestoreSettings()

        if (coinType.restoreSettingTypes.contains(RestoreSettingType.BirthdayHeight)
            && existingSettings.birthdayHeight == null
        ) {
            requestObservable.onNext(Request(platformCoin, RequestType.BirthdayHeight))
            return
        }

        approveSettingsObservable.onNext(CoinWithSettings(platformCoin, RestoreSettings()))
    }

    fun save(settings: RestoreSettings, account: Account, coinType: CoinType) {
        manager.save(settings, account, coinType)
    }

    fun enter(birthdayHeight: String?, platformCoin: PlatformCoin) {
        val settings = RestoreSettings()
        settings.birthdayHeight = birthdayHeight?.toIntOrNull()

        val coinWithSettings = CoinWithSettings(platformCoin, settings)
        approveSettingsObservable.onNext(coinWithSettings)
    }

    fun cancel(coin: Coin) {
        rejectApproveSettingsObservable.onNext(coin)
    }

    override fun clear() = Unit

    data class CoinWithSettings(val platformCoin: PlatformCoin, val settings: RestoreSettings)
    data class Request(val platformCoin: PlatformCoin, val requestType: RequestType)
    enum class RequestType {
        BirthdayHeight
    }
}
