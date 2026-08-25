package io.horizontalsystems.walletkit.modules.manageaccount.publickeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.hdwalletkit.Mnemonic

class PublicKeysViewModel(account: Account) : ViewModel() {

    var viewState by mutableStateOf(PublicKeysModule.ViewState())
        private set

    init {
        val chainKeyRows = ChainRegistry.all.flatMap { it.publicKeyRows(account) }

        viewState = PublicKeysModule.ViewState(
            chainKeyRows = chainKeyRows
        )
    }

}
