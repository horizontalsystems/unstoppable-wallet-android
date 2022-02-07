package io.horizontalsystems.bankwallet.modules.send.submodules.address

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.swap.settings.IRecipientAddressService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.hodler.HodlerPlugin
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal
import java.util.*

class SendAddressPresenter(
    private val moduleDelegate: SendAddressModule.IAddressModuleDelegate
) : IRecipientAddressService, SendAddressModule.IAddressModule {

    private val errorsObservable = BehaviorSubject.createDefault<Optional<Throwable>>(Optional.empty())

    //  IRecipientAddressService

    override val initialAddress: Address? = null

    override var recipientAddressError: Throwable? = null
        private set(value) {
            errorsObservable.onNext(Optional.ofNullable(value))
            field = value
        }

    override val recipientAddressErrorObservable: Observable<Unit> = errorsObservable.map {
        Unit
    }

    override fun setRecipientAddress(address: Address?) {
        onSetAddress(address)
    }

    override fun setRecipientAmount(amount: BigDecimal) {
        moduleDelegate.onUpdateAmount(amount)
    }

    private fun onSetAddress(address: Address?) {
        if (address == null || address.hex.isEmpty()) {
            recipientAddressError = null
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
        return currentAddress ?: throw AddressValidationException.Blank()
    }

    override fun validateAddress() {
        val address = enteredAddress
        if (address == null) {
            currentAddress = null
            throw AddressValidationException.Blank()
        }

        try {
            moduleDelegate.validate(address.hex)
            currentAddress = address
            recipientAddressError = null
        } catch (err: Exception) {
            currentAddress = null
            recipientAddressError = getError(err)
            throw err
        }
    }

    private fun getError(error: Throwable): Throwable {
        val message = when (error) {
            is HodlerPlugin.UnsupportedAddressType -> Translator.getString(R.string.Send_Error_UnsupportedAddress)
            is AddressValidator.AddressValidationException -> Translator.getString(R.string.Send_Error_IncorrectAddress)
            is ZcashAdapter.ZcashError.TransparentAddressNotAllowed -> Translator.getString(R.string.Send_Error_TransparentAddress)
            is ZcashAdapter.ZcashError.SendToSelfNotAllowed -> Translator.getString(R.string.Send_Error_SendToSelf)
            else -> error.message ?: error.javaClass.simpleName
        }

        return Throwable(message)
    }

    // SendAddressModule.IViewDelegate

    private fun onAddressEnter(address: Address) {
        enteredAddress = address

        try {
            validateAddress()
        } catch (e: Exception) {
        }

        moduleDelegate.onUpdateAddress()
    }
}
