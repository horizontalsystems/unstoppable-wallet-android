package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Wallet(
    val configuredToken: ConfiguredToken,
    val account: Account
) : Parcelable {
    val token
        get() = configuredToken.token

    val coinSettings
        get() = configuredToken.coinSettings

    val coin
        get() = token.coin

    val decimal
        get() = token.decimals

    val badge
        get() = configuredToken.badge

    val transactionSource get() = TransactionSource(token.blockchain, account, coinSettings)

    constructor(token: Token, account: Account) : this(ConfiguredToken(token), account)

    override fun equals(other: Any?): Boolean {
        if (other is Wallet) {
            return configuredToken == other.configuredToken && account == other.account
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(configuredToken, account)
    }
}
