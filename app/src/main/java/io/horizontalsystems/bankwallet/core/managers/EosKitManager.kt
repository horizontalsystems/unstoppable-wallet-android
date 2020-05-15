package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IEosKitManager
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.eoskit.EosKit

class EosKitManager(
        private val testMode: Boolean
) : IEosKitManager {
    private var kit: EosKit? = null
    private var useCount = 0

    override val eosKit: EosKit?
        get() = kit

    override val statusInfo: Map<String, Any>?
        get() = eosKit?.statusInfo()

    override fun eosKit(wallet: Wallet): EosKit {
        val account = wallet.account
        if (account.type is AccountType.Eos) {
            useCount += 1

            kit?.let { return it }
            val networkType = if (testMode)
                EosKit.NetworkType.TestNet else
                EosKit.NetworkType.MainNet

            kit = EosKit.instance(App.instance, account.type.account, account.type.activePrivateKey, networkType, account.id)
            kit?.refresh()

            return kit!!
        }

        throw UnsupportedAccountException()
    }

    override fun unlink() {
        useCount -= 1

        if (useCount < 1) {
            kit?.stop()
            kit = null
        }
    }
}
