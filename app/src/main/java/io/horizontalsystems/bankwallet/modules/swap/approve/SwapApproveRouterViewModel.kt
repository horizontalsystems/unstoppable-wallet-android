package io.horizontalsystems.bankwallet.modules.swap.approve

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService.ApproveData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal

class SwapApproveRouterViewModel(private val arguments: Bundle?) : ViewModel() {

    fun getPage(): Page {
        val approveData = arguments?.getParcelable<ApproveData>(SwapApproveModule.dataKey)
        return when {
            approveData == null -> Page.NoArguments
            approveData.allowance.compareTo(BigDecimal.ZERO) != 0 && isUsdt(approveData.token) -> {
                Page.RevokeAndApprove(approveData)
            }
            else -> Page.Approve(approveData)
        }
    }

    private fun isUsdt(token: Token): Boolean {
        val tokenType = token.type

        return token.blockchainType is BlockchainType.Ethereum
            && tokenType is TokenType.Eip20
            && tokenType.address.lowercase() == "0xdac17f958d2ee523a2206206994597c13d831ec7"
    }

    sealed class Page {
        object NoArguments : Page()
        class RevokeAndApprove(val approveData: ApproveData) : Page()
        class Approve(val approveData: ApproveData) : Page()
    }

    class Factory(private val arguments: Bundle?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapApproveRouterViewModel(arguments) as T
        }
    }
}
