package io.horizontalsystems.bankwallet.modules.send.sendviews.address

import io.horizontalsystems.bankwallet.entities.AddressError
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress

class SendAddressPresenter(private val interactor: SendAddressModule.IInteractor)
    : SendAddressModule.IViewDelegate, SendAddressModule.IInteractorDelegate {

    var view: SendAddressViewModel? = null
    private var address: String? = null
    private var invalidAddressError: AddressError.InvalidPaymentAddress? = null

    override val validState: Boolean
        get() {
            return !address.isNullOrBlank() && invalidAddressError == null
        }

    override fun onViewDidLoad() {
        updatePasteButtonState()
    }

    override fun getAddress(): String? {
        return address
    }

    override fun onAddressScan(address: String) {
        onAddressEnter(address)
    }

    override fun onPasteButtonClick() {
        interactor.addressFromClipboard?.let {
            onAddressEnter(it)
        }
    }

    override fun onAddressDeleteClick() {
        onAddressChange(null)
        updatePasteButtonState()
    }

    override fun onParsedAddress(parsedAddress: PaymentRequestAddress) {
        parsedAddress.amount?.let {
            view?.onAmountChange(it)
        }

        address = parsedAddress.address
        onAddressChange(parsedAddress.address, parsedAddress.error)
    }

    private fun onAddressEnter(address: String) {
        view?.parseAddressInMainViewModel(address)
    }

    private fun onAddressChange(address: String?, error: AddressError.InvalidPaymentAddress? = null) {
        this.invalidAddressError = error
        this.address = address
        view?.setAddress(address)
        view?.setAddressError(error)
        //update send button state
        view?.notifyMainViewModelAddressUpdated()
    }

    private fun updatePasteButtonState() {
        view?.setPasteButtonState(interactor.clipboardHasPrimaryClip)
    }
}
