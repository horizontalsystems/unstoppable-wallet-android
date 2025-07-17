package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ApproveTransactionRecord
import io.horizontalsystems.bankwallet.modules.multiswap.action.ActionApprove
import io.horizontalsystems.bankwallet.modules.multiswap.action.ActionRevoke
import io.horizontalsystems.bankwallet.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

object EvmSwapHelper {
    suspend fun getAllowance(token: Token, spenderAddressStr: String): BigDecimal? {
        val spenderAddress = getEvmAddress(spenderAddressStr) ?:  return null
        return getAllowance(token, spenderAddress)
    }

    private fun getEvmAddress(spenderAddress: String) = try {
        Address(spenderAddress)
    } catch (_: Throwable) {
        null
    }

    suspend fun getAllowance(token: Token, spenderAddress: Address): BigDecimal? {
        if (token.type !is TokenType.Eip20) return null

        val eip20Adapter = App.adapterManager.getAdapterForToken<Eip20Adapter>(token) ?: return null

        return eip20Adapter.allowance(spenderAddress, DefaultBlockParameter.Latest).await()
    }

    fun actionApprove(
        allowance: BigDecimal?,
        amountIn: BigDecimal,
        routerAddressStr: String,
        token: Token,
    ): ISwapProviderAction? {
        val routerAddress = getEvmAddress(routerAddressStr) ?:  return null
        return actionApprove(allowance, amountIn, routerAddress, token)
    }

    fun actionApprove(
        allowance: BigDecimal?,
        amountIn: BigDecimal,
        routerAddress: Address,
        token: Token,
    ): ISwapProviderAction? {
        if (allowance == null || allowance >= amountIn) return null
        val eip20Adapter = App.adapterManager.getAdapterForToken<Eip20Adapter>(token) ?: return null

        val approveTransaction = eip20Adapter.pendingTransactions
            .filterIsInstance<ApproveTransactionRecord>()
            .filter { it.spender.equals(routerAddress.eip55, true) }
            .maxByOrNull { it.timestamp }

        val revoke = allowance > BigDecimal.ZERO && isUsdt(token)

        return if (revoke) {
            val revokeInProgress = approveTransaction != null && approveTransaction.value.zeroValue
            ActionRevoke(
                token,
                routerAddress.eip55,
                revokeInProgress,
                allowance
            )
        } else {
            val approveInProgress = approveTransaction != null && !approveTransaction.value.zeroValue
            ActionApprove(
                amountIn,
                routerAddress.eip55,
                token,
                approveInProgress
            )
        }
    }

    private fun isUsdt(token: Token): Boolean {
        val tokenType = token.type

        return token.blockchainType is BlockchainType.Ethereum
            && tokenType is TokenType.Eip20
            && tokenType.address.lowercase() == "0xdac17f958d2ee523a2206206994597c13d831ec7"
    }

    fun getAllBridgeProxyFee(proxyAddress: String, amountIn: BigDecimal): BigDecimal {
        // need to fetch it from contract
        val feeBP = 100
        val feeMultiplier = feeBP.toBigDecimal().movePointLeft(4).stripTrailingZeros()
        return amountIn * feeMultiplier
    }

}
