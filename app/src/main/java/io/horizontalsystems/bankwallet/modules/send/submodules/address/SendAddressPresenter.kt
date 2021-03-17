package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.IRecipientAddressService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.hodler.HodlerPlugin
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal
import java.util.*

class SendAddressPresenter(
        private val moduleDelegate: SendAddressModule.IAddressModuleDelegate,
        private val stringProvider: StringProvider
) : ViewModel(), IRecipientAddressService, SendAddressModule.IAddressModule, SendAddressModule.IInteractorDelegate, SendAddressModule.IViewDelegate {

    private val errorsObservable = BehaviorSubject.createDefault<Optional<Throwable>>(Optional.empty())

    //  IRecipientAddressService

    override val initialAddress: Address? = null

    override var error: Throwable? = null
        private set(value) {
            errorsObservable.onNext(Optional.ofNullable(value))
            field = value
        }

    override val errorObservable: Observable<Unit> = errorsObservable.map {
        Unit
    }

    override fun set(address: Address?) {
        onSetAddress(address)
    }

    override fun set(amount: BigDecimal) {
        moduleDelegate.onUpdateAmount(amount)
    }

    private fun onSetAddress(address: Address?) {
        if (address == null || address.hex.isEmpty()) {
            error = null
            currentAddress = null
            enteredAddress = null
            moduleDelegate.onUpdateAddress()

            return
        }

        onAddressEnter(address)
    }

    //  IAddressModule

    private var enteredAddress: Address? = null

    override var currentAddress: Address? = null

    override fun validAddress(): Address {
        return currentAddress ?: throw SendAddressModule.ValidationError.EmptyValue()
    }

    override fun validateAddress() {
        val address = enteredAddress
        if (address == null) {
            currentAddress = null
            throw SendAddressModule.ValidationError.EmptyValue()
        }

        try {
            moduleDelegate.validate(address.hex)
            currentAddress = address
            error = null
        } catch (err: Exception) {
            currentAddress = null
            error = getError(err)
            throw err
        }
    }

    private fun getError(error: Throwable): Throwable {
        val message = when (error) {
            is HodlerPlugin.UnsupportedAddressType -> stringProvider.string(R.string.Send_Error_UnsupportedAddress)
            is AddressValidator.AddressValidationException -> stringProvider.string(R.string.Send_Error_IncorrectAddress)
            is ZcashAdapter.ZcashError.TransparentAddressNotAllowed -> stringProvider.string(R.string.Send_Error_TransparentAddress)
            is ZcashAdapter.ZcashError.SendToSelfNotAllowed -> stringProvider.string(R.string.Send_Error_SendToSelf)
            else -> error.message ?: error.javaClass.simpleName
        }

        return Throwable(message)
    }

    // SendAddressModule.IViewDelegate

    override fun onViewDidLoad() {
    }

    override fun onAddressDeleteClicked() {
        enteredAddress = null
        moduleDelegate.onUpdateAddress()
    }

    private fun onAddressEnter(address: Address) {
        enteredAddress = address

        try {
            validateAddress()
        } catch (e: Exception) {
        }

        moduleDelegate.onUpdateAddress()
    }
}
