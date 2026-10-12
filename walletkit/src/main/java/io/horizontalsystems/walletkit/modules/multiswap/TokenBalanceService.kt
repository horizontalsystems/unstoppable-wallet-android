package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.SpendPurpose
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
    private var spendableBalance: BigDecimal? = null
    private var preparableBalance: BigDecimal? = null
    private var error: Throwable? = null
    private var preparationAction: ISwapProviderAction? = null

    override fun createState() = State(
        balance = spendableBalance?.let { it + (preparableBalance ?: BigDecimal.ZERO) },
        error = error,
        preparationAction = preparationAction,
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
        preparationAction = null

        val amount = amount ?: return

        error = when (adapter?.balanceState) {
            null -> TokenNotEnabled()
            is AdapterState.SearchingTxs -> WalletSyncing()
            is AdapterState.Syncing -> WalletSyncing()
            is AdapterState.Downloading -> WalletSyncing()
            is AdapterState.Connecting -> WalletSyncing()
            is AdapterState.NotSynced -> WalletNotSynced()
            AdapterState.Synced -> {
                val spendable = spendableBalance ?: BigDecimal.ZERO
                if (amount <= spendable) {
                    null
                } else {
                    val token = token
                    val action = if (token != null && amount <= spendable + (preparableBalance ?: BigDecimal.ZERO)) {
                        ChainRegistry[token.blockchainType]?.balancePreparationAction(token, amount, purpose)
                    } else {
                        null
                    }
                    preparationAction = action
                    if (action == null) SwapError.InsufficientBalanceFrom else null
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
        val plugin = token?.let { ChainRegistry[it.blockchainType] }
        spendableBalance = adapter?.let { plugin?.spendableBalance(token) ?: it.balanceData?.available }
        preparableBalance = token?.let { plugin?.preparableBalance(it) }
    }

    /**
     * [balance] includes funds that need a preparation step before they can be spent; an
     * amount that needs them comes with [preparationAction] instead of an error.
     */
    data class State(
        val balance: BigDecimal?,
        val error: Throwable?,
        val preparationAction: ISwapProviderAction? = null,
    )
}

class TokenNotEnabled : Exception()
class WalletSyncing : Exception()
class WalletNotSynced : Exception()
