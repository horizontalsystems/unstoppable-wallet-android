package io.horizontalsystems.walletkit.modules.send.monero.utxoexpert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendMoneroAdapter
import io.horizontalsystems.walletkit.core.MoneroUnspentOutput
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.helpers.DateHelper
import io.horizontalsystems.walletkit.modules.send.bitcoin.utxoexpert.UtxoExpertModeModule
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Date

class MoneroUtxoExpertModeViewModel(
    adapter: ISendMoneroAdapter,
    private val token: Token,
    initialCustomUnspentOutputs: List<MoneroUnspentOutput>?,
    xRateService: XRateService,
) : ViewModelUiState<UtxoExpertModeModule.UiState>() {

    private var unspentOutputs = listOf<MoneroUnspentOutput>()
    private var unspentOutputViewItems = listOf<UtxoExpertModeModule.UnspentOutputViewItem>()
    private var selectedUnspentOutputs = listOf<String>()
    private var coinRate = xRateService.getRate(token.coin.uid)
    private var availableBalanceInfo = UtxoExpertModeModule.InfoItem(
        value = App.numberFormatter.formatCoinFull(BigDecimal.ZERO, token.coin.code, token.decimals),
        subValue = "",
    )

    val customOutputs: List<MoneroUnspentOutput>
        get() = unspentOutputs.filter { selectedUnspentOutputs.contains(it.keyImage) }

    init {
        initialCustomUnspentOutputs?.forEach {
            selectedUnspentOutputs = selectedUnspentOutputs + it.keyImage
        }
        xRateService.getRateFlow(token.coin.uid).collectWith(viewModelScope) {
            coinRate = it
            setUnspentOutputViewItems()
            emitState()
        }
        // the output list comes from JNI and must not be fetched on the main thread
        viewModelScope.launch(Dispatchers.IO) {
            unspentOutputs = adapter.getUnspentOutputs()
            setAvailableBalanceInfo()
            setUnspentOutputViewItems()
            emitState()
        }
        emitState()
    }

    override fun createState() = UtxoExpertModeModule.UiState(
        availableBalanceInfo = availableBalanceInfo,
        utxoItems = unspentOutputViewItems,
        unselectAllIsEnabled = selectedUnspentOutputs.isNotEmpty(),
    )

    private fun setAvailableBalanceInfo() {
        var totalCoinValue = BigDecimal.ZERO
        unspentOutputs.forEach { utxo ->
            if (selectedUnspentOutputs.isEmpty() || selectedUnspentOutputs.contains(utxo.keyImage)) {
                totalCoinValue += utxo.amount
            }
        }
        availableBalanceInfo = availableBalanceInfo.copy(
            value = App.numberFormatter.formatCoinFull(totalCoinValue, token.coin.code, token.decimals),
            subValue = coinRate?.let { rate ->
                rate.copy(value = totalCoinValue.times(rate.value)).getFormattedFull()
            } ?: "",
        )
    }

    private fun setUnspentOutputViewItems() {
        unspentOutputViewItems = unspentOutputs.map { utxo ->
            UtxoExpertModeModule.UnspentOutputViewItem(
                id = utxo.keyImage,
                outputIndex = 0,
                date = utxo.timestamp?.let {
                    DateHelper.shortDate(Date(it * 1000), "MMM d", "MM/dd/yyyy")
                } ?: "",
                amountToken = App.numberFormatter.formatCoinFull(utxo.amount, token.coin.code, token.decimals),
                amountFiat = coinRate?.let { rate ->
                    rate.copy(value = utxo.amount.times(rate.value)).getFormattedFull()
                } ?: "",
                address = utxo.address,
                selected = selectedUnspentOutputs.contains(utxo.keyImage),
            )
        }
    }

    private fun updateUtxoSelectedState() {
        unspentOutputViewItems = unspentOutputViewItems.map { utxo ->
            utxo.copy(selected = selectedUnspentOutputs.contains(utxo.id))
        }
    }

    fun onUnspentOutputClicked(id: String) {
        selectedUnspentOutputs = if (selectedUnspentOutputs.contains(id)) {
            selectedUnspentOutputs.filter { it != id }
        } else {
            selectedUnspentOutputs + id
        }
        updateUtxoSelectedState()
        setAvailableBalanceInfo()
        emitState()
    }

    fun unselectAll() {
        selectedUnspentOutputs = listOf()
        updateUtxoSelectedState()
        setAvailableBalanceInfo()
        emitState()
    }

    fun selectAll() {
        selectedUnspentOutputs = unspentOutputViewItems.map { it.id }
        updateUtxoSelectedState()
        setAvailableBalanceInfo()
        emitState()
    }

    class Factory(
        private val adapter: ISendMoneroAdapter,
        private val token: Token,
        private val customUnspentOutputs: List<MoneroUnspentOutput>?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MoneroUtxoExpertModeViewModel(
                adapter = adapter,
                token = token,
                initialCustomUnspentOutputs = customUnspentOutputs,
                xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            ) as T
        }
    }
}
