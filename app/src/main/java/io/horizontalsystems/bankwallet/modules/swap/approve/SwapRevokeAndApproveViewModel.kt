package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal
import java.math.BigInteger

class SwapRevokeAndApproveViewModel(private val approveData: SwapAllowanceService.ApproveData) : ViewModel() {

    private var observeAllowanceJob: Job? = null
    private val erc20Adapter: Eip20Adapter
    private var amount = approveData.amount

    val blockchainType: BlockchainType = approveData.dex.blockchainType
    val requiredAmountStr = approveData.amount.toString()
    val steps = listOf("1", "2")
    val allowanceValue = CoinValue(approveData.token, approveData.allowance)

    private var revokeEnabled = true
    private var revokeInProgress = false
    private var approveEnabled = false
    private var showAmountInput = false
    private var currentStep = "1"

    var uiState by mutableStateOf(
        UiState(
            revokeEnabled = revokeEnabled,
            revokeInProgress = revokeInProgress,
            approveEnabled = approveEnabled,
            showAmountInput = showAmountInput,
            currentStep = currentStep,
        )
    )

    init {
        val wallet =
            checkNotNull(App.walletManager.activeWallets.firstOrNull { it.token == approveData.token })
        erc20Adapter = App.adapterManager.getAdapterForWallet(wallet) as Eip20Adapter
    }

    fun getRevokeSendEvmData(): SendEvmData {
        val transactionData = erc20Adapter.eip20Kit.buildApproveTransactionData(
            Address(approveData.spenderAddress),
            BigInteger.ZERO
        )

        return SendEvmData(transactionData)
    }

    fun getApproveSendEvmData(): SendEvmData {
        val transactionData = erc20Adapter.eip20Kit.buildApproveTransactionData(
            Address(approveData.spenderAddress),
            amount.movePointRight(approveData.token.decimals).toBigInteger()
        )

        return SendEvmData(transactionData)
    }

    fun onRevokeTransactionSend() {
        revokeEnabled = false
        revokeInProgress = true
        emitState()

        observeAllowanceJob = viewModelScope.launch {
            erc20Adapter.evmKit.lastBlockHeightFlowable.asFlow().cancellable()
                .collect {
                    val allowance = erc20Adapter.allowance(
                        Address(approveData.spenderAddress),
                        DefaultBlockParameter.Latest
                    ).await()

                    if (allowance.compareTo(BigDecimal.ZERO) == 0) {
                        revokeInProgress = false
                        approveEnabled = true
                        showAmountInput = true
                        currentStep = "2"
                        emitState()

                        observeAllowanceJob?.cancel()
                    }
                }
        }
    }

    private fun emitState() {
        uiState = UiState(
            revokeEnabled = revokeEnabled,
            revokeInProgress = revokeInProgress,
            approveEnabled = approveEnabled,
            showAmountInput = showAmountInput,
            currentStep = currentStep,
        )
    }

    fun validateAmount(value: String): Boolean {
        if (value.isEmpty()) return true

        return try {
            BigDecimal(value).scale() <= approveData.token.decimals
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun onEnterAmount(value: String) {
        amount = try {
            BigDecimal(value)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }

        approveEnabled = amount.compareTo(BigDecimal.ZERO) != 0
        emitState()
    }

    class Factory(private val approveData: SwapAllowanceService.ApproveData) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapRevokeAndApproveViewModel(approveData) as T
        }
    }

    data class UiState(
        var revokeEnabled: Boolean,
        var revokeInProgress: Boolean,
        var approveEnabled: Boolean,
        var showAmountInput: Boolean,
        val currentStep: String,
    )

}
