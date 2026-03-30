package cash.p.terminal.core

import cash.p.terminal.entities.AppVersion
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.entities.SyncMode
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.modules.displayoptions.DisplayDiffOptionType
import cash.p.terminal.modules.displayoptions.DisplayPricePeriod
import cash.p.terminal.modules.balance.token.addresspoisoning.AddressPoisoningViewMode
import cash.p.terminal.modules.main.MainModule
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.favorites.WatchlistSorting
import cash.p.terminal.modules.settings.appearance.AppIcon
import cash.p.terminal.modules.settings.appearance.PriceChangeInterval
import cash.p.terminal.modules.settings.security.autolock.AutoLockInterval
import cash.p.terminal.modules.theme.ThemeType
import cash.p.terminal.wallet.BalanceSortType
import cash.p.terminal.wallet.Derivation
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.wallet.entities.EncryptedString
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import io.horizontalsystems.core.ILoggingSettings
import io.horizontalsystems.core.ISmsNotificationSettings
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

interface ILocalStorage : ILoggingSettings, ISmsNotificationSettings {
    var marketSearchRecentCoinUids: List<String>
    var zcashAccountIds: Set<String>
    var autoLockInterval: AutoLockInterval

    var recipientAddressBaseCheckEnabled: Boolean
    var recipientAddressContractCheckEnabled: Boolean

    var chartIndicatorsEnabled: Boolean
    var amountInputType: AmountInputType?
    var baseCurrencyCode: String?
    var authToken: String?
    val appId: String?

    var baseBitcoinProvider: String?
    var baseLitecoinProvider: String?
    var baseDogecoinProvider: String?
    var baseEthereumProvider: String?
    var baseDashProvider: String?
    var baseBinanceProvider: String?
    var baseZcashProvider: String?
    var syncMode: SyncMode?
    var sortType: BalanceSortType
    var appVersions: List<AppVersion>
    var isAlertNotificationOn: Boolean
    var encryptedSampleText: String?
    var bitcoinDerivation: Derivation?
    var torEnabled: Boolean
    var appLaunchCount: Int
    var rateAppLastRequestTime: Long
    var balanceHidden: Boolean
    var balanceAutoHideEnabled: Boolean
    var swapMevProtectionEnabled: Boolean

    var transactionHideEnabled: Boolean
    var transactionDisplayLevel: TransactionDisplayLevel
    var transactionHideSecretPin: EncryptedString?

    var transferPasscodeEnabled: Boolean

    var balanceTotalCoinUid: String?
    var termsAccepted: Boolean
    var mainShowedOnce: Boolean
    var notificationId: String?
    var notificationServerTime: Long
    var currentTheme: ThemeType
    var balanceViewType: BalanceViewType?
    var changelogShownForAppVersion: String?
    var ignoreRootedDeviceWarning: Boolean
    var launchPage: LaunchPage?
    var appIcon: AppIcon?
    val appIconRaw: String?
    var mainTab: MainModule.MainNavigation?
    var marketFavoritesSorting: WatchlistSorting?
    var marketFavoritesShowSignals: Boolean
    var marketFavoritesManualSortingOrder: List<String>
    var marketFavoritesPeriod: TimeDuration?
    var relaunchBySettingChange: Boolean
    var marketsTabEnabled: Boolean
    val marketsTabEnabledFlow: StateFlow<Boolean>
    var balanceTabButtonsEnabled: Boolean
    val balanceTabButtonsEnabledFlow: StateFlow<Boolean>
    var nonRecommendedAccountAlertDismissedAccounts: Set<String>
    var personalSupportEnabled: Boolean
    var hideSuspiciousTransactions: Boolean
    var pinRandomized: Boolean
    var utxoExpertModeEnabled: Boolean
    var rbfEnabled: Boolean
    var statsLastSyncTime: Long
    var shareCrashDataEnabled: Boolean
    var showChangelogAfterUpdate: Boolean
    var selectBalanceTabOnNextLaunch: Boolean

    var customDashPeers: String

    val utxoExpertModeEnabledFlow: StateFlow<Boolean>

    var priceChangeInterval: PriceChangeInterval
    val priceChangeIntervalFlow: StateFlow<PriceChangeInterval>
    var isRoundingAmountMainPage: Boolean

    // Display Options
    var displayDiffPricePeriod: DisplayPricePeriod
    val displayDiffPricePeriodFlow: StateFlow<DisplayPricePeriod>
    var displayDiffOptionType: DisplayDiffOptionType
    val displayDiffOptionTypeFlow: StateFlow<DisplayDiffOptionType>

    var addressPoisoningViewMode: AddressPoisoningViewMode

    var passphraseTermsAgreed: Boolean
    var safetyRulesAgreed: Boolean

    fun getStackingUpdateTimestamp(wallet: Wallet): Long
    fun setStackingUnpaid(wallet: Wallet, unpaid: BigDecimal)
    fun getStackingUnpaid(wallet: Wallet): BigDecimal?

    var isSystemPinRequired: Boolean
    // Login Logging properties inherited from ILoggingSettings

    var moneroSkipNewAddressConfirm: Boolean

    fun clear()
}
