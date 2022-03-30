package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.send.SendModule
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
        val minimumSendAmount: BigDecimal?,
        val maximumSendAmount: BigDecimal?,
        val fee: BigDecimal,
        val addressError: Throwable?,
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

    private lateinit var availableBalance: BigDecimal
    private var fee: BigDecimal = BigDecimal.ZERO
    private var addressError: Throwable? = null

    var sendInputType: SendModule.InputType
        get() = localStorage.sendInputType ?: SendModule.InputType.COIN
        set(value) {
            localStorage.sendInputType = value
        }

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
                minimumSendAmount = minimumSendAmount,
                maximumSendAmount = maximumSendAmount,
                fee = fee,
                addressError = addressError,
                canBeSend = amount != null && address != null && addressError == null
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
        refreshFee()

        emitState()
    }

    fun setAddress(address: Address?) {
        this.address = address

        if (validateAddress()) {
            refreshFee()
            refreshMinimumSendAmount()
            refreshAvailableBalance()
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