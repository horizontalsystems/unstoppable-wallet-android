package io.horizontalsystems.bankwallet.modules.receive.viewitems

import io.horizontalsystems.bankwallet.modules.transactions.Coin

data class AddressItem(var address: String, var coin: Coin)
