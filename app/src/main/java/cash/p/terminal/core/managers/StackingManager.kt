package cash.p.terminal.core.managers

import android.util.Log
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.isCosanta
import cash.p.terminal.wallet.isPirateCash
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class StackingManager(
    private val piratePlaceRepository: PiratePlaceRepository,
    private val localStorageManager: LocalStorageManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _unpaidFlow = MutableStateFlow<BigDecimal?>(null)
    val unpaidFlow = _unpaidFlow.asStateFlow()

    private companion object {
        const val CACHE_DURATION = 60 * 60 * 1000L
    }

    fun loadInvestmentData(wallet: Wallet, address: String) {
        if (wallet.isPirateCash()) {
            loadInvestmentData(wallet, address, StackingType.PCASH.value.lowercase())
        } else if (wallet.isCosanta()) {
            loadInvestmentData(wallet, address, StackingType.COSANTA.value.lowercase())
        } else {
            _unpaidFlow.value = BigDecimal.ZERO
        }
    }

    private fun loadInvestmentData(wallet: Wallet, address: String, coin: String) {
        _unpaidFlow.value = null
        val cachedValue = localStorageManager.getStackingUnpaid(wallet)
        if (cachedValue != null) {
            _unpaidFlow.value = cachedValue
            if (System.currentTimeMillis() - localStorageManager.getStackingUpdateTimestamp(wallet) < CACHE_DURATION) {
                return
            }
        }
        scope.launch(
            CoroutineExceptionHandler { _, throwable ->
                Log.e("StackingManager", "Error loading investment data", throwable)
                _unpaidFlow.value = BigDecimal.ZERO
            }) {
            piratePlaceRepository.getInvestmentData(
                coin = coin.lowercase(),
                address = address
            ).unrealizedValue.toBigDecimal().also {
                localStorageManager.setStackingUnpaid(wallet, it)
                _unpaidFlow.value = it
            }
        }
    }
}
