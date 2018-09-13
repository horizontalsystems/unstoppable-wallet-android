package bitcoin.wallet.modules.receive

import bitcoin.wallet.R
import bitcoin.wallet.modules.receive.viewitems.AddressItem

class ReceivePresenter(private val interactor: ReceiveModule.IInteractor, private val router: ReceiveModule.IRouter) : ReceiveModule.IViewDelegate, ReceiveModule.IInteractorDelegate {

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
//        is UnsupportedBlockchain -> R.string.error_unsupported_blockchain
        else -> R.string.error
    }

    override fun onCopyClick(index: Int) {
        interactor.copyToClipboard(receiveAddresses[index].address)
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

    override fun onShareClick(index: Int) {
        router.openShareView(receiveAddresses[index].address)
    }

}
