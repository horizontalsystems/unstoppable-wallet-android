package cash.p.terminal.wallet

import android.os.Parcelable
import cash.p.terminal.wallet.transaction.TransactionSource
import kotlinx.parcelize.Parcelize
import java.util.Objects

@Parcelize
data class Wallet(
    val token: Token,
    val account: Account
) : Parcelable {

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
