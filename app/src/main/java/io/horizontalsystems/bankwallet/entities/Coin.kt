package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Coin(val title: String, val code: String, val type: CoinType) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (other is Coin) {
            return title == other.title && code == other.code
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(title, code)
    }
}
