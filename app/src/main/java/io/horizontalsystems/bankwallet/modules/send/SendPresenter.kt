package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.CustomPriorityUnit
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SendPresenter : ViewModel(), SendModule.ISendHandlerDelegate {

    val error = SingleLiveEvent<Throwable>()
    val confirmationViewItems = MutableLiveData<List<SendModule.SendConfirmationViewItem>>()
    val showSendConfirmation = SingleLiveEvent<Unit>()
    val sendButtonEnabled = MutableLiveData<ActionState>()
    val inputItems = SingleLiveEvent<List<SendModule.Input>>()
    val closeWithSuccess = SingleLiveEvent<Unit>()

    var amountModuleDelegate: SendAmountModule.IAmountModuleDelegate? = null
    var addressModuleDelegate: SendAddressModule.IAddressModuleDelegate? = null
    var feeModuleDelegate: SendFeeModule.IFeeModuleDelegate? = null
    var hodlerModuleDelegate: SendHodlerModule.IHodlerModuleDelegate? = null
    var customPriorityUnit: CustomPriorityUnit? = null

    lateinit var handler: SendModule.ISendHandler

    private var disposables = CompositeDisposable()

    // SendModule.IViewDelegate

    fun onViewDidLoad() {
        inputItems.value = handler.inputItems
    }

    fun onModulesDidLoad() {
        handler.onModulesDidLoad()
    }

    fun onProceedClicked() {
        confirmationViewItems.value = handler.confirmationViewItems()
        showSendConfirmation.call()
    }

    fun onSendConfirmed(logger: AppLogger) {
        handler.sendSingle(logger).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                logger.info("success")

                closeWithSuccess.call()
            }, { error ->
                logger.warning("failed", error)

                this.error.value = error
            }).let {
                disposables.add(it)
            }
    }

    // ViewModel

    override fun onCleared() {
        disposables.clear()
        handler.onClear()
    }

    // SendModule.ISendInteractorDelegate

    fun sync() {
        handler.sync()
    }

    // SendModule.ISendHandlerDelegate

    override fun onChange(isValid: Boolean, amountError: Throwable?, addressError: Throwable?) {
        val actionState: ActionState

        if (isValid) {
            actionState = ActionState.Enabled()
        } else if (amountError != null && !isEmptyAmountError(amountError)) {
            actionState = ActionState.Disabled("Invalid Amount")
        } else if (addressError != null && !isEmptyAddressError(addressError)) {
            actionState = ActionState.Disabled("Invalid Address")
        } else {
            actionState = ActionState.Disabled(null)
        }

        sendButtonEnabled.postValue(actionState)
    }

    private fun isEmptyAmountError(error: Throwable): Boolean {
        return error is SendAmountModule.ValidationError.EmptyValue
    }

    private fun isEmptyAddressError(error: Throwable): Boolean {
        return error is AddressValidationException.Blank
    }

    sealed class ActionState {
        class Enabled : ActionState()
        class Disabled(val title: String?) : ActionState()
    }
}
