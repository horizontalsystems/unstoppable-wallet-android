package io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.toMoneroSeed
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.monerokit.MoneroKit
import kotlinx.parcelize.Parcelize

object ShowMoneroKeyModule {

    @Parcelize
    class MoneroKeys(
        val spendKey: String,
        val viewKey: String,
        val isPrivate: Boolean,
        val title: Int = if (isPrivate) R.string.MoneroKeyType_Private else R.string.MoneroKeyType_Public
    ) : Parcelable {

        fun getKey(type: MoneroKeyType) = when (type) {
            MoneroKeyType.Spend -> spendKey
            MoneroKeyType.View -> viewKey
        }

    }

    enum class MoneroKeyType(val title: Int) {
        Spend(R.string.MoneroKeyType_Spend),
        View(R.string.MoneroKeyType_View)
    }

    fun getPrivateMoneroKeys(account: Account) = getMoneroKeys(account, true)

    fun getPublicMoneroKeys(account: Account) = getMoneroKeys(account, false)

    private fun getMoneroKeys(account: Account, isPrivate: Boolean): MoneroKeys? = try {
        val moneroSeed = account.type.toMoneroSeed()
        val keys = MoneroKit.getKeys(moneroSeed)
        if (isPrivate) {
            MoneroKeys(keys.privateSpendKey, keys.privateViewKey, true)
        } else {
            MoneroKeys(keys.publicSpendKey, keys.publicViewKey, false)
        }
    } catch (_: Throwable) {
        null
    }

}
