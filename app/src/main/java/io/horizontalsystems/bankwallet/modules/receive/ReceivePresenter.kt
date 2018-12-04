package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem

class ReceivePresenter(private val interactor: ReceiveModule.IInteractor) : ReceiveModule.IViewDelegate, ReceiveModule.IInteractorDelegate {

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
        view?.showError(getError(exception))
    }

    private fun getError(exception: Exception) = when (exception) {
//        is UnsupportedBlockchain -> R.string.Error_UnsupportedBlockchain
        else -> R.string.Error
    }

    override fun onCopyClick(index: Int) {
        interactor.copyToClipboard(receiveAddresses[index].address)
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

}
