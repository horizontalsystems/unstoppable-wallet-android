package io.horizontalsystems.bankwallet.modules.balance.token

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class TokenBalanceService(
    private val wallet: Wallet,
    private val xRateRepository: BalanceXRateRepository,
    private val balanceAdapterRepository: BalanceAdapterRepository
) : Clearable {

    private val _balanceItemFlow = MutableStateFlow<BalanceModule.BalanceItem?>(null)
    val balanceItemFlow = _balanceItemFlow.asStateFlow()

    var balanceItem: BalanceModule.BalanceItem? = null
        private set(value) {
            field = value

            _balanceItemFlow.update { value }
        }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    val baseCurrency by xRateRepository::baseCurrency

    fun start() {
        balanceAdapterRepository.setWallet(listOf(wallet))
        xRateRepository.setCoinUids(listOf(wallet.coin.uid))

        val latestRates = xRateRepository.getLatestRates()

        balanceItem = BalanceModule.BalanceItem(
            wallet = wallet,
            balanceData = balanceAdapterRepository.balanceData(wallet),
            state = balanceAdapterRepository.state(wallet),
            sendAllowed = balanceAdapterRepository.sendAllowed(wallet),
            coinPrice = latestRates[wallet.coin.uid]
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
