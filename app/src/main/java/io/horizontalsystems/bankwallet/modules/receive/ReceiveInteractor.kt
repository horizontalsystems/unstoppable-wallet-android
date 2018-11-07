package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem

class ReceiveInteractor(private var adapterManager: IAdapterManager, private var adapterId: String?, private var clipboardManager: IClipboardManager) : ReceiveModule.IInteractor {

    var delegate: ReceiveModule.IInteractorDelegate? = null

    override fun getReceiveAddress() {
        val adapters = adapterManager.adapters.filter { adapterId == null || it.id == adapterId }

        val addresses = mutableListOf<AddressItem>()
        adapters.forEach { adapter ->
            addresses.add(AddressItem(adapterId = adapter.id, address = adapter.receiveAddress, coin = adapter.coin))
        }
        if (addresses.isNotEmpty()) {
            delegate?.didReceiveAddresses(addresses)
        }
    }

    override fun copyToClipboard(coinAddress: String) {
        clipboardManager.copyText(coinAddress)
        delegate?.didCopyToClipboard()
    }

}
