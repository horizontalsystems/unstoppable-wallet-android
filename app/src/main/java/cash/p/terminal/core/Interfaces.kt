package cash.p.terminal.core

import com.google.gson.JsonObject
import cash.p.terminal.core.adapters.BitcoinFeeInfo
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.core.managers.Bep2TokenInfoService
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.core.providers.FeeRates
import cash.p.terminal.core.utils.AddressUriResult
import cash.p.terminal.entities.AppVersion
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.entities.RestoreSettingRecord
import cash.p.terminal.entities.SyncMode
import cash.p.terminal.entities.TransactionDataSortMode
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.wallet.BalanceSortType
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.modules.main.MainModule
import cash.p.terminal.modules.market.MarketModule
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.favorites.WatchlistSorting
import cash.p.terminal.modules.settings.appearance.AppIcon
import cash.p.terminal.modules.settings.appearance.PriceChangeInterval
import cash.p.terminal.modules.settings.security.autolock.AutoLockInterval
import cash.p.terminal.modules.settings.security.tor.TorStatus
import cash.p.terminal.modules.settings.terms.TermsModule
import cash.p.terminal.modules.theme.ThemeType
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.CexType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.EncryptedString
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import io.horizontalsystems.solanakit.models.FullTransaction
import io.horizontalsystems.tonkit.FriendlyAddress
import io.horizontalsystems.tronkit.transaction.Fee
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import io.horizontalsystems.solanakit.models.Address as SolanaAddress
import io.horizontalsystems.tronkit.models.Address as TronAddress

interface ILocalStorage {
    var marketSearchRecentCoinUids: List<String>
    var zcashAccountIds: Set<String>
    var autoLockInterval: AutoLockInterval
    var chartIndicatorsEnabled: Boolean
    var amountInputType: AmountInputType?
    var baseCurrencyCode: String?
    var authToken: String?
    val appId: String?

    var baseBitcoinProvider: String?
    var baseLitecoinProvider: String?
    var baseEthereumProvider: String?
    var baseDashProvider: String?
    var baseBinanceProvider: String?
    var baseZcashProvider: String?
    var syncMode: SyncMode?
    var sortType: BalanceSortType
    var appVersions: List<AppVersion>
    var isAlertNotificationOn: Boolean
    var encryptedSampleText: String?
    var bitcoinDerivation: AccountType.Derivation?
    var torEnabled: Boolean
    var appLaunchCount: Int
    var rateAppLastRequestTime: Long
    var balanceHidden: Boolean
    var balanceAutoHideEnabled: Boolean

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
    var uiStatsEnabled: Boolean?

    val utxoExpertModeEnabledFlow: StateFlow<Boolean>

    var priceChangeInterval: PriceChangeInterval
    val priceChangeIntervalFlow: StateFlow<PriceChangeInterval>

    fun getStackingUpdateTimestamp(wallet: Wallet): Long
    fun setStackingUnpaid(wallet: Wallet, unpaid: BigDecimal)
    fun getStackingUnpaid(wallet: Wallet): BigDecimal?

    fun clear()
}

interface IRestoreSettingsStorage {
    fun restoreSettings(accountId: String, blockchainTypeUid: String): List<RestoreSettingRecord>
    fun restoreSettings(accountId: String): List<RestoreSettingRecord>
    fun save(restoreSettingRecords: List<RestoreSettingRecord>)
    fun deleteAllRestoreSettings(accountId: String)
}

interface IMarketStorage {
    var currentMarketTab: MarketModule.Tab?
}

interface IBackupManager {
    val allBackedUp: Boolean
    val allBackedUpFlowable: Flowable<Boolean>
}

interface IAccountFactory {
    fun account(
        name: String,
        type: AccountType,
        origin: AccountOrigin,
        backedUp: Boolean,
        fileBackedUp: Boolean
    ): Account
    fun watchAccount(name: String, type: AccountType): Account
    fun getNextWatchAccountName(): String
    fun getNextAccountName(): String
    fun getNextCexAccountName(cexType: CexType): String
}

interface IRandomProvider {
    fun getRandomNumbers(count: Int, maxIndex: Int): List<Int>
}

interface INetworkManager {
    suspend fun getMarkdown(host: String, path: String): String
    suspend fun getReleaseNotes(host: String, path: String): JsonObject
    fun getTransaction(host: String, path: String, isSafeCall: Boolean): Flowable<JsonObject>
    fun getTransactionWithPost(
        host: String,
        path: String,
        body: Map<String, Any>
    ): Flowable<JsonObject>

    fun ping(host: String, url: String, isSafeCall: Boolean): Flowable<Any>
    fun getEvmInfo(host: String, path: String): Single<JsonObject>
    suspend fun getBep2Tokens(): List<Bep2TokenInfoService.Bep2Token>
}

