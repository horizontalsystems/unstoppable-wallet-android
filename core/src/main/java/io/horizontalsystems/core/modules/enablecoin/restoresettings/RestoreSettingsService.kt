package io.horizontalsystems.core.modules.enablecoin.restoresettings

import io.horizontalsystems.core.core.Clearable
import io.horizontalsystems.core.core.managers.MoneroBirthdayProvider
import io.horizontalsystems.core.core.managers.RestoreSettingType
import io.horizontalsystems.core.core.managers.RestoreSettings
import io.horizontalsystems.core.core.managers.RestoreSettingsManager
import io.horizontalsystems.core.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.core.core.restoreSettingTypes
import io.horizontalsystems.core.entities.Account
import io.horizontalsystems.core.entities.AccountOrigin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.subjects.PublishSubject

class RestoreSettingsService(
    private val restoreSettingsManager: RestoreSettingsManager,
    private val zcashBirthdayProvider: ZcashBirthdayProvider,
    private val moneroBirthdayProvider: MoneroBirthdayProvider
) : Clearable {

    val approveSettingsObservable = PublishSubject.create<TokenWithSettings>()
    val rejectApproveSettingsObservable = PublishSubject.create<Token>()
    val requestObservable = PublishSubject.create<Request>()

    fun approveSettings(token: Token, account: Account? = null) {
        val blockchainType = token.blockchainType

        if (account != null && account.origin == AccountOrigin.Created) {
            val settings = RestoreSettings()
            blockchainType.restoreSettingTypes.forEach { settingType ->
                restoreSettingsManager.getSettingValueForCreatedAccount(settingType, blockchainType)?.let {
                    settings[settingType] = it
                }
            }
            approveSettingsObservable.onNext(TokenWithSettings(token, settings))
            return
        }

        val existingSettings = account?.let { restoreSettingsManager.settings(it, blockchainType) } ?: RestoreSettings()

        if (blockchainType.restoreSettingTypes.contains(RestoreSettingType.BirthdayHeight)
            && existingSettings.birthdayHeight == null
        ) {
            requestObservable.onNext(Request(token, RequestType.BirthdayHeight))
            return
        }

        approveSettingsObservable.onNext(TokenWithSettings(token, RestoreSettings()))
    }

    fun save(settings: RestoreSettings, account: Account, blockchainType: BlockchainType, reload: Boolean = true) {
        restoreSettingsManager.save(settings, account, blockchainType, reload)
    }

    fun enter(config: BirthdayHeightConfig, token: Token) {
        val settings = RestoreSettings()
        settings.birthdayHeight = if (config.restoreAsNew) {
            when (token.blockchainType) {
                BlockchainType.Zcash -> zcashBirthdayProvider.getLatestCheckpointBlockHeight()
                BlockchainType.Monero -> moneroBirthdayProvider.restoreHeightForNewWallet()
                else -> null
            }
        } else {
            config.birthdayHeight?.toLongOrNull()
        }

        val tokenWithSettings = TokenWithSettings(token, settings)
        approveSettingsObservable.onNext(tokenWithSettings)
    }

    fun cancel(token: Token) {
        rejectApproveSettingsObservable.onNext(token)
    }

    override fun clear() = Unit

    data class TokenWithSettings(val token: Token, val settings: RestoreSettings)
    data class Request(val token: Token, val requestType: RequestType)
    enum class RequestType {
        BirthdayHeight
    }
}
