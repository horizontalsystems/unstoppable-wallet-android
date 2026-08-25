package io.horizontalsystems.walletkit.core.factories

import android.content.Context
import android.util.Log
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmLabelManager
import io.horizontalsystems.walletkit.core.managers.EvmSyncSourceManager
import io.horizontalsystems.walletkit.core.managers.RestoreSettingsManager
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.TokenType

class AdapterFactory(
    private val context: Context,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val backgroundManager: BackgroundManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val coinManager: ICoinManager,
    private val evmLabelManager: EvmLabelManager,
    private val localStorage: ILocalStorage,
) {

    fun getAdapterOrNull(wallet: Wallet) = try {
        getAdapter(wallet)
    } catch (e: Throwable) {
        Log.e("AAA", "get adapter error", e)
        null
    }

    private fun getAdapter(wallet: Wallet) = when (val tokenType = wallet.token.type) {
        is TokenType.Derived -> registryAdapter(wallet)
        is TokenType.AddressTyped -> registryAdapter(wallet)
        TokenType.Native -> registryAdapter(wallet)
        is TokenType.Eip20 -> registryAdapter(wallet)
        is TokenType.Spl -> registryAdapter(wallet)
        is TokenType.Jetton -> registryAdapter(wallet)
        is TokenType.Asset -> registryAdapter(wallet)
        is TokenType.ThorchainAsset -> registryAdapter(wallet)
        is TokenType.ZanoAsset -> registryAdapter(wallet)
        is TokenType.Unsupported -> null
    }

    private fun registryAdapter(wallet: Wallet): IAdapter? =
        ChainRegistry[wallet.token.blockchainType]?.createAdapter(
            wallet,
            restoreSettingsManager.settings(wallet.account, wallet.token.blockchainType),
        )

    fun unlinkAdapter(wallet: Wallet) {
        ChainRegistry[wallet.transactionSource.blockchain.type]?.unlink(wallet.account)
    }

    fun unlinkAdapter(transactionSource: TransactionSource) {
        ChainRegistry[transactionSource.blockchain.type]?.unlink(transactionSource.account)
    }
}
