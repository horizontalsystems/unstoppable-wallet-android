package io.horizontalsystems.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.marketkit.models.FullCoin
import javax.inject.Inject

@HiltViewModel
class ReceiveSharedViewModel @Inject constructor(
    private val accountManager: IAccountManager,
    private val marketKit: MarketKitWrapper,
) : ViewModel() {

    var coinUid: String? = null

    val activeAccount: Account?
        get() = accountManager.activeAccount

    fun fullCoin(): FullCoin? {
        val coinUid = coinUid ?: return null
        return marketKit.fullCoins(listOf(coinUid)).firstOrNull()
    }

}