package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.core.App
import cash.p.terminal.core.adapters.BitcoinAdapter
import cash.p.terminal.core.adapters.BitcoinCashAdapter
import cash.p.terminal.core.adapters.LitecoinAdapter
import cash.p.terminal.core.adapters.Trc20Adapter
import cash.p.terminal.core.isEvm
import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.core.managers.StellarKitManager
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.modules.multiswap.action.ActionApprove
import cash.p.terminal.modules.multiswap.action.ActionRevoke
import cash.p.terminal.modules.multiswap.action.ISwapProviderAction
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.wallet.NoActiveAccount
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.isLitecoinMweb
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.horizontalsystems.core.entities.BlockchainType
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal

object SwapHelper {

    suspend fun getAllowanceTrc20(token: Token, spenderAddress: String): BigDecimal? {
        if (token.type !is TokenType.Eip20) return null

        val trc20Adapter = App.adapterManager.getAdapterForToken<Trc20Adapter>(token) ?: return null
        return trc20Adapter.allowance(spenderAddress)
    }

    suspend fun getReceiveAddressForToken(token: Token): String {
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
                val evmAddress =
                    account.type.evmAddress(chain) ?: throw SwapError.NoDestinationAddress()
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
                    if (token.isLitecoinMweb) {
                        throw SwapError.NoDestinationAddress()
                    }
                    LitecoinAdapter.firstAddress(account.type, token.type)
                }

                BlockchainType.Dash -> {
                    val walletUseCase: WalletUseCase by inject(WalletUseCase::class.java)
                    walletUseCase.getReceiveAddress(token)
                }

                BlockchainType.Tron -> {
                    App.tronKitManager.getTronKitWrapper(account).tronKit.address.base58
                }

                BlockchainType.Stellar -> {
                    val stellarKitManager: StellarKitManager by inject(StellarKitManager::class.java)
                    stellarKitManager.getAddress(account)
                }

                BlockchainType.Solana -> {
                    val solanaKitManager: SolanaKitManager by inject(SolanaKitManager::class.java)
                    solanaKitManager.getSolanaKitWrapper(account).solanaKit.receiveAddress
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
            .filter {
                it.transactionRecordType == TransactionRecordType.TRON_APPROVE && it.spender.equals(
                    routerAddress,
                    true
                )
            }
            .maxByOrNull { it.timestamp }

        val revoke = allowance > BigDecimal.ZERO && isUsdt(token)

        return if (revoke) {
            val revokeInProgress = approveTransaction != null && approveTransaction.value?.zeroValue == true
            ActionRevoke(
                token,
                routerAddress,
                revokeInProgress,
                allowance
            )
        } else {
            val approveInProgress =
                approveTransaction != null && approveTransaction.value?.zeroValue != true

            ActionApprove(
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
                && tokenType.address.equals("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t", ignoreCase = true)
    }

}
