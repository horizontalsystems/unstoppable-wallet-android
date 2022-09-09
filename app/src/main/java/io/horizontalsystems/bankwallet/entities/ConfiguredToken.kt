package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class ConfiguredToken(
    val token: Token,
    val coinSettings: CoinSettings = CoinSettings()
): Parcelable {
    override fun hashCode(): Int {
        return Objects.hash(token, coinSettings)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ConfiguredToken &&
                other.token == token &&
                other.coinSettings == coinSettings
    }
}
