package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.core.managers.NoActiveAccount
import io.horizontalsystems.walletkit.core.supports
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

object SwapHelper {

    // False when the active account can't hold the token (e.g. TRX for a Monero-only
    // account) — such a swap is deliverable only to an external recipient address
    fun isTokenReceivableByAccount(token: Token): Boolean {
        val accountType = App.accountManager.activeAccount?.type ?: return true
        return token.supports(accountType) && token.blockchainType.supports(accountType)
    }

    fun getSendingAddressForToken(token: Token): String? {
        val blockchainType = token.blockchainType

        if (blockchainType.isEvm
            || blockchainType == BlockchainType.Solana
            || blockchainType == BlockchainType.Tron
            || blockchainType == BlockchainType.Ton
            || blockchainType == BlockchainType.Stellar
        ) {
            App.adapterManager.getAdapterForToken<IReceiveAdapter>(token)?.let {
                return it.receiveAddress
            }
        }

        return null
    }

    suspend fun getSourceAddressesForAmlCheck(token: Token, amountIn: BigDecimal): List<String> {
        val blockchainType = token.blockchainType

        if (blockchainType.isEvm
            || blockchainType == BlockchainType.Solana
            || blockchainType == BlockchainType.Tron
            || blockchainType == BlockchainType.Ton
            || blockchainType == BlockchainType.Stellar
            || blockchainType == BlockchainType.Zcash
            || blockchainType == BlockchainType.Monero
            || blockchainType == BlockchainType.Zano
        ) {
            App.adapterManager.getAdapterForToken<IReceiveAdapter>(token)?.let {
                return listOf(it.receiveAddress)
            }
        }

        // UTXO chains: the chain plugin selects the UTXOs that will actually cover amountIn
        return ChainRegistry[token.blockchainType]?.swapSourceAddresses(token, amountIn).orEmpty()

    }

    suspend fun getReceiveAddressForToken(token: Token): String {
        val blockchainType = token.blockchainType

        App.adapterManager.getAdapterForToken<IReceiveAdapter>(token)?.let {
            // chains that distinguish address types (Zcash) expose a transparent variant
            return it.receiveAddressTransparent ?: it.receiveAddress
        }

        val accountManager = App.accountManager

        val account = accountManager.activeAccount ?: throw NoActiveAccount()

        return ChainRegistry[blockchainType]?.swapDestinationAddress(account, token)
            ?: throw SwapError.NoDestinationAddress()
    }




}