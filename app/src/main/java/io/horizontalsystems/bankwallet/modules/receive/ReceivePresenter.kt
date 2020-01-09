package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem

class ReceivePresenter(
        val view: ReceiveModule.IView,
        val router: ReceiveModule.IRouter,
        private val interactor: ReceiveModule.IInteractor
) : ViewModel(), ReceiveModule.IViewDelegate, ReceiveModule.IInteractorDelegate {

    private var receiveAddress: AddressItem? = null

    override fun viewDidLoad() {
        interactor.getReceiveAddress()
    }

    override fun didReceiveAddress(address: AddressItem) {
        this.receiveAddress = address
        view.showAddress(address)
        val (hintId, hintDetails) = getHint(address.coin.type, address.address)
        view.setHint(hintId, hintDetails)
    }

    private fun getHint(type: CoinType, address: String ): Pair<Int,String> {
        return when (type) {
            is CoinType.Eos -> Pair(R.string.Deposit_Your_Account, "")
            is CoinType.Bitcoin -> {
                Pair(R.string.Deposit_Your_Account,
                     address.let {
                         if (it.startsWith("1"))
                             "(${AccountType.Derivation.bip44.value.toUpperCase()})"
                         else if (it.startsWith("3"))
                             "(${AccountType.Derivation.bip49.value.toUpperCase()})"
                         else if (it.startsWith("bc1"))
                             "(${AccountType.Derivation.bip84.value.toUpperCase()})"
                         else ""
                     }
                )
            }
            else -> Pair(R.string.Deposit_Your_Address, "")
        }
    }

    override fun didFailToReceiveAddress(exception: Exception) {
        view.showError(R.string.Error)
    }

    override fun onShareClick() {
        receiveAddress?.address?.let { router.shareAddress(it) }
    }

    override fun onAddressClick() {
        receiveAddress?.address?.let { interactor.copyToClipboard(it) }
    }

    override fun didCopyToClipboard() {
        view.showCopied()
    }

}
