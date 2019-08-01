package io.horizontalsystems.bankwallet.entities

import java.util.*

class Wallet(val coin: Coin, val account: Account, val syncMode: SyncMode?) {
    override fun equals(other: Any?): Boolean {
        if (other is Wallet) {
            return coin == other.coin && account == other.account
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(coin, account)
    }
}
