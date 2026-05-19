package cash.p.terminal.core.managers

import cash.p.terminal.core.adapters.BitcoinAdapter
import cash.p.terminal.core.adapters.BitcoinCashAdapter
import cash.p.terminal.core.adapters.DashAdapter
import cash.p.terminal.core.adapters.ECashAdapter
import cash.p.terminal.core.adapters.Eip20Adapter
import cash.p.terminal.core.adapters.EvmAdapter
import cash.p.terminal.core.adapters.LitecoinAdapter
import cash.p.terminal.core.adapters.SolanaAdapter
import cash.p.terminal.core.adapters.TronAdapter
import cash.p.terminal.core.storage.MoneroFileDao
import cash.p.terminal.domain.usecase.ClearZCashWalletDataUseCase
import cash.p.terminal.modules.pin.core.PinDbStorage
import cash.p.terminal.wallet.IAccountCleaner
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.isLitecoinMweb
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.useCases.RemoveMoneroWalletFilesUseCase
import io.horizontalsystems.core.ISmsNotificationSettings
import io.horizontalsystems.core.entities.BlockchainType

class AccountCleaner(
    private val clearZCashWalletDataUseCase: ClearZCashWalletDataUseCase,
    private val removeMoneroWalletFilesUseCase: RemoveMoneroWalletFilesUseCase,
    private val accountManager: IAccountManager,
    private val adapterManager: IAdapterManager,
    private val walletManager: IWalletManager,
    private val moneroFileDao: MoneroFileDao,
    private val smsNotificationSettings: ISmsNotificationSettings,
    private val pinDbStorage: PinDbStorage
) : IAccountCleaner {

    override suspend fun clearAccounts(accountIds: List<String>) {
        accountIds.forEach { clearAccount(it) }
    }


    private suspend fun clearAccount(accountId: String) {
        BitcoinAdapter.clear(accountId)
        BitcoinCashAdapter.clear(accountId)
        ECashAdapter.clear(accountId)
        LitecoinAdapter.clear(accountId)
        LitecoinAdapter.clearMweb(accountId)
        DashAdapter.clear(accountId)
        EvmAdapter.clear(accountId)
        Eip20Adapter.clear(accountId)
        clearWalletDataForBlockchainIfInactive(accountId, BlockchainType.Monero)
        clearWalletDataForBlockchainIfInactive(accountId, BlockchainType.Zcash)
        SolanaAdapter.clear(accountId)
        TronAdapter.clear(accountId)
        clearSmsNotificationSettings(accountId)
    }

    private fun clearSmsNotificationSettings(accountId: String) {
        pinDbStorage.getAllLevels().forEach { level ->
            if (smsNotificationSettings.getSmsNotificationAccountId(level) == accountId) {
                smsNotificationSettings.setSmsNotificationAccountId(level, null)
                smsNotificationSettings.setSmsNotificationAddress(level, null)
                smsNotificationSettings.setSmsNotificationMemo(level, null)
            }
        }
    }

    /**
     * Clear wallet data for tokens where birthday height can change.
     */
    override suspend fun clearWalletForAccount(accountId: String, token: Token) {
        if (token.isLitecoinMweb) {
            resetLitecoinMwebRestoreData(accountId)
            return
        }

        if (isWalletActive(accountId, token)) return
        clearWalletData(accountId, token.blockchainType)
    }

    /**
     * Called after an accepted MWEB birthday-height change. Active Litecoin
     * adapters are stopped first because public-send daemon ownership can live
     * in either public LTC or MWEB adapter instances.
     */
    private suspend fun resetLitecoinMwebRestoreData(accountId: String) {
        if (isWalletActive(accountId, BlockchainType.Litecoin)) {
            adapterManager.stopAdapters(listOf(accountId), BlockchainType.Litecoin)
        }
        LitecoinAdapter.clearMweb(accountId)
    }

    private suspend fun clearWalletDataForBlockchainIfInactive(
        accountId: String,
        blockchainType: BlockchainType
    ) {
        if (isWalletActive(accountId, blockchainType)) return
        clearWalletData(accountId, blockchainType)
    }

    private suspend fun clearWalletData(
        accountId: String,
        blockchainType: BlockchainType
    ) {
        when (blockchainType) {
            is BlockchainType.Zcash -> clearZCashWalletDataUseCase.invoke(accountId)
            is BlockchainType.Monero -> {
                accountManager.account(accountId)?.let {
                    removeMoneroWalletFilesUseCase(it)
                    moneroFileDao.deleteAssociatedRecord(it.id)
                }
            }

            else -> Unit
        }
    }

    private fun isWalletActive(accountId: String, token: Token): Boolean {
        return isWalletActive(accountId, token.blockchainType, token.type)
    }

    private fun isWalletActive(
        accountId: String,
        blockchainType: BlockchainType,
        tokenType: TokenType? = null
    ): Boolean {
        return walletManager.activeWallets.any {
            it.account.id == accountId &&
                it.token.blockchainType == blockchainType &&
                (tokenType == null || it.token.type == tokenType)
        }
    }

    override suspend fun clearWalletForCurrentAccount(token: Token) {
        accountManager.activeAccount?.let {
            clearWalletForAccount(it.id, token)
        }
    }
}
