package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class SendBitcoinService(
    private val adapter: ISendBitcoinAdapter,
    private val localStorage: ILocalStorage,
    private val coinType: CoinType
) {
    data class ServiceState(
        val availableBalance: BigDecimal,
        val fee: BigDecimal,
        val addressError: Throwable?,
        val amountError: Throwable?,
        val canBeSend: Boolean,
    )
    private var feeRate: Long = 1 // todo
    private var amount: BigDecimal? = null
    private var address: Address? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private var _stateFlow = MutableStateFlow<ServiceState?>(null)
    val stateFlow = _stateFlow.filterNotNull()

    private var minimumSendAmount: BigDecimal? = null
    private var maximumSendAmount: BigDecimal? = null

    private var availableBalance: BigDecimal = BigDecimal.ZERO
    private var fee: BigDecimal = BigDecimal.ZERO
    private var addressError: Throwable? = null
    private var amountError: Throwable? = null

    fun start() {
        adapter.balanceData

        refreshMinimumSendAmount()
        refreshMaximumSendAmount()

        refreshAvailableBalance()
        refreshFee()

        emitState()
//        adapter.send()
    }

    private fun emitState() {
        _stateFlow.update {
            ServiceState(
                availableBalance = availableBalance,
                fee = fee,
                addressError = addressError,
                amountError = amountError,
                canBeSend = amount != null && amountError == null && address != null && addressError == null
            )
        }
    }

    private fun refreshAvailableBalance() {
        availableBalance = adapter.availableBalance(feeRate, address?.hex, pluginData)
    }

    private fun refreshFee() {
        fee = amount?.let {
            adapter.fee(it, feeRate, address?.hex, pluginData)
        } ?: BigDecimal.ZERO
    }

    private fun refreshMaximumSendAmount() {
        maximumSendAmount = pluginData?.let { adapter.maximumSendAmount(it) }
    }

    private fun refreshMinimumSendAmount() {
        minimumSendAmount = adapter.minimumSendAmount(address?.hex)
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        if (validateAmount()) {
            refreshFee()
        }

        emitState()
    }

    private fun validateAmount(): Boolean {
        val tmpCoinAmount = amount
        val tmpMinimumSendAmount = minimumSendAmount
        val tmpMaximumSendAmount = maximumSendAmount

        amountError = when {
            tmpCoinAmount == null -> null
            tmpCoinAmount == BigDecimal.ZERO -> null
            tmpCoinAmount > availableBalance -> {
                Error(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
            }
            tmpMinimumSendAmount != null && tmpCoinAmount < tmpMinimumSendAmount -> {
                Error(Translator.getString(R.string.Send_Error_MinimumAmount, tmpMinimumSendAmount))
            }
            tmpMaximumSendAmount != null && tmpCoinAmount < tmpMaximumSendAmount -> {
                Error(Translator.getString(R.string.Send_Error_MaximumAmount, tmpMaximumSendAmount))
            }
            else -> null
        }

        return amountError == null
    }

    fun setAddress(address: Address?) {
        this.address = address

        if (validateAddress()) {
            refreshMinimumSendAmount()
            refreshAvailableBalance()
            if (validateAmount()) {
                refreshFee()
            }
        }

        emitState()
    }

    private fun validateAddress(): Boolean {
        addressError = null
        val address = this.address ?: return true

        try {
            adapter.validate(address.hex, pluginData)
        } catch (e: Exception) {
            addressError = e
        }

        return addressError == null
    }

}