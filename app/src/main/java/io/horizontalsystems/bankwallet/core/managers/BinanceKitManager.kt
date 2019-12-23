package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IBinanceKitManager
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.binancechainkit.BinanceChainKit

class BinanceKitManager(appConfig: IAppConfigProvider) : IBinanceKitManager {
    private var kit: BinanceChainKit? = null
    private var useCount = 0
    private val testMode = appConfig.testMode

    override val binanceKit: BinanceChainKit?
        get() = kit

    override val statusInfo: Map<String, Any>?
        get() = kit?.statusInfo()

    override fun binanceKit(wallet: Wallet): BinanceChainKit {
        val account = wallet.account
        if (account.type is AccountType.Mnemonic && account.type.words.size == 24) {
            useCount += 1

            kit?.let { return it }
            val networkType = if (testMode)
                BinanceChainKit.NetworkType.TestNet else
                BinanceChainKit.NetworkType.MainNet

            kit = BinanceChainKit.instance(App.instance, account.type.words, account.id, networkType)
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
