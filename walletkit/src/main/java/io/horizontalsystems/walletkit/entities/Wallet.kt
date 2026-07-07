package io.horizontalsystems.walletkit.entities

import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.meta
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import kotlinx.serialization.Serializable
import java.util.Objects

@Serializable
data class Wallet(
    val token: Token,
    val account: Account
) {

    val coin
        get() = token.coin

    val decimal
        get() = token.decimals

    val badge
        get() = token.badge

    val transactionSource get() = TransactionSource(token.blockchain, account, token.type.meta)

    override fun equals(other: Any?): Boolean {
        if (other is Wallet) {
            return token == other.token && account == other.account
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(token, account)
    }
}
