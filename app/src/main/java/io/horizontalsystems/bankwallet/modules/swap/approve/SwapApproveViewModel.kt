package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal

class SwapApproveViewModel(
    val blockchainType: BlockchainType,
    private val service: SwapApproveService,
    private val coinService: EvmCoinService
) : ViewModel() {

    val initialAmount = service.amount?.let {
        coinService.convertToMonetaryValue(it).toPlainString()
    } ?: ""

    var approveAllowed by mutableStateOf(false)
    var amountError by mutableStateOf<Throwable?>(null)

    init {
        viewModelScope.launch {
            service.stateObservable.asFlow().collect {
                handle(it)
            }
        }
    }

    fun validateAmount(value: String): Boolean {
        if (value.isEmpty()) return true

        return try {
            BigDecimal(value).scale() <= coinService.token.decimals
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun onEnterAmount(value: String) {
        service.amount = when {
            value.isEmpty() -> null
            else -> coinService.convertToFractionalMonetaryValue(BigDecimal(value))
        }
    }

    private fun handle(approveState: SwapApproveService.State) {
        approveAllowed = approveState is SwapApproveService.State.ApproveAllowed
        amountError = (approveState as? SwapApproveService.State.ApproveNotAllowed)?.error
    }

    fun getSendEvmData(): SendEvmData? {
        return (service.state as? SwapApproveService.State.ApproveAllowed)
            ?.let {
                SendEvmData(it.transactionData)
            }
    }
}
