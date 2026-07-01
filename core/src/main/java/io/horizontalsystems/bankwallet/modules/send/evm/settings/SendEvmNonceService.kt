package io.horizontalsystems.bankwallet.modules.send.evm.settings

import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsError
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext

class SendEvmNonceService(
    private val evmKit: EthereumKit,
    private val initialNonce: Long? = null,
) {
    private var fixedNonce: Long? = null
    private var latestNonce: Long? = null

    var state: DataState<State> = DataState.Loading
        private set(value) {
            field = value
            _stateFlow.update { value }
        }

    private val _stateFlow = MutableStateFlow(state)
    val stateFlow: Flow<DataState<State>> = _stateFlow

    suspend fun start() {
        try {
            latestNonce = evmKit.getNonce(DefaultBlockParameter.Latest).await()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        val fixedNonce = fixedNonce

        when {
            fixedNonce != null -> sync(fixedNonce)
            initialNonce != null -> sync(initialNonce)
            else -> setRecommended()
        }
    }

    fun fixNonce(nonce: Long) {
        fixedNonce = nonce

        sync(nonce)
    }

    suspend fun reset() {
        setRecommended()
    }

    fun setNonce(nonce: Long) {
        sync(nonce)
    }

    fun increment() {
        state.dataOrNull?.let { currentState ->
            sync(currentState.nonce + 1)
        }
    }

    fun decrement() {
        state.dataOrNull?.let { currentState ->
            sync(currentState.nonce - 1)
        }
    }

    private fun sync(nonce: Long, default: Boolean = false) {
        val fixedNonce = fixedNonce
        state = if (fixedNonce != null) {
            DataState.Success(State(nonce = fixedNonce, default = true, fixed = true))
        } else {
            DataState.Success(State(nonce = nonce, default = default, errors = errors(nonce), fixed = false))
        }
    }

    private fun errors(nonce: Long): List<FeeSettingsError> {
        return latestNonce?.let {
            if (it > nonce) {
                listOf(FeeSettingsError.UsedNonce)
            } else {
                listOf()
            }
        } ?: listOf()
    }

    private suspend fun setRecommended() = withContext(Dispatchers.IO) {
        try {
            val nonce = evmKit.getNonce(DefaultBlockParameter.Pending).await()
            sync(nonce, default = true)
        } catch (e: Throwable) {
            state = DataState.Error(e)
        }
    }

    data class State(
        val nonce: Long,
        val default: Boolean,
        val fixed: Boolean,
        val warnings: List<Warning> = listOf(),
        val errors: List<Throwable> = listOf()
    )
}
