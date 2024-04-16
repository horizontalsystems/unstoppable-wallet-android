package io.horizontalsystems.bankwallet.modules.swap.allowance

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.math.BigDecimal
import java.math.BigInteger

class SwapAllowanceService(
    private val adapterManager: IAdapterManager,
    private val ethereumKit: EthereumKit
) {

    private var token: Token? = null
    private var spenderAddress: Address? = null

    var state: State? = null
        private set(value) {
            field = value
            _stateFlow.tryEmit(value)
        }

    private val _stateFlow =
        MutableSharedFlow<State?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val stateFlow = _stateFlow.asSharedFlow()

    private val disposables = CompositeDisposable()
    private var allowanceDisposable: Disposable? = null

    fun set(spenderAddress: Address?) {
        this.spenderAddress = spenderAddress
        sync()
    }

    fun set(token: Token?) {
        this.token = token
        sync()
    }

    fun revokeEvmData(): SendEvmData? {
        val token = token
        val adapter = token?.let { adapterManager.getAdapterForToken(it) } as? Eip20Adapter ?: return null
        val address = spenderAddress ?: return null

        return SendEvmData(adapter.eip20Kit.buildApproveTransactionData(address, BigInteger.ZERO))
    }

    fun approveData(dex: SwapMainModule.Dex, amount: BigDecimal): SwapMainModule.ApproveData? {
        val allowance = (state as? State.Ready)?.allowance
        val address = spenderAddress ?: return null

        return allowance?.let {
            token?.let { token ->
                SwapMainModule.ApproveData(
                    dex.blockchainType,
                    token,
                    address.hex,
                    amount,
                    allowance.value
                )
            }
        }

    }

    fun start() {
        ethereumKit.lastBlockHeightFlowable
            .subscribeOn(Schedulers.io())
            .subscribe {
                sync()
            }
            .let { disposables.add(it) }
    }

    fun onCleared() {
        disposables.clear()
        allowanceDisposable?.dispose()
    }

    private fun sync() {
        allowanceDisposable?.dispose()
        allowanceDisposable = null

        val address = spenderAddress
        val token = token
        val adapter = token?.let { adapterManager.getAdapterForToken(it) } as? Eip20Adapter

        if (address == null || token == null || adapter == null) {
            state = null
            return
        }

        if (state != null && state is State.Ready) {
            // no need to set loading, simply update to new allowance value
        } else {
            state = State.Loading
        }

        allowanceDisposable = adapter.allowance(address, DefaultBlockParameter.Latest)
            .subscribeOn(Schedulers.io())
            .subscribe({ allowance ->
                state = State.Ready(CoinValue(token, allowance))
            }, { error ->
                state = State.NotReady(error)
            })
    }

    //region models
    sealed class State {
        object Loading : State()
        class Ready(val allowance: CoinValue) : State()
        class NotReady(val error: Throwable) : State()

        override fun equals(other: Any?): Boolean {
            return when {
                this is Loading && other is Loading -> true
                this is Ready && other is Ready -> allowance == other.allowance
                else -> false
            }
        }

        override fun hashCode(): Int {
            return when (this) {
                is Loading -> Loading.javaClass.hashCode()
                is Ready -> allowance.hashCode()
                is NotReady -> error.hashCode()
            }
        }
    }

}
