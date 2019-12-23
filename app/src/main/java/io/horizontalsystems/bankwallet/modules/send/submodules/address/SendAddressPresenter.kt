package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.ViewModel

class SendAddressPresenter(
        val view: SendAddressModule.IView,
        val editable: Boolean,
        private val interactor: SendAddressModule.IInteractor
) : ViewModel(), SendAddressModule.IAddressModule, SendAddressModule.IInteractorDelegate, SendAddressModule.IViewDelegate {

    var moduleDelegate: SendAddressModule.IAddressModuleDelegate? = null

    private var enteredAddress: String? = null

    override val currentAddress: String?
        get() = try {
            validAddress()
        } catch (e: Exception) {
            null
        }

    override fun validAddress(): String {
        val address = enteredAddress ?: throw SendAddressModule.ValidationError.InvalidAddress()

        try {
            moduleDelegate?.validate(address)

            view.setAddress(address)
            view.setAddressError(null)
        } catch (e: Exception) {
            view.setAddress(address)
            view.setAddressError(e)
            throw e
        }

        return address
    }

    override fun didScanQrCode(address: String) {
        onAddressEnter(address)
    }

    // SendAddressModule.IViewDelegate

    override fun onViewDidLoad() {
        updatePasteButtonState()
        view.setAddressInputAsEditable(editable)
    }

    override fun onAddressScanClicked() {
        moduleDelegate?.scanQrCode()
    }

    override fun onAddressPasteClicked() {
        interactor.addressFromClipboard?.let {
            onAddressEnter(it)
        }
    }

    override fun onAddressDeleteClicked() {
        view.setAddress(null)
        view.setAddressError(null)

        enteredAddress = null
        moduleDelegate?.onUpdateAddress()
        updatePasteButtonState()
    }

    private fun onAddressEnter(address: String) {
        val (parsedAddress, amount) = interactor.parseAddress(address)

        enteredAddress = parsedAddress

        try {
            validAddress()
        } catch (e: Exception) {
        }

        moduleDelegate?.onUpdateAddress()

        amount?.let { parsedAmount ->
            moduleDelegate?.onUpdateAmount(parsedAmount)
        }
    }

    override fun onManualAddressEnter(addressText: String) {
        enteredAddress = addressText

        try {
            moduleDelegate?.validate(addressText)
            view.setAddressError(null)
        } catch (e: Exception) {
            view.setAddressError(e)
        }

        moduleDelegate?.onUpdateAddress()
    }

    private fun updatePasteButtonState() {
        view.setPasteButtonState(interactor.clipboardHasPrimaryClip)
    }

}
