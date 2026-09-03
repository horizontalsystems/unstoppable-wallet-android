package io.horizontalsystems.walletkit.modules.balance.token

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.walletkit.modules.balance.BalanceModule
import io.horizontalsystems.walletkit.modules.balance.BalanceXRateRepository
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            coinPrice = latestRates[wallet.coin.uid],
            warning = null
        )

        coroutineScope.launch {
            balanceItem = balanceItem?.copy(
                warning = balanceAdapterRepository.warning(wallet)
            )
        }
        coroutineScope.launch {
            xRateRepository.itemObservable.collect {
                handleXRateUpdate(it)
            }
        }
        coroutineScope.launch {
            balanceAdapterRepository.readyFlow.collect {
                handleAdapterUpdate()
            }
        }
        coroutineScope.launch {
            balanceAdapterRepository.updatesFlow.collect {
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
        )
    }

    override fun clear() {
        balanceAdapterRepository.clear()
    }
}
