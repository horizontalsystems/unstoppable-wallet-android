package cash.p.terminal.core

import com.google.gson.JsonObject
import cash.p.terminal.core.adapters.BitcoinFeeInfo
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.core.providers.FeeRates
import cash.p.terminal.core.utils.AddressUriResult
import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.RestoreSettingRecord
import cash.p.terminal.entities.TransactionDataSortMode
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.market.MarketModule
import cash.p.terminal.modules.settings.security.tor.TorStatus
import cash.p.terminal.modules.settings.terms.TermsModule
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.CexType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.TokenQuery
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import io.horizontalsystems.core.logger.AppLogger
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
    fun getNextHardwareAccountName(): String
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

interface ITransactionsAdapter {
    val explorerTitle: String
    val transactionsState: AdapterState
    val transactionsStateUpdatedFlowable: Flowable<Unit>

    val lastBlockInfo: LastBlockInfo?
    val lastBlockUpdatedFlowable: Flowable<Unit>
    val additionalTokenQueries: List<TokenQuery> get() = listOf()

    suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord>

    fun getRawTransaction(transactionHash: String): String? = null

    fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?
    ): Flow<List<TransactionRecord>>

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
    suspend fun send(
        amount: BigDecimal,
        address: String,
        memo: String?,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        transactionSorting: TransactionDataSortMode?,
        rbfEnabled: Boolean,
        logger: AppLogger
    ): String
}

internal interface ISendEthereumAdapter {
    val evmTransactionRepository: EvmTransactionRepository
    val balanceData: BalanceData

    fun getTransactionData(amount: BigDecimal, address: Address): TransactionData
}

interface ISendZcashAdapter {
    val availableBalance: BigDecimal
    val fee: StateFlow<BigDecimal>

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