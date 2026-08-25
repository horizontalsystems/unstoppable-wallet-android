package io.horizontalsystems.walletkit.modules.manageaccount.privatekeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.hdwalletkit.Mnemonic

class PrivateKeysViewModel(account: Account) : ViewModel() {

    var viewState by mutableStateOf(PrivateKeysModule.ViewState())
        private set

    init {

        val hdExtendedKey = (account.type as? AccountType.HdExtendedKey)?.hdExtendedKey

        val chainKeyRows = ChainRegistry.all.flatMap { it.privateKeyRows(account) }

        viewState = PrivateKeysModule.ViewState(
            chainKeyRows = chainKeyRows
        )
    }
}
