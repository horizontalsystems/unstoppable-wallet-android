package io.horizontalsystems.bankwallet.modules.depositcex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CexType
import io.horizontalsystems.bankwallet.modules.balance.cex.CoinzixCexDepositService
import io.horizontalsystems.bankwallet.modules.balance.cex.ICexDepositService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DepositViewModel(coinUid: String?) : ViewModel() {

    private val account = App.accountManager.activeAccount
    private val depositService: ICexDepositService
    var openCoinSelect: Boolean = false

    private var coins: List<DepositCexModule.CexCoinViewItem>? = null
    private var loading = false

    var uiState by mutableStateOf(
        DepositUiState(
            coins = coins,
            loading = loading
        )
    )
        private set

    private fun emitState() {
        viewModelScope.launch {
            uiState = DepositUiState(
                coins = coins,
                loading = loading
            )
        }
    }


    init {
        val cexType = (account?.type as? AccountType.Cex)?.cexType
        depositService = when (cexType) {
            is CexType.Binance -> TODO()
            is CexType.Coinzix -> {
                CoinzixCexDepositService(
                    cexType.authToken,
                    cexType.secret
                )
            }

            null -> TODO()
        }

        viewModelScope.launch(Dispatchers.IO) {
            loading = true
            emitState()

            coins = depositService.getCoins()
            loading = false
            emitState()
        }

        if (coinUid == null) {
            openCoinSelect = true
        }
    }

    fun setCoinUid(coinUid: String) {
        openCoinSelect = false
    }
}

data class DepositUiState(
    val coins: List<DepositCexModule.CexCoinViewItem>?,
    val loading: Boolean
)
