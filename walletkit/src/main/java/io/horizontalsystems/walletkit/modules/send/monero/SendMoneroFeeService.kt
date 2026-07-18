package io.horizontalsystems.walletkit.modules.send.monero

import android.util.Log
import io.horizontalsystems.walletkit.core.ISendMoneroAdapter
import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.core.managers.ConnectivityManager
import io.horizontalsystems.walletkit.entities.Address
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.net.UnknownHostException

class SendMoneroFeeService(
    private val adapter: ISendMoneroAdapter,
    private val connectivityManager: ConnectivityManager,
) : ServiceState<SendMoneroFeeService.State>(), AutoCloseable {
    private var memo: String? = null
    private var address: Address? = null
    private var amount: BigDecimal? = null

    private var fee: BigDecimal? = null
    private var inProgress = false
    private var error: Throwable? = null
    private val mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var estimateFeeJob: Job? = null

    init {
        coroutineScope.launch {
            connectivityManager.networkAvailabilityFlow.collect {
                refreshFeeAndEmitState()
            }
        }
    }

    override fun createState() = State(
        fee = fee,
        inProgress = inProgress,
        error = error,
    )

    private fun refreshFeeAndEmitState() {
        val amount = amount
        val address = address
        val memo = memo

        estimateFeeJob?.cancel()
        estimateFeeJob = coroutineScope.launch {
            if (amount != null && address != null) {
                // the native fee estimator blocks on daemon RPC and cannot be
                // cancelled, so fail fast while there is no network at all
                if (!connectivityManager.isConnected) {
                    mutex.withLock {
                        fee = null
                        error = UnknownHostException()
                        inProgress = false
                        emitState()
                    }
                    return@launch
                }

                mutex.withLock {
                    inProgress = true
                    error = null
                    emitState()
                }

                delay(1000)

                var estimatedFee: BigDecimal? = null
                var lastError: Throwable? = null
                var attempts = 0
                while (attempts < MAX_FEE_ATTEMPTS) {
                    ensureActive()
                    try {
                        estimatedFee = adapter.estimateFee(amount, address.hex, memo)
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Log.e("SendMoneroFeeService", "estimateFee attempt ${attempts + 1} failed", e)
                        lastError = e
                        attempts++
                        if (attempts < MAX_FEE_ATTEMPTS) delay(500)
                    }
                }

                mutex.withLock {
                    fee = estimatedFee
                    error = lastError
                    inProgress = false
                    emitState()
                }
            } else {
                mutex.withLock {
                    fee = null
                    error = null
                    inProgress = false
                    emitState()
                }
            }
        }
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshFeeAndEmitState()
    }

    fun setAddress(address: Address?) {
        this.address = address

        refreshFeeAndEmitState()
    }

    fun setMemo(memo: String?) {
        this.memo = memo

        refreshFeeAndEmitState()
    }

    data class State(
        val fee: BigDecimal?,
        val inProgress: Boolean,
        val error: Throwable? = null,
    )

    companion object {
        private const val MAX_FEE_ATTEMPTS = 3
    }

    override fun close() {
        coroutineScope.cancel()
    }

}
