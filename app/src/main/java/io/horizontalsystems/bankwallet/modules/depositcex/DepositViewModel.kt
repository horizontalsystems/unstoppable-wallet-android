package cash.p.terminal.modules.depositcex

import androidx.lifecycle.ViewModel

class DepositViewModel(coinId: String?) : ViewModel() {
    var openCoinSelect: Boolean = false

    init {
        if (coinId == null) {
            openCoinSelect = true
        }
    }

    fun setCoin(coinId: String) {
        openCoinSelect = false
    }
}
