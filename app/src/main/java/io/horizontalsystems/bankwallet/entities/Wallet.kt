package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.modules.transactions.FilterAdapter
import io.horizontalsystems.coinkit.models.Coin
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class Wallet(val coin: Coin, val account: Account) : Parcelable, FilterAdapter.FilterItem(coin.code) {

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
