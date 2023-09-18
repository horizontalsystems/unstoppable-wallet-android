package cash.p.terminal.modules.withdrawcex.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CexWithdrawVerificationViewModel(private val withdrawId: String) : ViewModel() {
    var submitEnabled by mutableStateOf(false)
        private set

    fun onEnterEmailCode(v: String) {
        TODO("Not yet implemented")
    }

    fun onEnter2FaCode(v: String) {
        TODO("Not yet implemented")
    }

    fun submit() {
        TODO("Not yet implemented")
    }

}
