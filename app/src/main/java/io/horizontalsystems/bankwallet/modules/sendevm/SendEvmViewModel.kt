package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class SendEvmViewModel(
        val service: SendEvmService,
        private val clearables: List<Clearable>
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val proceedEnabledLiveData = MutableLiveData(false)
    val amountCautionLiveData = MutableLiveData<Caution?>(null)
    val proceedLiveEvent = SingleLiveEvent<SendEvmData>()
    val availableBalance: BigDecimal get() = service.availableBalance

    val coin: PlatformCoin
        get() = service.coin

    init {
        service.stateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.amountCautionObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
    }

    fun onClickProceed() {
        (service.state as? SendEvmService.State.Ready)?.let { readyState ->
            proceedLiveEvent.postValue(readyState.sendData)
        }
    }

    private fun sync(state: SendEvmService.State) {
        proceedEnabledLiveData.postValue(state is SendEvmService.State.Ready)
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

        amountCautionLiveData.postValue(caution)
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
        proceedLiveEvent.postValue(null)
    }
}
