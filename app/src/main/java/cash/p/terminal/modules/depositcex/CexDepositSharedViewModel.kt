package cash.p.terminal.modules.depositcex

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexDepositNetwork
import cash.p.terminal.modules.receive.ui.UsedAddressesParams

class CexDepositSharedViewModel : ViewModel() {

    var network: CexDepositNetwork? = null
    var cexAsset: CexAsset? = null
    var usedAddressesParams: UsedAddressesParams? = null

}