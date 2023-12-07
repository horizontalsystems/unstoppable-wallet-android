package io.horizontalsystems.bankwallet.core.managers

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IMarketStorage
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.AppVersion
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppIcon
import io.horizontalsystems.bankwallet.modules.settings.security.autolock.AutoLockInterval
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.core.ILockoutStorage
import io.horizontalsystems.core.IPinSettingsStorage
import io.horizontalsystems.core.IThirdKeyboard
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class LocalStorageManager(
    private val preferences: SharedPreferences
) : ILocalStorage, IPinSettingsStorage, ILockoutStorage, IThirdKeyboard, IMarketStorage {

    private val THIRD_KEYBOARD_WARNING_MSG = "third_keyboard_warning_msg"
    private val SEND_INPUT_TYPE = "send_input_type"
    private val BASE_CURRENCY_CODE = "base_currency_code"
    private val AUTH_TOKEN = "auth_token"
    private val FAILED_ATTEMPTS = "failed_attempts"
    private val LOCKOUT_TIMESTAMP = "lockout_timestamp"
    private val BASE_BITCOIN_PROVIDER = "base_bitcoin_provider"
    private val BASE_LITECOIN_PROVIDER = "base_litecoin_provider"
    private val BASE_ETHEREUM_PROVIDER = "base_ethereum_provider"
    private val BASE_DASH_PROVIDER = "base_dash_provider"
    private val BASE_BINANCE_PROVIDER = "base_binance_provider"
    private val BASE_ZCASH_PROVIDER = "base_zcash_provider"
    private val SYNC_MODE = "sync_mode"
    private val SORT_TYPE = "balance_sort_type"
    private val APP_VERSIONS = "app_versions"
    private val ALERT_NOTIFICATION_ENABLED = "alert_notification"
    private val LOCK_TIME_ENABLED = "lock_time_enabled"
    private val ENCRYPTION_CHECKER_TEXT = "encryption_checker_text"
    private val BITCOIN_DERIVATION = "bitcoin_derivation"
    private val TOR_ENABLED = "tor_enabled"
    private val APP_LAUNCH_COUNT = "app_launch_count"
    private val RATE_APP_LAST_REQ_TIME = "rate_app_last_req_time"
    private val BALANCE_HIDDEN = "balance_hidden"
    private val TERMS_AGREED = "terms_agreed"
    private val MARKET_CURRENT_TAB = "market_current_tab"
    private val BIOMETRIC_ENABLED = "biometric_auth_enabled"
    private val PIN = "lock_pin"
    private val MAIN_SHOWED_ONCE = "main_showed_once"
    private val NOTIFICATION_ID = "notification_id"
    private val NOTIFICATION_SERVER_TIME = "notification_server_time"
    private val CURRENT_THEME = "current_theme"
    private val CHANGELOG_SHOWN_FOR_APP_VERSION = "changelog_shown_for_app_version"
    private val IGNORE_ROOTED_DEVICE_WARNING = "ignore_rooted_device_warning"
    private val SWAP_PROVIDER = "swap_provider_"
    private val LAUNCH_PAGE = "launch_page"
    private val APP_ICON = "app_icon"
    private val MAIN_TAB = "main_tab"
    private val MARKET_FAVORITES_SORTING_FIELD = "market_favorites_sorting_field"
    private val MARKET_FAVORITES_MARKET_FIELD = "market_favorites_market_field"
    private val RELAUNCH_BY_SETTING_CHANGE = "relaunch_by_setting_change"
    private val MARKETS_TAB_ENABLED = "markets_tab_enabled"
    private val BALANCE_AUTO_HIDE_ENABLED = "balance_auto_hide_enabled"
    private val NON_RECOMMENDED_ACCOUNT_ALERT_DISMISSED_ACCOUNTS = "non_recommended_account_alert_dismissed_accounts"
    private val PERSONAL_SUPPORT_ENABLED = "personal_support_enabled"
    private val APP_ID = "app_id"
    private val APP_AUTO_LOCK_INTERVAL = "app_auto_lock_interval"
    private val HIDE_SUSPICIOUS_TX = "hide_suspicious_tx"
    private val PIN_RANDOMIZED = "pin_randomized"

    private val gson by lazy { Gson() }

    override var chartIndicatorsEnabled: Boolean
        get() = preferences.getBoolean("chartIndicatorsEnabled", false)
        set(enabled) {
            preferences.edit().putBoolean("chartIndicatorsEnabled", enabled).apply()
        }

    override var amountInputType: AmountInputType?
        get() = preferences.getString(SEND_INPUT_TYPE, null)?.let {
            try {
                AmountInputType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        set(value) {
            val editor = preferences.edit()
            when (value) {
                null -> editor.remove(SEND_INPUT_TYPE).apply()
                else -> editor.putString(SEND_INPUT_TYPE, value.name).apply()
            }
        }

    override var marketSearchRecentCoinUids: List<String>
        get() = preferences.getString("marketSearchRecentCoinUids", null)?.split(",") ?: listOf()
        set(value) {
            preferences.edit().putString("marketSearchRecentCoinUids", value.joinToString(",")).apply()
        }

    override var zcashAccountIds: Set<String>
        get() = preferences.getStringSet("zcashAccountIds", setOf()) ?: setOf()
        set(value) {
            preferences.edit().putStringSet("zcashAccountIds", value).apply()
        }

    override var baseCurrencyCode: String?
        get() = preferences.getString(BASE_CURRENCY_CODE, null)
        set(value) {
            preferences.edit().putString(BASE_CURRENCY_CODE, value).apply()
        }

    override var authToken: String?
        get() = preferences.getString(AUTH_TOKEN, null)
        set(value) {
            preferences.edit().putString(AUTH_TOKEN, value).apply()
        }

    override val appId: String?
        get() {
            return when (val id = preferences.getString(APP_ID, null)) {
                null -> {
                    val newId = UUID.randomUUID().toString()
                    preferences.edit().putString(APP_ID, newId).apply()
                    newId
                }
                else -> id
            }
        }

    override var baseBitcoinProvider: String?
        get() = preferences.getString(BASE_BITCOIN_PROVIDER, null)
        set(value) {
            preferences.edit().putString(BASE_BITCOIN_PROVIDER, value).apply()
        }

    override var baseLitecoinProvider: String?
        get() = preferences.getString(BASE_LITECOIN_PROVIDER, null)
        set(value) {
            preferences.edit().putString(BASE_LITECOIN_PROVIDER, value).apply()
        }

    override var baseEthereumProvider: String?
        get() = preferences.getString(BASE_ETHEREUM_PROVIDER, null)
        set(value) {
            preferences.edit().putString(BASE_ETHEREUM_PROVIDER, value).apply()
        }

    override var baseDashProvider: String?
        get() = preferences.getString(BASE_DASH_PROVIDER, null)
        set(value) {
            preferences.edit().putString(BASE_DASH_PROVIDER, value).apply()
        }

    override var baseBinanceProvider: String?
        get() = preferences.getString(BASE_BINANCE_PROVIDER, null)
        set(value) {
            preferences.edit().putString(BASE_BINANCE_PROVIDER, value).apply()
        }

    override var baseZcashProvider: String?
        get() = preferences.getString(BASE_ZCASH_PROVIDER, null)
        set(value) {
            preferences.edit().putString(BASE_ZCASH_PROVIDER, value).apply()
        }

    override var sortType: BalanceSortType
        get() {
            val sortString = preferences.getString(SORT_TYPE, null)
                ?: BalanceSortType.Value.getAsString()
            return BalanceSortType.getTypeFromString(sortString)
        }
        set(sortType) {
            preferences.edit().putString(SORT_TYPE, sortType.getAsString()).apply()
        }

    override var appVersions: List<AppVersion>
        get() {
            val versionsString = preferences.getString(APP_VERSIONS, null) ?: return listOf()
            val type = object : TypeToken<ArrayList<AppVersion>>() {}.type
            return gson.fromJson(versionsString, type)
        }
        set(value) {
            val versionsString = gson.toJson(value)
            preferences.edit().putString(APP_VERSIONS, versionsString).apply()
        }

    override var isAlertNotificationOn: Boolean
        get() = preferences.getBoolean(ALERT_NOTIFICATION_ENABLED, true)
        set(enabled) {
            preferences.edit().putBoolean(ALERT_NOTIFICATION_ENABLED, enabled).apply()
        }

    override var isLockTimeEnabled: Boolean
        get() = preferences.getBoolean(LOCK_TIME_ENABLED, false)
        set(enabled) {
            preferences.edit().putBoolean(LOCK_TIME_ENABLED, enabled).apply()
        }

    override var encryptedSampleText: String?
        get() = preferences.getString(ENCRYPTION_CHECKER_TEXT, null)
        set(encryptedText) {
            preferences.edit().putString(ENCRYPTION_CHECKER_TEXT, encryptedText).apply()
        }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    override var currentTheme: ThemeType
        get() = preferences.getString(CURRENT_THEME, null)?.let { ThemeType.valueOf(it) }
            ?: ThemeType.System
        set(themeType) {
            preferences.edit().putString(CURRENT_THEME, themeType.value).apply()
        }

    override var balanceViewType: BalanceViewType?
        get() = preferences.getString("balanceViewType", null)?.let {
            try {
                BalanceViewType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        set(value) {
            if (value != null) {
                preferences.edit().putString("balanceViewType", value.name).apply()
            } else {
                preferences.edit().remove("balanceViewType").apply()
            }
        }

    //  IKeyboardStorage

    override var isThirdPartyKeyboardAllowed: Boolean
        get() = preferences.getBoolean(THIRD_KEYBOARD_WARNING_MSG, false)
        set(enabled) {
            preferences.edit().putBoolean(THIRD_KEYBOARD_WARNING_MSG, enabled).apply()
        }

    //  IPinStorage

    override var failedAttempts: Int?
        get() {
            val attempts = preferences.getInt(FAILED_ATTEMPTS, 0)
            return when (attempts) {
                0 -> null
                else -> attempts
            }
        }
        set(value) {
            value?.let {
                preferences.edit().putInt(FAILED_ATTEMPTS, it).apply()
            } ?: preferences.edit().remove(FAILED_ATTEMPTS).apply()
        }

    override var lockoutUptime: Long?
        get() {
            val timestamp = preferences.getLong(LOCKOUT_TIMESTAMP, 0L)
            return when (timestamp) {
                0L -> null
                else -> timestamp
            }
        }
        set(value) {
            value?.let {
                preferences.edit().putLong(LOCKOUT_TIMESTAMP, it).apply()
            } ?: preferences.edit().remove(LOCKOUT_TIMESTAMP).apply()
        }

    override var biometricAuthEnabled: Boolean
        get() = preferences.getBoolean(BIOMETRIC_ENABLED, false)
        set(value) {
            preferences.edit().putBoolean(BIOMETRIC_ENABLED, value).apply()
        }

    override var pin: String?
        get() = preferences.getString(PIN, null)
        set(value) {
            preferences.edit().putString(PIN, value).apply()
        }

    //used only in db migration
    override var syncMode: SyncMode?
        get() = preferences.getString(SYNC_MODE, null)?.let {
            try {
                SyncMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        set(syncMode) {
            preferences.edit().putString(SYNC_MODE, syncMode?.value).apply()
        }

    //used only in db migration
    override var bitcoinDerivation: AccountType.Derivation?
        get() {
            val derivationString = preferences.getString(BITCOIN_DERIVATION, null)
            return derivationString?.let { AccountType.Derivation.valueOf(it) }
        }
        set(derivation) {
            preferences.edit().putString(BITCOIN_DERIVATION, derivation?.value).apply()
        }

    //  IChartTypeStorage

    override var torEnabled: Boolean
        get() = preferences.getBoolean(TOR_ENABLED, false)
        @SuppressLint("ApplySharedPref")
        set(enabled) {
            //keep using commit() for synchronous storing
            preferences.edit().putBoolean(TOR_ENABLED, enabled).commit()
        }

    override var appLaunchCount: Int
        get() = preferences.getInt(APP_LAUNCH_COUNT, 0)
        set(value) {
            preferences.edit().putInt(APP_LAUNCH_COUNT, value).apply()
        }

    override var rateAppLastRequestTime: Long
        get() = preferences.getLong(RATE_APP_LAST_REQ_TIME, 0)
        set(value) {
            preferences.edit().putLong(RATE_APP_LAST_REQ_TIME, value).apply()
        }

    override var balanceHidden: Boolean
        get() = preferences.getBoolean(BALANCE_HIDDEN, false)
        set(value) {
            preferences.edit().putBoolean(BALANCE_HIDDEN, value).apply()
        }

    override var balanceAutoHideEnabled: Boolean
        get() = preferences.getBoolean(BALANCE_AUTO_HIDE_ENABLED, false)
        set(value) {
            preferences.edit().putBoolean(BALANCE_AUTO_HIDE_ENABLED, value).commit()
        }

    override var balanceTotalCoinUid: String?
        get() = preferences.getString("balanceTotalCoinUid", null)
        set(value) {
            preferences.edit().putString("balanceTotalCoinUid", value).apply()
        }

    override var termsAccepted: Boolean
        get() = preferences.getBoolean(TERMS_AGREED, false)
        set(value) {
            preferences.edit().putBoolean(TERMS_AGREED, value).apply()
        }

    override var currentMarketTab: MarketModule.Tab?
        get() = preferences.getString(MARKET_CURRENT_TAB, null)?.let {
            MarketModule.Tab.fromString(it)
        }
        set(value) {
            preferences.edit().putString(MARKET_CURRENT_TAB, value?.name).apply()
        }

    override var mainShowedOnce: Boolean
        get() = preferences.getBoolean(MAIN_SHOWED_ONCE, false)
        set(value) {
            preferences.edit().putBoolean(MAIN_SHOWED_ONCE, value).apply()
        }

    override var notificationId: String?
        get() = preferences.getString(NOTIFICATION_ID, null)
        set(value) {
            preferences.edit().putString(NOTIFICATION_ID, value).apply()
        }

    override var notificationServerTime: Long
        get() = preferences.getLong(NOTIFICATION_SERVER_TIME, 0)
        set(value) {
            preferences.edit().putLong(NOTIFICATION_SERVER_TIME, value).apply()
        }

    override var changelogShownForAppVersion: String?
        get() = preferences.getString(CHANGELOG_SHOWN_FOR_APP_VERSION, null)
        set(value) {
            preferences.edit().putString(CHANGELOG_SHOWN_FOR_APP_VERSION, value).apply()
        }

    override var ignoreRootedDeviceWarning: Boolean
        get() = preferences.getBoolean(IGNORE_ROOTED_DEVICE_WARNING, false)
        set(value) {
            preferences.edit().putBoolean(IGNORE_ROOTED_DEVICE_WARNING, value).apply()
        }

    override var launchPage: LaunchPage?
        get() = preferences.getString(LAUNCH_PAGE, null)?.let {
            LaunchPage.fromString(it)
        }
        set(value) {
            preferences.edit().putString(LAUNCH_PAGE, value?.name).apply()
        }

    override var appIcon: AppIcon?
        get() = preferences.getString(APP_ICON, null)?.let {
            AppIcon.fromString(it)
        }
        set(value) {
            preferences.edit().putString(APP_ICON, value?.name).apply()
        }

    override var mainTab: MainModule.MainNavigation?
        get() = preferences.getString(MAIN_TAB, null)?.let {
            MainModule.MainNavigation.fromString(it)
        }
        set(value) {
            preferences.edit().putString(MAIN_TAB, value?.name).apply()
        }

    override var marketFavoritesSortingField: SortingField?
        get() = preferences.getString(MARKET_FAVORITES_SORTING_FIELD, null)?.let {
            SortingField.fromString(it)
        }
        set(value) {
            preferences.edit().putString(MARKET_FAVORITES_SORTING_FIELD, value?.name).apply()
        }

    override var marketFavoritesMarketField: MarketField?
        get() = preferences.getString(MARKET_FAVORITES_MARKET_FIELD, null)?.let {
            MarketField.fromString(it)
        }
        set(value) {
            preferences.edit().putString(MARKET_FAVORITES_MARKET_FIELD, value?.name).apply()
        }

    override var relaunchBySettingChange: Boolean
        get() = preferences.getBoolean(RELAUNCH_BY_SETTING_CHANGE, false)
        set(value) {
            preferences.edit().putBoolean(RELAUNCH_BY_SETTING_CHANGE, value).commit()
        }

    override var marketsTabEnabled: Boolean
        get() = preferences.getBoolean(MARKETS_TAB_ENABLED, true)
        set(value) {
            preferences.edit().putBoolean(MARKETS_TAB_ENABLED, value).commit()
            _marketsTabEnabledFlow.update {
                value
            }
        }

    override var personalSupportEnabled: Boolean
        get() = preferences.getBoolean(PERSONAL_SUPPORT_ENABLED, false)
        set(enabled) {
            preferences.edit().putBoolean(PERSONAL_SUPPORT_ENABLED, enabled).apply()
        }

    override var hideSuspiciousTransactions: Boolean
        get() = preferences.getBoolean(HIDE_SUSPICIOUS_TX, true)
        set(value) {
            preferences.edit().putBoolean(HIDE_SUSPICIOUS_TX, value).apply()
        }

    override var pinRandomized: Boolean
        get() = preferences.getBoolean(PIN_RANDOMIZED, false)
        set(value) {
            preferences.edit().putBoolean(PIN_RANDOMIZED, value).apply()
        }

    private val _marketsTabEnabledFlow = MutableStateFlow(marketsTabEnabled)
    override val marketsTabEnabledFlow = _marketsTabEnabledFlow.asStateFlow()

    override var nonRecommendedAccountAlertDismissedAccounts: Set<String>
        get() = preferences.getStringSet(NON_RECOMMENDED_ACCOUNT_ALERT_DISMISSED_ACCOUNTS, setOf()) ?: setOf()
        set(value) {
            preferences.edit().putStringSet(NON_RECOMMENDED_ACCOUNT_ALERT_DISMISSED_ACCOUNTS, value).apply()
        }

    override fun getSwapProviderId(blockchainType: BlockchainType): String? {
        return preferences.getString(getSwapProviderKey(blockchainType), null)
    }

    override fun setSwapProviderId(blockchainType: BlockchainType, providerId: String) {
        preferences.edit().putString(getSwapProviderKey(blockchainType), providerId).apply()
    }

    override var autoLockInterval: AutoLockInterval
        get() = preferences.getString(APP_AUTO_LOCK_INTERVAL, null)?.let {
            AutoLockInterval.fromRaw(it)
        } ?: AutoLockInterval.AFTER_1_MIN
        set(value) {
            preferences.edit().putString(APP_AUTO_LOCK_INTERVAL, value.raw).apply()
        }

    private fun getSwapProviderKey(blockchainType: BlockchainType): String {
        return SWAP_PROVIDER + blockchainType.uid
    }

}
