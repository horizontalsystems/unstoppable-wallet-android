package io.horizontalsystems.bankwallet.modules.receive.viewitems

import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

data class AddressItem(var address: String, var coinCode: CoinCode)
