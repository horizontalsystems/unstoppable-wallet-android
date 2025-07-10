package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinCashAdapter
import io.horizontalsystems.bankwallet.core.adapters.LitecoinAdapter
import io.horizontalsystems.bankwallet.core.adapters.Trc20Adapter
import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronApproveTransactionRecord
import io.horizontalsystems.bankwallet.modules.multiswap.action.ActionApprove
import io.horizontalsystems.bankwallet.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal

object SwapHelper {

    suspend fun getAllowanceTrc20(token: Token, spenderAddress: String): BigDecimal? {
        if (token.type !is TokenType.Eip20) return null

        val trc20Adapter = App.adapterManager.getAdapterForToken<Trc20Adapter>(token) ?: return null
        return trc20Adapter.allowance(spenderAddress)
    }

    fun getReceiveAddressForToken(token: Token): String {
        val blockchainType = token.blockchainType

        App.adapterManager.getAdapterForToken<IReceiveAdapter>(token)?.let {
            return it.receiveAddress
        }

        val accountManager = App.accountManager
        val evmBlockchainManager = App.evmBlockchainManager

        val account = accountManager.activeAccount ?: throw NoActiveAccount()

        when (blockchainType) {
            BlockchainType.Avalanche,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Ethereum -> {
                val chain = evmBlockchainManager.getChain(blockchainType)
                val evmAddress = account.type.evmAddress(chain) ?: throw SwapError.NoDestinationAddress()
                return evmAddress.eip55
            }
            BlockchainType.Bitcoin -> {
                return BitcoinAdapter.firstAddress(account.type, token.type)
            }
            BlockchainType.BitcoinCash -> {
                return BitcoinCashAdapter.firstAddress(account.type, token.type)
            }
            BlockchainType.Litecoin -> {
                return LitecoinAdapter.firstAddress(account.type, token.type)
            }
            BlockchainType.Tron -> {
                return App.tronKitManager.getAddress(account.type)
            }
            BlockchainType.Stellar -> {
                return App.stellarKitManager.getAddress(account.type)
            }
            else -> throw SwapError.NoDestinationAddress()
        }
    }

    suspend fun actionApproveTrc20(
        allowance: BigDecimal?,
        amountIn: BigDecimal,
        routerAddress: String,
        token: Token,
    ): ISwapProviderAction? {
        if (allowance == null || allowance >= amountIn) return null
        val trc20Adapter = App.adapterManager.getAdapterForToken<Trc20Adapter>(token) ?: return null

        val approveTransaction = trc20Adapter.getPendingTransactions()
            .filterIsInstance<TronApproveTransactionRecord>()
            .filter { it.spender.equals(routerAddress, true) }
            .maxByOrNull { it.timestamp }

        val approveInProgress = approveTransaction != null && !approveTransaction.value.zeroValue

        return ActionApprove(
            amountIn,
            routerAddress,
            token,
            approveInProgress
        )
    }
}