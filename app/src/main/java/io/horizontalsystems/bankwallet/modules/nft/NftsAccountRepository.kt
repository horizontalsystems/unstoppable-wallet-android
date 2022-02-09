package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.orNull
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NftsAccountRepository(private val accountManager: IAccountManager) {
    private val _account = MutableStateFlow<Pair<Account, Address>?>(null)
    val account = _account.asStateFlow()

    private val disposables = CompositeDisposable()

    fun start() {
        handleActiveAccount(accountManager.activeAccount)

        accountManager.activeAccountObservable
            .subscribeIO {
                handleActiveAccount(it.orNull)
            }
            .let {
                disposables.add(it)
            }
    }

    fun stop() {
        disposables.clear()
    }

    private fun handleActiveAccount(account: Account?) {
        _account.update {
            account?.let {
                Pair(account, getAddress(account))
            }
        }
    }

    private fun getAddress(account: Account): Address {
        val addressStr = when (val type = account.type) {
            is AccountType.Address -> type.address
            is AccountType.Mnemonic -> Signer.address(type.seed, EthereumKit.NetworkType.EthMainNet).hex
            else -> throw Exception("Not Supported")
        }

        return Address(addressStr)
    }
}
