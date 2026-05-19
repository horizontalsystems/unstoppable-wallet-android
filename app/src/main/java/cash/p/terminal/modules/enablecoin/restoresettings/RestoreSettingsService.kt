package cash.p.terminal.modules.enablecoin.restoresettings

import cash.p.terminal.core.managers.LitecoinBirthdayProvider
import cash.p.terminal.core.managers.LitecoinMwebRestoreHeight
import cash.p.terminal.core.managers.RestoreSettingType
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.ZcashBirthdayProvider
import cash.p.terminal.core.restoreSettingTypes
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.isLitecoinMweb
import io.horizontalsystems.core.entities.BlockchainType
import io.reactivex.subjects.PublishSubject
import org.koin.java.KoinJavaComponent.inject

class RestoreSettingsService(
    private val manager: RestoreSettingsManager,
    private val zcashBirthdayProvider: ZcashBirthdayProvider,
    private val litecoinBirthdayProvider: LitecoinBirthdayProvider
) : Clearable {

    val approveSettingsObservable = PublishSubject.create<TokenWithSettings>()
    val rejectApproveSettingsObservable = PublishSubject.create<Token>()
    val requestObservable = PublishSubject.create<Request>()

    private val accountManager: IAccountManager by inject(IAccountManager::class.java)

    fun approveSettings(
        token: Token,
        account: Account? = null,
        forceRequest: Boolean = false,
        initialConfig: TokenConfig? = null
    ) {
        val blockchainType = token.blockchainType

        if (account != null && account.origin == AccountOrigin.Created) {
            val settings = RestoreSettings()
            token.restoreSettingTypes.forEach { settingType ->
                manager.getSettingValueForCreatedAccount(settingType, blockchainType)?.let {
                    settings[settingType] = it
                }
            }
            approveSettingsObservable.onNext(TokenWithSettings(token, settings))
            return
        }

        val existingSettings =
            account?.let { manager.settings(it, blockchainType) } ?: RestoreSettings()

        val requiresBirthdayHeight =
            token.restoreSettingTypes.contains(RestoreSettingType.BirthdayHeight)

        if (requiresBirthdayHeight && (forceRequest || existingSettings.birthdayHeight == null)
        ) {
            val requestConfig = initialConfig ?: existingSettings.birthdayHeight
                ?.takeIf { it > 0 }
                ?.let { height ->
                    TokenConfig(
                        birthdayHeight = height.toString(),
                        restoreAsNew = false
                    )
                }
            requestObservable.onNext(
                Request(
                    token = token,
                    requestType = RequestType.BirthdayHeight,
                    initialConfig = requestConfig,
                    accountId = account?.id
                )
            )
            return
        }

        approveSettingsObservable.onNext(
            TokenWithSettings(
                token = token,
                settings = if (requiresBirthdayHeight) existingSettings else RestoreSettings()
            )
        )
    }

    fun save(settings: RestoreSettings, account: Account, blockchainType: BlockchainType) {
        manager.save(settings, account, blockchainType)
    }

    fun enter(tokenConfig: TokenConfig, token: Token): Boolean {
        val settings = RestoreSettings()
        settings.birthdayHeight = when (token.blockchainType) {
            BlockchainType.Zcash -> {
                if (tokenConfig.restoreAsNew) {
                    tokenConfig.birthdayHeight?.toLongOrNull()
                        ?: zcashBirthdayProvider.getLatestCheckpointBlockHeight()
                } else {
                    tokenConfig.birthdayHeight?.toLongOrNull()
                }
            }

            BlockchainType.Monero -> {
                tokenConfig.birthdayHeight?.toLongOrNull() ?: -1
            }

            BlockchainType.Litecoin -> {
                if (token.isLitecoinMweb) {
                    if (tokenConfig.restoreAsNew) {
                        LitecoinMwebRestoreHeight.parse(tokenConfig.birthdayHeight)
                            ?: litecoinBirthdayProvider.getLatestCheckpointBlockHeight()
                    } else {
                        LitecoinMwebRestoreHeight.parse(tokenConfig.birthdayHeight)
                    }
                } else {
                    null
                }
            }

            else -> null
        }
        val changed = accountManager.activeAccount?.let { activeAccount ->
            getSettings(activeAccount, token.blockchainType).birthdayHeight
        }?.let { prevHeight ->
            settings.birthdayHeight != prevHeight
        } ?: false

        val tokenWithSettings = TokenWithSettings(token, settings)
        approveSettingsObservable.onNext(tokenWithSettings)
        return changed
    }

    fun cancel(token: Token) {
        rejectApproveSettingsObservable.onNext(token)
    }

    fun getSettings(account: Account, blockchainType: BlockchainType): RestoreSettings {
        return manager.settings(account, blockchainType)
    }

    override fun clear() = Unit

    data class TokenWithSettings(val token: Token, val settings: RestoreSettings)
    data class Request(
        val token: Token,
        val requestType: RequestType,
        val initialConfig: TokenConfig? = null,
        val accountId: String? = null
    )

    enum class RequestType {
        BirthdayHeight
    }
}
