package io.horizontalsystems.bankwallet.modules.multiswap

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class PriceImpactLevel : Parcelable {
    Negligible, Normal, Warning, Forbidden
}
