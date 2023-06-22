package cash.p.terminal.modules.depositcex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.CexType
import cash.p.terminal.modules.balance.cex.CoinzixCexDepositService
import cash.p.terminal.modules.balance.cex.ICexDepositService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DepositViewModel(private var assetId: String?) : ViewModel() {

    private val account = App.accountManager.activeAccount
    private val depositService: ICexDepositService
    val openCoinSelect: Boolean
        get() = assetId == null

    private var coins: List<DepositCexModule.CexCoinViewItem>? = null
    private var loading = false
    private var networks: List<DepositCexModule.NetworkViewItem>? = null
    private var networkId: String? = null
    private var address: String? = null

    var uiState by mutableStateOf(
        DepositUiState(
            coins = coins,
            loading = loading,
            networks = networks,
            address = address,
        )
    )
        private set

    private fun emitState() {
        viewModelScope.launch {
            uiState = DepositUiState(
                coins = coins,
                loading = loading,
                networks = networks,
                address = address
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
    }

    fun onSelectAsset(v: String) {
        viewModelScope.launch(Dispatchers.IO) {
            assetId = v
            networks = null
            address = null
            emitState()

            networks = depositService.getNetworks(v)
            emitState()
        }
    }

    fun onSelectNetwork(networkViewItem: DepositCexModule.NetworkViewItem) {
        networkId = networkViewItem.title
        address = null
        emitState()

        viewModelScope.launch {
            address = assetId?.let {
                depositService.getAddress(it, networkId)
            }
            emitState()
        }
    }
}

data class DepositUiState(
    val coins: List<DepositCexModule.CexCoinViewItem>?,
    val loading: Boolean,
    val networks: List<DepositCexModule.NetworkViewItem>?,
    val address: String?
)
