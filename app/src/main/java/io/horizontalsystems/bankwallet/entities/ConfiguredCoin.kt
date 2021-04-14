package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.coinkit.models.Coin
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ConfiguredCoin(val coin: Coin, val settings: CoinSettings = CoinSettings()) : Parcelable
