package io.horizontalsystems.bankwallet.modules.walletconnect.list

import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSessionManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Flowable

class WalletConnectListService(
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val sessionManager: WalletConnectSessionManager
) {

    val items: List<Item>
        get() = getItems(sessionManager.sessions)

    val itemsObservable: Flowable<List<Item>>
        get() = sessionManager.sessionsObservable.map { sessions ->
            getItems(sessions)
        }

    private fun getEvmAddress(chainId: Int, accountType: AccountType): Address? = when {
        accountType !is AccountType.Mnemonic -> null
        chainId == 1 -> EthereumKit.address(accountType.words, NetworkType.EthMainNet)
        chainId == 56 -> EthereumKit.address(accountType.words, NetworkType.BscMainNet)
        else -> null
    }

    private fun getItems(sessions: List<WalletConnectSession>): List<Item> {
        val items = mutableListOf<Item>()

        for (predefinedAccountType in predefinedAccountTypeManager.allTypes) {
            predefinedAccountTypeManager.account(predefinedAccountType)?.let { account ->
                val accountSessions = sessions.filter { it.accountId == account.id }
                if (accountSessions.isNotEmpty()) {
                    getEvmAddress(accountSessions.first().chainId, account.type)?.let { address ->
                        items.add(Item(predefinedAccountType, address, accountSessions))
                    }
                }
            }
        }

        return items
    }

    data class Item(
            val predefinedAccountType: PredefinedAccountType,
            val address: Address,
            val sessions: List<WalletConnectSession>
    )

}
