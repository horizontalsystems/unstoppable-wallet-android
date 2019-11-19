package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Coin(val coinId: String, val title: String, val code: String, val decimal: Int, val type: CoinType) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (other is Coin) {
            return coinId == other.coinId && title == other.title && code == other.code
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(coinId, title, code)
    }
}
