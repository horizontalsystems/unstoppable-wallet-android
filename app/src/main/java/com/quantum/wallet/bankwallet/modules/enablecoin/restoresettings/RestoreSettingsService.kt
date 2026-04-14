package com.quantum.wallet.bankwallet.modules.enablecoin.restoresettings

import com.quantum.wallet.bankwallet.core.Clearable
import com.quantum.wallet.bankwallet.core.managers.MoneroBirthdayProvider
import com.quantum.wallet.bankwallet.core.managers.RestoreSettingType
import com.quantum.wallet.bankwallet.core.managers.RestoreSettings
import com.quantum.wallet.bankwallet.core.managers.RestoreSettingsManager
import com.quantum.wallet.bankwallet.core.managers.ZcashBirthdayProvider
import com.quantum.wallet.bankwallet.core.restoreSettingTypes
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.entities.AccountOrigin
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

    fun save(settings: RestoreSettings, account: Account, blockchainType: BlockchainType) {
        restoreSettingsManager.save(settings, account, blockchainType)
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
