package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class SwapApproveViewModel(
        val dex: SwapModule.Dex,
        private val service: SwapApproveService,
        private val coinService: EvmCoinService,
        private val stringProvider: StringProvider
) : ViewModel() {

    var amount: String
        get() {
            return service.amount?.let {
                coinService.convertToMonetaryValue(it).toPlainString()
            } ?: ""
        }
        set(value) {
            service.amount = when {
                value.isEmpty() -> null
                else -> coinService.convertToFractionalMonetaryValue(BigDecimal(value))
            }
        }

    private val disposables = CompositeDisposable()

    val approveAllowedLiveData = MutableLiveData<Boolean>()
    val openConfirmationLiveEvent = SingleLiveEvent<SendEvmData>()
    val amountErrorLiveData = MutableLiveData<String?>(null)

    init {
        service.stateObservable
                .subscribe {
                    handle(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    fun validateAmount(value: String): Boolean {
        if (value.isEmpty()) return true

        return try {
            BigDecimal(value).scale() <= coinService.coin.decimal
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun handle(approveState: SwapApproveService.State) {
        approveAllowedLiveData.postValue(approveState is SwapApproveService.State.ApproveAllowed)

        var amountErrorText: String? = null

        if (approveState is SwapApproveService.State.ApproveNotAllowed) {
            val errors = approveState.errors.toMutableList()

            val balanceErrorIndex = errors.indexOfFirst {
                it is SwapApproveService.TransactionAmountError
            }
            if (balanceErrorIndex != -1) {
                amountErrorText = convertError(errors.removeAt(balanceErrorIndex))
            }
        }
        amountErrorLiveData.postValue(amountErrorText)
    }

    private fun convertError(error: Throwable): String {
        return when (val convertedError = error.convertedError) {
            is SwapApproveService.TransactionAmountError.AlreadyApproved -> {
                stringProvider.string(R.string.Approve_Error_AlreadyApproved)
            }
            else -> convertedError.message ?: convertedError.javaClass.simpleName
        }
    }

    fun onProceed() {
        val serviceState = service.state
        if (serviceState is SwapApproveService.State.ApproveAllowed) {
            openConfirmationLiveEvent.postValue(SendEvmData(serviceState.transactionData))
        }
    }

    override fun onCleared() {
        disposables.dispose()
    }
}
