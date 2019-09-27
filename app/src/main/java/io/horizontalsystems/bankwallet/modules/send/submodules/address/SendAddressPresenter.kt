package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.ViewModel

class SendAddressPresenter(val view: SendAddressModule.IView,
                           private val interactor: SendAddressModule.IInteractor)
    : ViewModel(), SendAddressModule.IAddressModule, SendAddressModule.IInteractorDelegate,
      SendAddressModule.IViewDelegate {

    var moduleDelegate: SendAddressModule.IAddressModuleDelegate? = null

    override var currentAddress: String? = null
        private set(value) {
            field = value
            moduleDelegate?.onUpdateAddress()
        }

    override fun validAddress(): String {
        return currentAddress ?: throw SendAddressModule.ValidationError.InvalidAddress()
    }

    override fun didScanQrCode(address: String) {
        onAddressEnter(address)
    }

    // SendAddressModule.IViewDelegate

    override fun onViewDidLoad() {
        updatePasteButtonState()
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
        updateAddress(null)
        updatePasteButtonState()
    }

    private fun onAddressEnter(address: String) {
        val (parsedAddress, amount) = interactor.parseAddress(address)

        try {
            moduleDelegate?.validate(parsedAddress)

            updateAddress(parsedAddress, null)

            amount?.let { parsedAmount ->
                moduleDelegate?.onUpdateAmount(parsedAmount)
            }
        } catch (ex: Exception) {
            updateAddress(parsedAddress, ex)
        }
    }

    private fun updateAddress(address: String?, error: Exception? = null) {
        view.setAddress(address)
        view.setAddressError(error)

        this.currentAddress = if (error == null) address else null
    }

    private fun updatePasteButtonState() {
        view.setPasteButtonState(interactor.clipboardHasPrimaryClip)
    }

}
