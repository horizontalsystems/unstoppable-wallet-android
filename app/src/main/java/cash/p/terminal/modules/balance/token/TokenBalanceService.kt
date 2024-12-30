package cash.p.terminal.modules.balance.token

import cash.p.terminal.wallet.Clearable
import cash.p.terminal.modules.balance.BalanceAdapterRepository
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.modules.balance.DefaultBalanceXRateRepository
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.models.CoinPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class TokenBalanceService(
    private val wallet: Wallet,
    private val xRateRepository: DefaultBalanceXRateRepository,
    private val balanceAdapterRepository: BalanceAdapterRepository
) : Clearable {

    private val _balanceItemFlow = MutableStateFlow<BalanceItem?>(null)
    val balanceItemFlow = _balanceItemFlow.asStateFlow()

    var balanceItem: BalanceItem? = null
        private set(value) {
            field = value

            _balanceItemFlow.update { value }
        }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    val baseCurrency by xRateRepository::baseCurrency

    suspend fun start() {
        balanceAdapterRepository.setWallet(listOf(wallet))
        xRateRepository.setCoinUids(listOf(wallet.coin.uid))

        val latestRates = xRateRepository.getLatestRates()

        balanceItem = BalanceItem(
            wallet = wallet,
            balanceData = balanceAdapterRepository.balanceData(wallet),
            state = balanceAdapterRepository.state(wallet),
            sendAllowed = balanceAdapterRepository.sendAllowed(wallet),
            coinPrice = latestRates[wallet.coin.uid],
            warning = balanceAdapterRepository.warning(wallet)
        )

        coroutineScope.launch {
            xRateRepository.itemObservable.asFlow().collect {
                handleXRateUpdate(it)
            }
        }
        coroutineScope.launch {
            balanceAdapterRepository.readyObservable.asFlow().collect {
                handleAdapterUpdate()
            }
        }
        coroutineScope.launch {
            balanceAdapterRepository.updatesObservable.asFlow().collect {
                handleAdapterUpdate()
            }
        }
    }

    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        balanceItem = balanceItem?.copy(
            coinPrice = latestRates[wallet.coin.uid]
        )
    }

    private fun handleAdapterUpdate() {
        balanceItem = balanceItem?.copy(
            balanceData = balanceAdapterRepository.balanceData(wallet),
            state = balanceAdapterRepository.state(wallet),
            sendAllowed = balanceAdapterRepository.sendAllowed(wallet)
        )
    }

    override fun clear() {
        balanceAdapterRepository.clear()
    }
}
