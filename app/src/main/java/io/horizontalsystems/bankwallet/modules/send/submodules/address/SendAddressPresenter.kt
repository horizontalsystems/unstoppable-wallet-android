package io.horizontalsystems.bankwallet.modules.send.submodules.address

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.swap.settings.IRecipientAddressService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.hodler.HodlerPlugin
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class SendAddressPresenter(
    private val moduleDelegate: SendAddressModule.IAddressModuleDelegate
) : IRecipientAddressService, SendAddressModule.IAddressModule {

    //  IRecipientAddressService

    override val initialAddress: Address? = null
    override val recipientAddressState = BehaviorSubject.create<DataState<Unit>>()

    override fun setRecipientAddress(address: Address?) {
        onSetAddress(address)
    }

    override fun setRecipientAmount(amount: BigDecimal) {
        moduleDelegate.onUpdateAmount(amount)
    }

    private fun onSetAddress(address: Address?) {
        if (address == null || address.hex.isEmpty()) {
            recipientAddressState.onNext(DataState.Success(Unit))
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
            recipientAddressState.onNext(DataState.Success(Unit))
        } catch (err: Exception) {
            currentAddress = null
            recipientAddressState.onNext(DataState.Error(getError(err)))
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
