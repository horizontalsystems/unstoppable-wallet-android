package io.horizontalsystems.walletkit.core.chain

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.modules.addtoken.AddTokenModule
import io.horizontalsystems.walletkit.modules.transactionInfo.options.SpeedUpCancelType
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.walletkit.modules.balance.BalanceModule
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.walletconnect.handler.IWCHandler
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date
import kotlin.reflect.KClass

/**
 * Per-blockchain extension point. Optional chains implement this and register in
 * [ChainRegistry] at app start; shared code resolves chain-specific behavior through the
 * registry instead of hardcoded `when (blockchainType)` branches, so a consuming app that
 * does not include a chain's module simply has no plugin for it — and no trace of the
 * chain in the UI.
 *
 * Hooks are added incrementally as dispatch points are converted; see
 * docs/Walletkit-Modularization-Plan.md.
 */
interface ChainPlugin {
    val blockchainType: BlockchainType

    /**
     * Creates the balance/send adapter for a wallet of this chain. [restoreSettings] holds
     * the chain's stored restore configuration (e.g. birthday height) and is empty for
     * chains without restore settings. Returning null defers creation (the adapter is
     * recreated on the next reloadWallets for this chain).
     */
    fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? = null

    /** Releases kit-manager resources held for [account] when its last wallet is unlinked. */
    fun unlink(account: Account) = Unit

    /** Handlers recognizing this chain's plain address formats. */
    fun addressHandlers(): List<IAddressHandler> = emptyList()

    /** Handlers resolving this chain's naming/alias systems to addresses. */
    fun domainAddressHandlers(): List<IAddressHandler> = emptyList()

    /** Validator used by send/enter-address flows, or null to fall back to core dispatch. */
    fun addressValidator(token: Token): EnterAddressValidator? = null

    /**
     * Validator variant for swap-recipient entry. [allowOwnAddress] permits the wallet's
     * own address; [transparentOnly] restricts to transparent-style addresses where the
     * chain distinguishes them (Zcash). Defaults to the plain validator.
     */
    fun addressValidator(token: Token, allowOwnAddress: Boolean, transparentOnly: Boolean): EnterAddressValidator? =
        addressValidator(token)

    /** Page replacing the default receive flow (e.g. Zcash address-type picker). */
    fun receivePage(wallet: Wallet, entryPoint: KClass<out HSPage>? = null): HSPage? = null

    /** Page for shielding transparent funds, opened from the locked-balance sheet. */
    fun shieldPage(wallet: Wallet, entryPoint: KClass<out HSPage>): HSPage? = null

    /** Threshold above which an unshielded balance triggers the balance-screen alert. */
    fun unshieldedBalanceThreshold(): BigDecimal? = null

    /**
     * Balance the chain requires the user to migrate (e.g. Zcash Orchard→Ironwood after a
     * network upgrade), or null when no migration is pending for [wallet].
     */
    fun migrationRequiredBalance(wallet: Wallet): BigDecimal? = null

    /** Confirmation page executing the pending migration reported by [migrationRequiredBalance]. */
    fun migrationPage(wallet: Wallet, entryPoint: KClass<out HSPage>): HSPage? = null

    /** Lowest accepted birthday height for the restore date picker. */
    fun minBirthdayHeight(): Long? = null

    /** Stops the chain's adapter and clears local data so a rescan starts clean. */
    suspend fun prepareRescan(wallet: Wallet) = Unit

    /** The wallet's unified/shielded receive address for swap delivery, or null. */
    suspend fun swapUnifiedReceiveAddress(token: Token): String? = null

    /** Birthday height stored when a NEW wallet of this chain is created, or null if unused. */
    fun newWalletBirthdayHeight(): Long? = null

    /** Date of the chain's first block, for the restore birthday-height date picker. */
    fun firstBlockDate(): LocalDate? = null

    /** Estimates the date a block at [height] was mined. */
    suspend fun estimateBlockDate(height: Long): Date? = null

    /** Estimates the block height at [date]. */
    suspend fun estimateBlockHeightFromDate(date: Date): Long? = null

    /**
     * Emissions trigger reloadWallets for this chain (e.g. after a node/endpoint change).
     * Collected once by WalletManager.start.
     */
    val walletReloadTrigger: Flow<*>? get() = null

    /** Emissions refresh the blockchain-settings rows; defaults to [walletReloadTrigger]. */
    val settingsRefreshTrigger: Flow<*>? get() = walletReloadTrigger

    /** Kit status details for the App Status debug screen. */
    fun statusInfo(): Map<String, Any>? = null

