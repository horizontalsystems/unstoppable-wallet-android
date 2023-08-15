package cash.p.terminal.entities

import android.os.Parcelable
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import java.util.Objects

@Parcelize
data class ConfiguredToken(
    val token: Token,
): Parcelable {
    override fun hashCode(): Int {
        return Objects.hash(token)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ConfiguredToken &&
                other.token == token
    }

}
