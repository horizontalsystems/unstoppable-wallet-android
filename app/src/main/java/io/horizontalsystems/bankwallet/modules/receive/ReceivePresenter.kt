package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem

class ReceivePresenter(
        private val interactor: ReceiveModule.IInteractor,
        private val router: ReceiveModule.IRouter) : ReceiveModule.IViewDelegate, ReceiveModule.IInteractorDelegate {

    var view: ReceiveModule.IView? = null
    private var receiveAddress: AddressItem? = null

    override fun viewDidLoad() {
        interactor.getReceiveAddress()
    }

    override fun didReceiveAddress(address: AddressItem) {
        this.receiveAddress = address
        view?.showAddress(address)
    }

    override fun didFailToReceiveAddress(exception: Exception) {
        view?.showError(R.string.Error)
    }

    override fun onShareClick() {
        receiveAddress?.address?.let { router.shareAddress(it) }
    }

    override fun onAddressClick() {
        receiveAddress?.address?.let { interactor.copyToClipboard(it) }
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

}
