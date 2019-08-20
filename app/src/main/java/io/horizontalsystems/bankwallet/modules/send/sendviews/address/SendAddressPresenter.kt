package io.horizontalsystems.bankwallet.modules.send.sendviews.address

class SendAddressPresenter(private val interactor: SendAddressModule.IInteractor)
    : SendAddressModule.IViewDelegate, SendAddressModule.IInteractorDelegate, SendAddressModule.IAddressModule {

    var view: SendAddressModule.IView? = null
    var moduleDelegate: SendAddressModule.IAddressModuleDelegate? = null

    // SendAddressModule.IAddressModule

    override var address: String? = null
        private set(value) {
            field = value
            moduleDelegate?.onUpdateAddress()
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
        view?.setAddress(address)
        view?.setAddressError(error)

        this.address = address
    }

    private fun updatePasteButtonState() {
        view?.setPasteButtonState(interactor.clipboardHasPrimaryClip)
    }

}
