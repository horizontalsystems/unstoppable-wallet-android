package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendEvmViewModel(
        val service: SendEvmService,
        private val clearables: List<Clearable>
) : ViewModel() {
    private val disposable = CompositeDisposable()

    var proceedEnabled by mutableStateOf(false)
        private set
    var proceedEvent by mutableStateOf<SendEvmData?>(null)
        private set
    var amountCaution by mutableStateOf<Caution?>(null)
        private set
    val availableBalance get() = service.availableBalance
    val coinDecimal get() = service.coinDecimal
    val fiatDecimal get() = service.fiatDecimal

    var amountInputMode by mutableStateOf(AmountInputModule.InputMode.Coin)
        private set

    init {
        service.stateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.amountCautionObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
    }

    fun onClickProceed() {
        (service.state as? SendEvmService.State.Ready)?.let { readyState ->
            proceedEvent = readyState.sendData
        }
    }

    private fun sync(state: SendEvmService.State) {
        viewModelScope.launch {
            proceedEnabled = state is SendEvmService.State.Ready
        }
    }

    private fun sync(amountCaution: SendEvmService.AmountCaution) {
        var caution: Caution? = null
        if (amountCaution.error?.convertedError != null) {
            val text =
                amountCaution.error.localizedMessage ?: amountCaution.error.javaClass.simpleName
            caution = Caution(text, Caution.Type.Error)
        } else if (amountCaution.amountWarning == SendEvmService.AmountWarning.CoinNeededForFee) {
            caution = Caution(
                Translator.getString(
                    R.string.EthereumTransaction_Warning_CoinNeededForFee, service.sendCoin.code
                ),
                Caution.Type.Warning
            )
        }

        this.amountCaution = caution
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
    }

    fun onEnterAddress(address: Address?) {
        service.setRecipientAddress(address)
    }

    fun onEnterAmount(amount: BigDecimal?) {
        service.onChangeAmount(amount)
    }

    fun onHandleProceedEvent() {
        proceedEvent = null
    }

    fun onUpdateAmountInputMode(mode: AmountInputModule.InputMode) {
        amountInputMode = mode
    }
}
