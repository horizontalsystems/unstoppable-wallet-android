package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem

class ReceivePresenter(
        private val interactor: ReceiveModule.IInteractor,
        private val router: ReceiveModule.IRouter) : ReceiveModule.IViewDelegate, ReceiveModule.IInteractorDelegate {

    var view: ReceiveModule.IView? = null
    private var receiveAddresses: List<AddressItem> = mutableListOf()

    override fun viewDidLoad() {
        interactor.getReceiveAddress()
    }

    override fun didReceiveAddresses(addresses: List<AddressItem>) {
        this.receiveAddresses = addresses
        view?.showAddresses(receiveAddresses)
    }

    override fun didFailToReceiveAddress(exception: Exception) {
        view?.showError(R.string.Error)
    }

    override fun onShareClick(index: Int) {
        router.shareAddress(receiveAddresses[index].address)
    }

    override fun onAddressClick(index: Int) {
        interactor.copyToClipboard(receiveAddresses[index].address)
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

}