    /** Chain-specific warning for a wallet's balance row (e.g. inactive Tron account). */
    suspend fun balanceWarning(wallet: Wallet): BalanceModule.BalanceWarning? = null

    /** Refreshes the chain's kit on user-initiated refresh, if one is running. */
    suspend fun refreshKit() = Unit

    /** The chain's network/node settings page, opened from sync errors and settings. */
    fun networkSettingsPage(): HSPage? = null

    /** The chain's send screen. Default renders nothing (matches the previous silent else). */
    @Composable
    fun SendScreen(args: ChainSendScreenArgs) = Unit

    /** Row shown in Settings > Blockchain Settings, or null to omit the chain there. */
    fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem? = null

    /**
     * Derives the account's receive address for swap destinations without requiring an
     * enabled wallet. Null means the address cannot be derived for this account type.
     */
    suspend fun swapDestinationAddress(account: Account): String? = null

    /** Token-aware variant for chains whose first address depends on the token's derivation. */
    suspend fun swapDestinationAddress(account: Account, token: Token): String? = swapDestinationAddress(account)

    /** Startup work after core managers are ready (e.g. Monero fastest-node auto-select). */
    suspend fun onAppStart() = Unit

    /**
     * Work when the app returns to the foreground. Runs on every foreground transition,
     * including the first one after launch, so implementations must be idempotent and cheap
     * when there is nothing to do (e.g. Auto-Select only re-picks a node when one has died).
     */
    suspend fun onEnterForeground() = Unit

    /**
     * Work when this chain's sync has been stalled for a while, foreground or background.
     * Called on a backoff by StalledSyncWatcher, so implementations need not rate-limit
     * themselves, but must tolerate being called while a previous attempt is still running.
     */
    suspend fun onSyncStalled() = Unit

    /**
     * True when an active wallet of this chain has an adapter reporting [AdapterState.NotSynced].
     *
     * Deliberately narrower than "not Synced": Syncing/Connecting are healthy progress, and a
     * chain with no adapter yet (creation deferred, wallet disabled) reports false.
     */
    fun hasUnsyncedWallet(): Boolean =
        App.walletManager.activeWallets
            .filter { it.token.blockchainType == blockchainType }
            .any { App.adapterManager.getBalanceAdapterForWallet(it)?.balanceState is AdapterState.NotSynced }

    /**
     * Deletes the chain's locally stored data for a removed account.
     *
     * Deliberately without a default: this used to be a no-op that plugins inherited silently, and
     * five of them did, so deleting an account left their databases behind. A chain with nothing
     * to delete should say so with an empty body rather than by omission.
     */
    fun clearAccountData(accountId: String)

    /** Dedicated transactions adapter, or null when the balance adapter serves both roles. */
    fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? = null

    /** True when the chain supports adding custom tokens by reference. */
    val supportsCustomTokens: Boolean get() = false

    /** Resolves custom-token info for the add-token flow; null when unsupported. */
    fun addTokenBlockchainService(blockchain: Blockchain): AddTokenModule.IAddTokenBlockchainService? = null

    /** The chain's sync-source/node display name for the settings backup, or null. */
    fun backupSyncSourceName(): String? = null

    /** True when the chain replaces the generic receive screen with [ReceiveScreen]. */
    val hasReceiveScreen: Boolean get() = false

    /** Chain-specific receive screen; only called when [hasReceiveScreen] is true. */
    @Composable
    fun ReceiveScreen(navigation: HSNavigation, wallet: Wallet, receiveEntryPointDestId: KClass<out HSPage>?) = Unit

    /** Extra cells on the token balance screen (e.g. Monero account picker). */
    @Composable
    fun TokenBalanceExtraCells(wallet: Wallet, navigation: HSNavigation) = Unit

    /** Validates a watch-account private view key against an address; throws when invalid. */
    fun validateWatchViewKey(viewKey: String, address: String) {
        throw UnsupportedOperationException("watch accounts not supported")
    }

    /** Word suggestions for the chain's own mnemonic scheme (e.g. Monero 25-word legacy). */
    fun altMnemonicSuggestions(word: String): List<String> = emptyList()

    /** Whether [word] belongs to the chain's own mnemonic word list. */
    fun isAltMnemonicWord(word: String, partial: Boolean): Boolean = false

    /** Word count identifying the chain's own mnemonic scheme, or null if none. */
    val altMnemonicWordCount: Int? get() = null

