package cash.p.terminal.modules.send.bitcoin

import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.isLitecoinMweb
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class SendBitcoinAmountService(
    private val adapter: ISendBitcoinAdapter,
    private val coinCode: String,
    private val amountValidator: AmountValidator,
    private val adapterManager: IAdapterManager,
    private val wallet: Wallet
) {
    private var amount: BigDecimal? = null
    private var customUnspentOutputs: List<UnspentOutputInfo>? = null
    private var amountCaution: HSCaution? = null

    private var minimumSendAmount: BigDecimal? = null
    private var userMinimumSendAmount: BigDecimal? = null
    private var availableBalance: BigDecimal? = null
    private var validAddress: Address? = null
    private var memo: String? = null
    private var feeRate: Int? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private var changeToFirstInput = false
    private var utxoFilters = UtxoFilters()

    private val _stateFlow = MutableStateFlow(
        State(
            amount = amount,
            amountCaution = amountCaution,
            availableBalance = availableBalance,
            canBeSend = false,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private fun emitState() {
        val tmpAmount = amount
        val tmpAmountCaution = amountCaution

        val canBeSend = availableBalance != null
                && tmpAmount != null && tmpAmount > BigDecimal.ZERO
                && (tmpAmountCaution == null || tmpAmountCaution.isWarning())

        _stateFlow.update {
            State(
                amount = amount,
                amountCaution = amountCaution,
                availableBalance = availableBalance,
                canBeSend = canBeSend
            )
        }
    }

    private fun updateAvailableBalance() {
        val dynamicBalance = feeRate?.let {
            adapter.availableBalance(
                it,
                validAddress?.hex,
                memo,
                customUnspentOutputs,
                pluginData,
                changeToFirstInput,
                utxoFilters
            )
        }
        val adjustedBalance = adapterManager.getAdjustedBalanceData(wallet)?.available
        availableBalance = displayedAvailableBalance(dynamicBalance, adjustedBalance)
    }

    private fun displayedAvailableBalance(
        dynamicBalance: BigDecimal?,
        adjustedBalance: BigDecimal?
    ): BigDecimal? {
        if (dynamicBalance == null) return null
        if (adjustedBalance == null) return dynamicBalance

        // MWEB dry-run can temporarily return 0 while local unconfirmed change is waiting for a block.
        if (wallet.token.isLitecoinMweb && dynamicBalance.signum() == 0 && adjustedBalance.signum() > 0) {
            return adjustedBalance
        }

        return minOf(dynamicBalance, adjustedBalance)
    }

    private fun refreshAmountState(
        forceEmit: Boolean = true,
        afterBalanceUpdate: () -> Unit = {}
    ) {
        updateAvailableBalance()
        afterBalanceUpdate()
        validateAmount()

        if (forceEmit) {
            emitState()
        }
    }

    fun refreshAvailableBalance() {
        refreshAmountState()
    }

    private fun refreshMinimumSendAmount() {
        minimumSendAmount = adapter.minimumSendAmount(validAddress?.hex)
    }

    private fun validateAmount() {
        availableBalance?.let {
            val mins = listOfNotNull(minimumSendAmount, userMinimumSendAmount)
            amountCaution = amountValidator.validate(
                amount,
                coinCode,
                it,
                mins.maxOrNull(),
            )
        }
    }

    fun setAmount(amount: BigDecimal?, forceEmit: Boolean = true) {
        this.amount = amount

        validateAmount()

        if (forceEmit) {
            emitState()
        }
    }

    fun setValidAddress(validAddress: Address?) {
        this.validAddress = validAddress

        refreshAmountState {
            refreshMinimumSendAmount()
        }
    }

    fun setFeeRate(feeRate: Int?) {
        this.feeRate = feeRate

        refreshAmountState()
    }

    fun setPluginData(pluginData: Map<Byte, IPluginData>?) {
        this.pluginData = pluginData

        refreshAmountState()
    }

    fun setUserMinimumSendAmount(userMinimumSendAmount: Int?, forceEmit: Boolean = true) {
        this.userMinimumSendAmount = userMinimumSendAmount?.let {
            adapter.satoshiToBTC(it.toLong())
        }

        validateAmount()

        if (forceEmit) {
            emitState()
        }
    }

    fun setChangeToFirstInput(changeToFirstInput: Boolean, forceEmit: Boolean = true) {
        this.changeToFirstInput = changeToFirstInput

        refreshAmountState(forceEmit = forceEmit)
    }

    fun setUtxoFilters(utxoFilters: UtxoFilters, forceEmit: Boolean = true) {
        this.utxoFilters = utxoFilters

        refreshAmountState(forceEmit = forceEmit)
    }

    fun setCustomUnspentOutputs(customUnspentOutputs: List<UnspentOutputInfo>?) {
        this.customUnspentOutputs = customUnspentOutputs
        refreshAmountState()
    }

    fun setMemo(memo: String?, forceEmit: Boolean = true) {
        this.memo = memo

        refreshAmountState(forceEmit = forceEmit)
    }

    data class State(
        val amount: BigDecimal?,
        val amountCaution: HSCaution?,
        val availableBalance: BigDecimal?,
        val canBeSend: Boolean
    )
}