interface IClipboardManager {
    fun copyText(text: String)
    fun getCopiedText(): String
    val hasPrimaryClip: Boolean
}

interface IWordsManager {
    fun validateChecksum(words: List<String>)
    fun validateChecksumStrict(words: List<String>)
    fun isWordValid(word: String): Boolean
    fun isWordPartiallyValid(word: String): Boolean
    fun generateWords(count: Int = 12): List<String>
}

interface IBinanceKitManager {
    val binanceKit: BinanceChainKit?
    val statusInfo: Map<String, Any>?

    fun binanceKit(wallet: Wallet): BinanceChainKit
    fun unlink(account: Account)
}

interface ITransactionsAdapter {
    val explorerTitle: String
    val transactionsState: AdapterState
    val transactionsStateUpdatedFlowable: Flowable<Unit>

    val lastBlockInfo: LastBlockInfo?
    val lastBlockUpdatedFlowable: Flowable<Unit>
    val additionalTokenQueries: List<TokenQuery> get() = listOf()

    fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): Single<List<TransactionRecord>>

    fun getRawTransaction(transactionHash: String): String? = null

    fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?
    ): Flowable<List<TransactionRecord>>

    fun getTransactionUrl(transactionHash: String): String
}

class UnsupportedFilterException : Exception()

interface ISendBitcoinAdapter {
    val unspentOutputs: List<UnspentOutputInfo>
    val balanceData: BalanceData
    val blockchainType: BlockchainType
    fun availableBalance(
        feeRate: Int,
        address: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal

    fun minimumSendAmount(address: String?): BigDecimal?
    fun bitcoinFeeInfo(
        amount: BigDecimal,
        feeRate: Int,
        address: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?
    ): BitcoinFeeInfo?

    fun validate(address: String, pluginData: Map<Byte, IPluginData>?)
    fun send(
        amount: BigDecimal,
        address: String,
        memo: String?,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        transactionSorting: TransactionDataSortMode?,
        rbfEnabled: Boolean,
        logger: AppLogger
    ): Single<String>
}

interface ISendEthereumAdapter {
    val evmKitWrapper: EvmKitWrapper
    val balanceData: BalanceData

    fun getTransactionData(amount: BigDecimal, address: Address): TransactionData
}

interface ISendBinanceAdapter {
    val availableBalance: BigDecimal
    val availableBinanceBalance: BigDecimal
    val fee: BigDecimal

    fun validate(address: String)
    fun send(amount: BigDecimal, address: String, memo: String?, logger: AppLogger): Single<String>
}

interface ISendZcashAdapter {
    val availableBalance: BigDecimal
    val fee: BigDecimal

    suspend fun validate(address: String): ZcashAdapter.ZCashAddressType
    suspend fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger): FirstClassByteArray
}

interface ISendSolanaAdapter {
    val availableBalance: BigDecimal
    suspend fun send(amount: BigDecimal, to: SolanaAddress): FullTransaction
}

interface ISendTonAdapter {
    val availableBalance: BigDecimal
    suspend fun send(amount: BigDecimal, address: FriendlyAddress, memo: String?)
    suspend fun estimateFee(amount: BigDecimal, address: FriendlyAddress, memo: String?) : BigDecimal
}

interface ISendTronAdapter {
    val balanceData: BalanceData
    val trxBalanceData: BalanceData

    suspend fun estimateFee(amount: BigDecimal, to: TronAddress): List<Fee>
    suspend fun send(amount: BigDecimal, to: TronAddress, feeLimit: Long?): String
    suspend fun isAddressActive(address: TronAddress): Boolean
    fun isOwnAddress(address: TronAddress): Boolean
}

interface IFeeRateProvider {
    val feeRateChangeable: Boolean get() = false
    suspend fun getFeeRates() : FeeRates
}

interface IAddressParser {
    fun parse(addressUri: String): AddressUriResult
}

interface ITorManager {
    fun start()
    fun stop(): Single<Boolean>
    fun setTorAsEnabled()
    fun setTorAsDisabled()
    val isTorEnabled: Boolean
    val torStatusFlow: StateFlow<TorStatus>
}

interface IRateAppManager {
    val showRateAppFlow: Flow<Boolean>

    fun onBalancePageActive()
    fun onBalancePageInactive()
    fun onAppLaunch()
}

interface ICoinManager {
    fun getToken(query: TokenQuery): Token?
}

interface ITermsManager {
    val termsAcceptedSignalFlow: Flow<Boolean>
    val terms: List<TermsModule.TermType>
    val allTermsAccepted: Boolean
    fun acceptTerms()
}