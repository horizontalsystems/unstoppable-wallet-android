package io.horizontalsystems.bankwallet.modules.watchaddress

import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WatchAddressService(
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator
) {
    var address: String = ""
        set(value) {
            field = value

            accountCreatedMutableFlow.update {
                DataState.Success(false)
            }
        }

    private val accountCreatedMutableFlow = MutableStateFlow<DataState<Boolean>>(DataState.Success(false))
    val accountCreatedFlow = accountCreatedMutableFlow.asStateFlow()

    fun createAccount() {
        try {
            AddressValidator.validate(address)
            val account = accountFactory.watchAccount(address, null)

            accountManager.save(account)
            walletActivator.activateWallets(account, listOf(CoinType.Ethereum, CoinType.BinanceSmartChain))

            accountCreatedMutableFlow.update {
                DataState.Success(true)
            }
        } catch (e: AddressValidator.AddressValidationException) {
            accountCreatedMutableFlow.update {
                DataState.Error(e)
            }
        }
    }
}
