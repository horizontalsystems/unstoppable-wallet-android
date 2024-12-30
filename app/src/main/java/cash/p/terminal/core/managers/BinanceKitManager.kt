package cash.p.terminal.core.managers

import cash.p.terminal.core.App
import cash.p.terminal.core.IBinanceKitManager
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.binancechainkit.BinanceChainKit

class BinanceKitManager : IBinanceKitManager {
    private var kit: BinanceChainKit? = null
    private var useCount = 0
    private var currentAccount: cash.p.terminal.wallet.Account? = null

    override val binanceKit: BinanceChainKit?
        get() = kit

    override val statusInfo: Map<String, Any>?
        get() = kit?.statusInfo()

    override fun binanceKit(wallet: cash.p.terminal.wallet.Wallet): BinanceChainKit {
        val account = wallet.account
        val accountType = account.type

        if (kit != null && currentAccount != account) {
            kit?.stop()
            kit = null
            currentAccount = null
        }

        if (kit == null) {
            if (accountType !is cash.p.terminal.wallet.AccountType.Mnemonic)
                throw UnsupportedAccountException()

            useCount = 0

            kit = createKitInstance( accountType, account)
            currentAccount = account
        }

        useCount++
        return kit!!
    }

    private fun createKitInstance(accountType: cash.p.terminal.wallet.AccountType.Mnemonic, account: cash.p.terminal.wallet.Account): BinanceChainKit {
        val networkType = BinanceChainKit.NetworkType.MainNet

        val kit = BinanceChainKit.instance(App.instance, accountType.words, accountType.passphrase, account.id, networkType)
        kit.refresh()

        return kit
    }

    override fun unlink(account: cash.p.terminal.wallet.Account) {
        if (currentAccount != account) return

        useCount -= 1

        if (useCount < 1) {
            kit?.stop()
            kit = null
            currentAccount = null
        }
    }

}
