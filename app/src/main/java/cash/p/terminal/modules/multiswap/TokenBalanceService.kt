package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.core.ServiceState
import cash.p.terminal.core.getFeeTokenBalance
import cash.p.terminal.core.isNative
import cash.p.terminal.modules.send.zcash.calculateZcashAvailableToSend
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TokenBalanceService(
    private val adapterManager: IAdapterManager,
    private val marketKit: MarketKitWrapper,
) : ServiceState<TokenBalanceService.State>() {
    private var token: Token? = null
    private var amount: BigDecimal? = null
    private var adapter: IBalanceAdapter? = null
    private var balance: BigDecimal? = null
    private var displayBalance: BigDecimal? = null
    private var error: Throwable? = null

    private var fee: BigDecimal? = null
    private var feeToken: Token? = null
    private var feeCoinBalance: BigDecimal? = null
    private var insufficientFeeBalance: Boolean = false
    private var coroutineScope: CoroutineScope? = null
    private var balanceUpdateJob: Job? = null
    private var feeUpdateJob: Job? = null

    override fun createState() = State(
        balance = balance,
        displayBalance = displayBalance,
        error = error,
        fee = fee,
        feeToken = feeToken,
        feeCoinBalance = feeCoinBalance,
        insufficientFeeBalance = insufficientFeeBalance,
    )

    fun setToken(token: Token?) {
        this.token = token

        refreshAvailableBalance()
        refreshSubscriptions()
        validate()

        emitState()
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        validate()

        emitState()
    }

    fun start(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
        refreshSubscriptions()
    }

    private fun refreshSubscriptions() {
        balanceUpdateJob?.cancel()
        feeUpdateJob?.cancel()

        val currentAdapter = adapter ?: return
        val scope = coroutineScope ?: return

        balanceUpdateJob = scope.launch {
            currentAdapter.balanceUpdatedFlow.collect {
                refreshBalanceState()
            }
        }

        val zcashAdapter = currentAdapter as? ISendZcashAdapter
        if (token?.blockchainType == BlockchainType.Zcash && zcashAdapter != null) {
            feeUpdateJob = scope.launch {
                zcashAdapter.fee.collect {
                    refreshBalanceState()
                }
            }
        }
    }

    private fun refreshBalanceState() {
        refreshAvailableBalance()
        validate()
        emitState()
    }

    private fun validate() {
        error = null
        insufficientFeeBalance = false

        val amount = amount ?: return

        error = when (adapter?.balanceState) {
            null -> null
            is AdapterState.Connecting -> WalletSyncing()
            is AdapterState.SearchingTxs -> WalletSyncing()
            is AdapterState.Syncing -> WalletSyncing()
            is AdapterState.NotSynced -> WalletNotSynced()
            AdapterState.Synced -> {
                if (amount > balance) {
                    SwapError.InsufficientBalanceFrom
                } else {
                    null
                }
            }
        }

        // Fee balance validation (only when no other error)
        if (error == null) {
            val currentFee = fee ?: return
            val currentToken = token ?: return

            insufficientFeeBalance = if (currentToken.type.isNative) {
                false // fee is already accounted for in the native available-to-spend balance
            } else {
                val currentFeeCoinBalance = feeCoinBalance ?: BigDecimal.ZERO
                currentFee > currentFeeCoinBalance
            }
        }
    }

    private fun refreshAvailableBalance() {
        val currentAdapter =
            token?.let { adapterManager.getAdapterForToken<IBalanceAdapter>(it) }
        adapter = currentAdapter
        val currentToken = token
        val adjusted = token?.let { adapterManager.getAdjustedBalanceDataForToken(it)?.available }
        balance = getAvailableBalance(currentToken, currentAdapter, adjusted)

        displayBalance = adjusted

        if (currentToken != null) {
            feeToken = marketKit.token(TokenQuery(currentToken.blockchainType, TokenType.Native))
                ?: currentToken

            if (currentToken.type.isNative) {
                fee = currentAdapter?.fee?.value
                feeCoinBalance = null
            } else {
                val feeTokenAdapter = feeToken?.let {
                    adapterManager.getAdapterForToken(it) as? IBalanceAdapter
                }
                fee = feeTokenAdapter?.fee?.value
                feeCoinBalance = feeToken?.let {
                    adapterManager.getFeeTokenBalance(it, currentToken)
                } ?: feeTokenAdapter?.balanceData?.available
            }
        } else {
            feeToken = null
            fee = null
            feeCoinBalance = null
        }
    }

    private fun getAvailableBalance(
        token: Token?,
        adapter: IBalanceAdapter?,
        adjustedAvailable: BigDecimal?,
    ): BigDecimal? {
        val zcashAdapter = adapter as? ISendZcashAdapter
        if (token?.blockchainType == BlockchainType.Zcash && zcashAdapter != null) {
            return calculateZcashAvailableToSend(
                adjustedAvailable = adjustedAvailable,
                adapterAvailable = zcashAdapter.balanceData.available,
                fee = zcashAdapter.fee.value
            )
        }

        val adapterAvailableBalance = adapter?.maxSpendableBalance
        return if (adjustedAvailable != null && adapterAvailableBalance != null) {
            minOf(adjustedAvailable, adapterAvailableBalance)
        } else {
            adjustedAvailable ?: adapterAvailableBalance
        }
    }

    data class State(
        val balance: BigDecimal?,
        val displayBalance: BigDecimal?,
        val error: Throwable?,
        val fee: BigDecimal?,
        val feeToken: Token?,
        val feeCoinBalance: BigDecimal?,
        val insufficientFeeBalance: Boolean,
    )
}

class WalletSyncing : Exception()
class WalletNotSynced : Exception()
