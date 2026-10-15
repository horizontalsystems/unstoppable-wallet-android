package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.SpendPurpose
import io.horizontalsystems.walletkit.core.chain.spendBalance
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class TokenBalanceService(
    private val adapterManager: IAdapterManager,
    private val purpose: SpendPurpose,
) : ServiceState<TokenBalanceService.State>() {
    private var token: Token? = null
    private var amount: BigDecimal? = null
    private var adapter: IBalanceAdapter? = null
    private var balance: BigDecimal? = null
    private var error: Throwable? = null
    private var spendStep: ISwapProviderAction? = null

    override fun createState() = State(
        balance = balance,
        error = error,
        spendStep = spendStep,
    )

    fun setToken(token: Token?) {
        this.token = token

        refreshAvailableBalance()
        validate()

        emitState()
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        validate()

        emitState()
    }

    private fun validate() {
        error = null
        spendStep = null

        val amount = amount ?: return

        error = when (adapter?.balanceState) {
            null -> TokenNotEnabled()
            is AdapterState.SearchingTxs -> WalletSyncing()
            is AdapterState.Syncing -> WalletSyncing()
            is AdapterState.Downloading -> WalletSyncing()
            is AdapterState.Connecting -> WalletSyncing()
            is AdapterState.NotSynced -> WalletNotSynced()
            AdapterState.Synced -> {
                // The step covers everything between what is spendable now and the balance,
                // so the balance check below stays the same for every chain.
                spendStep = token?.let { ChainRegistry[it.blockchainType]?.spendStep(it, amount, purpose) }
                if (spendStep == null && amount > (balance ?: BigDecimal.ZERO)) {
                    SwapError.InsufficientBalanceFrom
                } else {
                    null
                }
            }
        }
    }

    fun refresh() {
        refreshAvailableBalance()
        validate()
        emitState()
    }

    private fun refreshAvailableBalance() {
        val token = token
        adapter = token?.let { adapterManager.getAdapterForToken<IBalanceAdapter>(it) }
        balance = token?.let { adapter?.spendBalance(it) }
    }

    /**
     * [balance] includes funds that need a step before they can be spent; an amount that
     * needs them comes with [spendStep] instead of an error.
     */
    data class State(
        val balance: BigDecimal?,
        val error: Throwable?,
        val spendStep: ISwapProviderAction? = null,
    )
}

class TokenNotEnabled : Exception()
class WalletSyncing : Exception()
class WalletNotSynced : Exception()
