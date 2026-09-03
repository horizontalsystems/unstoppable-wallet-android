package io.horizontalsystems.walletkit.modules.enablecoin.restoresettings

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.managers.RestoreSettingType
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.RestoreSettingsManager
import io.horizontalsystems.walletkit.core.restoreSettingTypes
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class RestoreSettingsService(
    private val restoreSettingsManager: RestoreSettingsManager
) : Clearable {

    private val _approveSettingsFlow = MutableSharedFlow<TokenWithSettings>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val approveSettingsFlow: Flow<TokenWithSettings> = _approveSettingsFlow

    private val _rejectApproveSettingsFlow = MutableSharedFlow<Token>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val rejectApproveSettingsFlow: Flow<Token> = _rejectApproveSettingsFlow

    private val _requestFlow = MutableSharedFlow<Request>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestFlow: Flow<Request> = _requestFlow

    fun approveSettings(token: Token, account: Account? = null) {
        val blockchainType = token.blockchainType

        if (account != null && account.origin == AccountOrigin.Created) {
            val settings = RestoreSettings()
            blockchainType.restoreSettingTypes.forEach { settingType ->
                restoreSettingsManager.getSettingValueForCreatedAccount(settingType, blockchainType)?.let {
                    settings[settingType] = it
                }
            }
            _approveSettingsFlow.tryEmit(TokenWithSettings(token, settings))
            return
        }

        val existingSettings = account?.let { restoreSettingsManager.settings(it, blockchainType) } ?: RestoreSettings()

        if (blockchainType.restoreSettingTypes.contains(RestoreSettingType.BirthdayHeight)
            && existingSettings.birthdayHeight == null
        ) {
            _requestFlow.tryEmit(Request(token, RequestType.BirthdayHeight))
            return
        }

        _approveSettingsFlow.tryEmit(TokenWithSettings(token, RestoreSettings()))
    }

    fun save(settings: RestoreSettings, account: Account, blockchainType: BlockchainType, reload: Boolean = true) {
        restoreSettingsManager.save(settings, account, blockchainType, reload)
    }

    fun enter(config: BirthdayHeightConfig, token: Token) {
        val settings = RestoreSettings()
        settings.birthdayHeight = if (config.restoreAsNew) {
            when (token.blockchainType) {
                BlockchainType.Zcash -> ChainRegistry[BlockchainType.Zcash]?.newWalletBirthdayHeight()
                BlockchainType.Monero -> ChainRegistry[BlockchainType.Monero]?.newWalletBirthdayHeight()
                else -> null
            }
        } else {
            config.birthdayHeight?.toLongOrNull()
        }

        val tokenWithSettings = TokenWithSettings(token, settings)
        _approveSettingsFlow.tryEmit(tokenWithSettings)
    }

    fun cancel(token: Token) {
        _rejectApproveSettingsFlow.tryEmit(token)
    }

    override fun clear() = Unit

    data class TokenWithSettings(val token: Token, val settings: RestoreSettings)
    data class Request(val token: Token, val requestType: RequestType)
    enum class RequestType {
        BirthdayHeight
    }
}