    /** Builds the account for the chain's own mnemonic scheme; null = invalid checksum. */
    fun buildAltMnemonicAccount(words: List<String>, passphrase: String): AccountType? = null

    /** Rows contributed to the manage-account private keys screen. */
    fun privateKeyRows(account: Account): List<ChainKeyRow> = emptyList()

    /** Rows contributed to the manage-account public keys screen. */
    fun publicKeyRows(account: Account): List<ChainKeyRow> = emptyList()

    /** True when the plugin recognizes this deep link / scanned text as its own. */
    fun isDeepLinkSupported(link: String, fromScanner: Boolean): Boolean = false

    /** Handles a deep link previously accepted by [isDeepLinkSupported]. */
    suspend fun handleDeepLink(link: String, closeApp: Boolean) = Unit

    /** Effects composed on the main screen (e.g. chain-specific request sheets). */
    @Composable
    fun MainScreenEffects(navigation: HSNavigation) = Unit

    /** Renders this chain's WalletConnect request sheet content; false when not handled. */
    @Composable
    fun WcRequestSheetScreen(navigation: HSNavigation): Boolean = false

    /** Chain-specific blacklist checker consulted by the address-check flow, or null. */
    fun blacklistAddressChecker(): io.horizontalsystems.walletkit.core.address.AddressChecker? = null

    /** Page for speeding up / cancelling a pending transaction, or null when unsupported. */
    fun resendTransactionPage(type: SpeedUpCancelType, transactionHash: String): HSPage? = null

    /** Maps a chain-library error to a user-presentable one, or null when not recognized. */
    fun convertError(throwable: Throwable): Throwable? = null

    /** Address reported to market analytics for this chain, or null. */
    fun analyticsAddress(accountType: AccountType): String? = null

    /** Current EIP-20 allowance granted to spender, or null when not applicable. */
    suspend fun eip20Allowance(token: Token, spenderAddress: String): BigDecimal? = null

    /** Approve/revoke action required before swapping, or null when none is needed. */
    suspend fun eip20ApproveAction(
        allowance: BigDecimal?,
        amountIn: BigDecimal,
        spenderAddress: String,
        token: Token,
    ): ISwapProviderAction? = null

    /** Chain-specific OpenCryptoPay confirmation page, or null to use the generic one. */
    fun openCryptoPayConfirmationPage(
        data: io.horizontalsystems.walletkit.modules.opencryptopay.OcpConfirmData,
        sendEntryPointDestId: KClass<out HSPage>,
    ): HSPage? = null

    /** Source addresses whose UTXOs cover amountIn for a swap, or null when not a UTXO chain. */
    suspend fun swapSourceAddresses(token: Token, amountIn: BigDecimal): List<String>? = null

    /** WalletConnect handlers to register at app start. */
    fun wcHandlers(): List<IWCHandler> = emptyList()

    /** Silent handler for wallet_* WalletConnect requests, or null. */
    fun wcWalletRequestHandler(): io.horizontalsystems.walletkit.modules.walletconnect.IWCWalletRequestHandler? = null

    /** Swap providers contributed to the multiswap registry. */
    fun swapProviders(): List<IMultiSwapProvider> = emptyList()

    /** Account types restorable from a pasted private key. */
    fun privateKeyAccountTypes(privateKey: String): List<AccountType> = emptyList()

    /** Send-transaction service used by multiswap/OpenCryptoPay confirm flows. */
    fun sendTransactionService(token: Token): AbstractSendTransactionService? = null

    /**
     * A plain transfer of [amount] to [address] as SendTransactionData, for flows that build a
     * deposit themselves (private send). Most chains are built kit-free by the caller; EVM
     * needs the kit for ERC20 transfer calldata, so its plugin overrides this.
     */
    fun depositTransferData(
        token: Token,
        amount: BigDecimal,
        address: String,
    ): io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData? = null
}

/** A row on the manage-account private/public keys screens, navigating to a chain page. */
class ChainKeyRow(
    val titleRes: Int,
    val descriptionRes: Int,
    val page: HSPage,
    val statPage: io.horizontalsystems.walletkit.core.stats.StatPage,
)

/** Everything SendPage provides to a chain's send screen. */
class ChainSendScreenArgs(
    val wallet: Wallet,
    val title: String,
    val navigation: HSNavigation,
    val amountInputModeViewModel: AmountInputModeViewModel,
    val sendEntryPointDestId: KClass<out HSPage>,
    val address: Address,
    val amount: BigDecimal?,
    val memo: String?,
    val hideAddress: Boolean,
    val riskyAddress: Boolean,
)
