package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IBinanceKitManager
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.binancechainkit.BinanceChainKit

class BinanceKitManager : IBinanceKitManager {
    private var kit: BinanceChainKit? = null
    private var useCount = 0
    private var currentAccount: Account? = null

    override val binanceKit: BinanceChainKit?
        get() = kit

    override val statusInfo: Map<String, Any>?
        get() = kit?.statusInfo()

    override fun binanceKit(wallet: Wallet): BinanceChainKit {
        val account = wallet.account
        val accountType = account.type

        if (kit != null && currentAccount != account) {
            kit?.stop()
            kit = null
            currentAccount = null
        }

        if (kit == null) {
            if (accountType !is AccountType.Mnemonic)
                throw UnsupportedAccountException()

            useCount = 0

            kit = createKitInstance( accountType, account)
            currentAccount = account
        }

        useCount++
        return kit!!
    }

    private fun createKitInstance(accountType: AccountType.Mnemonic, account: Account): BinanceChainKit {
        val networkType = BinanceChainKit.NetworkType.MainNet

        val kit = BinanceChainKit.instance(App.instance, accountType.words, accountType.passphrase, account.id, networkType)
        kit.refresh()

        return kit
    }

    override fun unlink(account: Account) {
        if (currentAccount != account) return

        useCount -= 1

        if (useCount < 1) {
            kit?.stop()
            kit = null
            currentAccount = null
        }
    }

}
