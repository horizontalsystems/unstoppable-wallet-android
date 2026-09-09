package io.horizontalsystems.walletkit.modules.send.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.SendMemoSupport
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.multiswap.FiatService
import io.horizontalsystems.walletkit.modules.multiswap.TokenBalanceService
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

enum class SendTab { Standard, Private, CrossPay }

enum class SendInputType { Amount, Address }

sealed class SendStep {
    data class InputRequired(val inputType: SendInputType) : SendStep()
    data class Error(val error: Throwable) : SendStep()
    data object Proceed : SendStep()
}

data class SendUiState(
    val wallet: Wallet,
    val tab: SendTab,
    val amount: BigDecimal?,
    val fiatAmount: BigDecimal?,
    val fiatAmountInputEnabled: Boolean,
    val currency: Currency,
    val availableBalance: BigDecimal?,
    val address: Address?,
    val riskyAddress: Boolean,
    val memo: String?,
    val memoSupport: SendMemoSupport?,
    val percentOptions: List<Int>,
    val step: SendStep,
)

/**
 * Input state of the send screen for any blockchain: the selected tab, amount, recipient and
 * memo, plus the wallet's spendable balance, the amount's fiat equivalent and whether the
 * chain accepts a memo for the chosen recipient. The amount is checked against the balance
 * the same way swap does it; fee-dependent checks belong to the confirmation step, which
 * owns the transaction itself.
 */
class SendViewModel(
    val wallet: Wallet,
    private val currencyManager: CurrencyManager,
    private val adapterManager: IAdapterManager,
    private val fiatService: FiatService,
    private val balanceService: TokenBalanceService,
) : ViewModelUiState<SendUiState>() {

    private var currency = currencyManager.baseCurrency
    private var tab = SendTab.Standard
    private var amount: BigDecimal? = null
    private var fiatAmount: BigDecimal? = null
    private var fiatAmountInputEnabled = false
    private var address: Address? = null
    private var riskyAddress = false
    private var memo: String? = null
    private var memoSupport: SendMemoSupport? = null
    private var memoSupportJob: Job? = null
    private var balanceState = balanceService.stateFlow.value
    private var balanceJob: Job? = null
    private val chainPlugin = ChainRegistry[wallet.token.blockchainType]

    init {
        fiatService.setCurrency(currency)
        fiatService.setToken(wallet.token)
        // The service converts in both directions, so the coin amount is taken from it too:
        // typing a fiat value updates the coin amount and vice versa.
        viewModelScope.launch {
            fiatService.stateFlow.collect {
                amount = it.amount
                fiatAmount = it.fiatAmount
                fiatAmountInputEnabled = it.coinPrice != null && !it.coinPrice.expired
                balanceService.setAmount(amount)
                emitState()
            }
        }
        balanceService.setToken(wallet.token)
        viewModelScope.launch {
            balanceService.stateFlow.collect {
                balanceState = it
                emitState()
            }
        }
        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                currency = currencyManager.baseCurrency
                fiatService.setCurrency(currency)
                emitState()
            }
        }
        observeBalanceUpdates()
        refreshMemoSupport()
        // Adapters are recreated on account or network changes; re-resolve the adapter then.
        viewModelScope.launch {
            adapterManager.adaptersReadyFlow.collectSafely {
                observeBalanceUpdates()
            }
        }
    }

    // The balance service reads the adapter on demand; follow the adapter's own updates so
    // the balance and its validation stay current while the wallet syncs.
    private fun observeBalanceUpdates() {
        balanceJob?.cancel()
        balanceService.refresh()

        val adapter = adapterManager.getAdapterForWallet<IBalanceAdapter>(wallet) ?: return
        balanceJob = viewModelScope.launch {
            adapter.balanceUpdatedFlow.collectSafely {
                balanceService.refresh()
            }
        }
    }

    private fun refreshMemoSupport() {
        memoSupportJob?.cancel()
        memoSupportJob = viewModelScope.launch {
            memoSupport = chainPlugin?.sendMemoSupport(wallet.token, address?.hex)
            // A memo typed before the recipient turned out to have no memo field must not
            // linger in the state, or it would silently go nowhere.
            if (memoSupport == null) {
                memo = null
            }
            emitState()
        }
    }

    override fun createState() = SendUiState(
        wallet = wallet,
        tab = tab,
        amount = amount,
        fiatAmount = fiatAmount,
        fiatAmountInputEnabled = fiatAmountInputEnabled,
        currency = currency,
        availableBalance = balanceState.balance,
        address = address,
        riskyAddress = riskyAddress,
        memo = memo,
        memoSupport = memoSupport,
        percentOptions = percentOptions(),
        step = step(),
    )

    // The network fee is only estimated on the confirmation screen, so 100% of an asset
    // that also pays its own fee always ends in an insufficient balance error. Offer it
    // only for tokens whose fee is paid with a separate native asset.
    private fun percentOptions(): List<Int> {
        val feePaidFromAsset = when (wallet.token.type) {
            TokenType.Native,
            is TokenType.Derived,
            is TokenType.AddressTyped,
            is TokenType.Unsupported -> true
            else -> false
        }
        return if (feePaidFromAsset) listOf(25, 50, 75) else listOf(25, 50, 75, 100)
    }

    private fun step(): SendStep {
        balanceState.error?.let { return SendStep.Error(it) }
        val amount = amount
        if (amount == null || amount <= BigDecimal.ZERO) {
            return SendStep.InputRequired(SendInputType.Amount)
        }
        if (address == null) {
            return SendStep.InputRequired(SendInputType.Address)
        }
        return SendStep.Proceed
    }

    fun onSelectTab(tab: SendTab) {
        this.tab = tab
        emitState()
    }

    fun onEnterAmount(amount: BigDecimal?) {
        fiatService.setAmount(amount)
    }

    fun onEnterFiatAmount(fiatAmount: BigDecimal?) {
        fiatService.setFiatAmount(fiatAmount)
    }

    fun onEnterAmountPercentage(percentage: Int) {
        val availableBalance = balanceState.balance ?: return

        val amount = availableBalance
            .times(BigDecimal(percentage / 100.0))
            .setScale(wallet.token.decimals, RoundingMode.DOWN)
            .stripTrailingZeros()

        fiatService.setAmount(amount)
    }

    fun onSelectAddress(address: Address, risky: Boolean) {
        this.address = address
        this.riskyAddress = risky
        emitState()
        refreshMemoSupport()
    }

    fun onEnterMemo(memo: String) {
        this.memo = memo.ifBlank { null }
        emitState()
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendViewModel(
                wallet,
                App.currencyManager,
                App.adapterManager,
                FiatService(App.marketKit),
                TokenBalanceService(App.adapterManager),
            ) as T
        }
    }
}
