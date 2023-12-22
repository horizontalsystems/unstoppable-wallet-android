package io.horizontalsystems.bankwallet.modules.depositcex

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexDepositNetwork
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressesParams

class CexDepositSharedViewModel : ViewModel() {

    var network: CexDepositNetwork? = null
    var cexAsset: CexAsset? = null
    var usedAddressesParams: UsedAddressesParams? = null

}