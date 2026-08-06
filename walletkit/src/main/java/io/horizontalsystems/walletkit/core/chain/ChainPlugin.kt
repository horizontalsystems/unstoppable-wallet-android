package io.horizontalsystems.walletkit.core.chain

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.modules.addtoken.AddTokenModule
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
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

    /** Refreshes the chain's kit on user-initiated refresh, if one is running. */
    suspend fun refreshKit() = Unit

    /** The chain's network/node settings page, opened from sync errors and settings. */
    fun networkSettingsPage(): HSPage? = null

    /** The chain's send screen. Default renders nothing (matches the previous silent else). */
    @Composable
    fun SendScreen(args: ChainSendScreenArgs) = Unit

    /** Row shown in Settings > Blockchain Settings, or null to omit the chain there. */
    fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem.Chain? = null

    /**
     * Derives the account's receive address for swap destinations without requiring an
     * enabled wallet. Null means the address cannot be derived for this account type.
     */
    suspend fun swapDestinationAddress(account: Account): String? = null

    /** Startup work after core managers are ready (e.g. Monero fastest-node auto-select). */
    suspend fun onAppStart() = Unit

    /** Deletes the chain's locally stored data for a removed account. */
    fun clearAccountData(accountId: String) = Unit

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

    /** WalletConnect handlers to register at app start. */
    fun wcHandlers(): List<IWCHandler> = emptyList()

    /** Swap providers contributed to the multiswap registry. */
    fun swapProviders(): List<IMultiSwapProvider> = emptyList()

    /** Account types restorable from a pasted private key. */
    fun privateKeyAccountTypes(privateKey: String): List<AccountType> = emptyList()

    /** Send-transaction service used by multiswap/OpenCryptoPay confirm flows. */
    fun sendTransactionService(token: Token): AbstractSendTransactionService? = null
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
