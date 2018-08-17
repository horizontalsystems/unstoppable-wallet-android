package bitcoin.wallet.modules.receive

import bitcoin.wallet.R
import bitcoin.wallet.blockchain.UnsupportedBlockchain

class ReceivePresenter(private val interactor: ReceiveModule.IInteractor, private val router: ReceiveModule.IRouter, private val coinCode: String) : ReceiveModule.IViewDelegate, ReceiveModule.IInteractorDelegate {

    var view: ReceiveModule.IView? = null
    private var coinAddress: String? = null

    override fun viewDidLoad() {
        interactor.getReceiveAddress(coinCode)
    }

    override fun didReceiveAddress(coinAddress: String) {
        this.coinAddress = coinAddress
        view?.showAddress(coinAddress)
    }

    override fun didFailToReceiveAddress(exception: Exception) {
        view?.showError(getError(exception))
    }

    private fun getError(exception: Exception) = when (exception) {
        is UnsupportedBlockchain -> R.string.error_unsupported_blockchain
        else -> R.string.error
    }

    override fun onCopyClick() {
        coinAddress?.let { interactor.copyToClipboard(it) }
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

    override fun onShareClick() {
        coinAddress?.let { router.openShareView(it) }
    }

}
