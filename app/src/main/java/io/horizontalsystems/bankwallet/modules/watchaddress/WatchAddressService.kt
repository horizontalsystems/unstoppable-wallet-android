package io.horizontalsystems.bankwallet.modules.watchaddress

import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.CoinType

class WatchAddressService(
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator
) {
    var address: Address? = null

    val isCreatable
        get() = address != null

    fun createAccount() {
        val tmpAddress = address ?: throw EmptyAddressException()

        val account = accountFactory.watchAccount(tmpAddress.hex, tmpAddress.domain)
        accountManager.save(account)
        walletActivator.activateWallets(account, listOf(CoinType.Ethereum, CoinType.BinanceSmartChain))
    }
}

class EmptyAddressException : Exception()