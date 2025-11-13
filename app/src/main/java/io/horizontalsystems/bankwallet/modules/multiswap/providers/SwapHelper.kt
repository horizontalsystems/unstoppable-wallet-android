package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinCashAdapter
import io.horizontalsystems.bankwallet.core.adapters.DashAdapter
import io.horizontalsystems.bankwallet.core.adapters.LitecoinAdapter
import io.horizontalsystems.bankwallet.core.adapters.Trc20Adapter
import io.horizontalsystems.bankwallet.core.isEvm
import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronApproveTransactionRecord
import io.horizontalsystems.bankwallet.modules.multiswap.action.ActionApprove
import io.horizontalsystems.bankwallet.modules.multiswap.action.ActionRevoke
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

    fun getSendingAddressForToken(token: Token): String? {
        val blockchainType = token.blockchainType

        if (blockchainType.isEvm || blockchainType == BlockchainType.Solana || blockchainType == BlockchainType.Tron) {
            App.adapterManager.getAdapterForToken<IReceiveAdapter>(token)?.let {
                return it.receiveAddress
            }
        }

        return null
    }

    fun getReceiveAddressForToken(token: Token): String {
        val blockchainType = token.blockchainType

        App.adapterManager.getAdapterForToken<IReceiveAdapter>(token)?.let {
            return it.receiveAddress
        }

        val accountManager = App.accountManager
        val evmBlockchainManager = App.evmBlockchainManager

        val account = accountManager.activeAccount ?: throw NoActiveAccount()

        return when {
            blockchainType.isEvm -> {
                val chain = evmBlockchainManager.getChain(blockchainType)
                val evmAddress = account.type.evmAddress(chain) ?: throw SwapError.NoDestinationAddress()
                evmAddress.eip55
            }

            else -> when (blockchainType) {
                BlockchainType.Bitcoin -> {
                    BitcoinAdapter.firstAddress(account.type, token.type)
                }

                BlockchainType.BitcoinCash -> {
                    BitcoinCashAdapter.firstAddress(account.type, token.type)
                }

                BlockchainType.Litecoin -> {
                    LitecoinAdapter.firstAddress(account.type, token.type)
                }

                BlockchainType.Dash -> {
                    DashAdapter.firstAddress(account.type)
                }

                BlockchainType.Tron -> {
                    App.tronKitManager.getAddress(account.type)
                }

                BlockchainType.Stellar -> {
                    App.stellarKitManager.getAddress(account.type)
                }

                BlockchainType.Solana -> {
                    App.solanaKitManager.getAddress(account.type)
                }

                else -> throw SwapError.NoDestinationAddress()
            }
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

        val revoke = allowance > BigDecimal.ZERO && isUsdt(token)

        return if (revoke) {
            val revokeInProgress = approveTransaction != null && approveTransaction.value.zeroValue
            ActionRevoke(
                token,
                routerAddress,
                revokeInProgress,
                allowance
            )
        } else {
            val approveInProgress =
                approveTransaction != null && !approveTransaction.value.zeroValue

            return ActionApprove(
                amountIn,
                routerAddress,
                token,
                approveInProgress
            )
        }
    }

    private fun isUsdt(token: Token): Boolean {
        val tokenType = token.type

        return token.blockchainType is BlockchainType.Tron
                && tokenType is TokenType.Eip20
                && tokenType.address.lowercase() == "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t".lowercase()
    }

}