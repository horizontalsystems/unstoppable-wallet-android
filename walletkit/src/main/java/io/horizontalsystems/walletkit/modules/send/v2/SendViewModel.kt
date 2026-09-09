package io.horizontalsystems.walletkit.modules.send.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.Wallet
import java.math.BigDecimal

enum class SendTab { Standard, Private, CrossPay }

enum class SendInputType { Amount, Address }

sealed class SendStep {
    data class InputRequired(val inputType: SendInputType) : SendStep()
    data object Proceed : SendStep()
}

data class SendUiState(
    val wallet: Wallet,
    val tab: SendTab,
    val amount: BigDecimal?,
    val fiatAmount: BigDecimal?,
    val currency: Currency,
    val availableBalance: BigDecimal?,
    val address: Address?,
    val riskyAddress: Boolean,
    val memo: String?,
    val step: SendStep,
)

/**
 * Input state of the send screen for any blockchain: the selected tab, amount, recipient and
 * memo. Balance, fee and validation are not wired yet; the confirmation step will own the
 * transaction itself.
 */
class SendViewModel(
    val wallet: Wallet,
    currencyManager: CurrencyManager,
) : ViewModelUiState<SendUiState>() {

    private val currency = currencyManager.baseCurrency
    private var tab = SendTab.Standard
    private var amount: BigDecimal? = null
    private var address: Address? = null
    private var riskyAddress = false
    private var memo: String? = null

    override fun createState() = SendUiState(
        wallet = wallet,
        tab = tab,
        amount = amount,
        fiatAmount = null,
        currency = currency,
        availableBalance = null,
        address = address,
        riskyAddress = riskyAddress,
        memo = memo,
        step = step(),
    )

    private fun step(): SendStep {
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
        this.amount = amount
        emitState()
    }

    fun onSelectAddress(address: Address, risky: Boolean) {
        this.address = address
        this.riskyAddress = risky
        emitState()
    }

    fun onEnterMemo(memo: String) {
        this.memo = memo.ifBlank { null }
        emitState()
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendViewModel(wallet, App.currencyManager) as T
        }
    }
}
