package io.horizontalsystems.bankwallet.modules.receive.viewitems

import io.horizontalsystems.bankwallet.entities.coins.Coin

class AddressItem(var adapterId: String = "", var address: String = "", var coin: Coin) {
    override fun equals(other: Any?): Boolean {
        if (other is AddressItem) {
            return adapterId == other.adapterId
                    && address == other.address
                    && coin.code == other.coin.code
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = adapterId.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + coin.hashCode()
        return result
    }
}
