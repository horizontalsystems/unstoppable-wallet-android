package io.horizontalsystems.bankwallet.modules.send.zcash

import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SendZCashMemoService {
    val memoMaxLength = 120

    private var memo: String = ""
    private var addressType: ZcashAdapter.ZCashAddressType? = null
    private var memoIsAllowed = addressType == ZcashAdapter.ZCashAddressType.Shielded

    private val _stateFlow = MutableStateFlow(
        State(
            memo = if (memoIsAllowed) memo else "",
            memoIsAllowed = memoIsAllowed
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun setMemo(memo: String) {
        this.memo = memo

        emitState()
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                memo = if (memoIsAllowed) memo else "",
                memoIsAllowed = memoIsAllowed
            )
        }
    }

    fun setAddressType(addressType: ZcashAdapter.ZCashAddressType?) {
        this.addressType = addressType

        refreshMemoIsAllowed()

        emitState()
    }

    private fun refreshMemoIsAllowed() {
        memoIsAllowed = addressType == ZcashAdapter.ZCashAddressType.Shielded
    }

    data class State(
        val memo: String,
        val memoIsAllowed: Boolean
    )
}
