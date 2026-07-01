package io.horizontalsystems.bankwallet.modules.send.bitcoin.utxoexpert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.util.Date

class UtxoExpertModeViewModel(
    private val adapter: ISendBitcoinAdapter,
    private val token: Token,
    initialCustomUnspentOutputs: List<UnspentOutputInfo>?,
    xRateService: XRateService,
) : ViewModelUiState<UtxoExpertModeModule.UiState>() {

    private var unspentOutputViewItems = listOf<UtxoExpertModeModule.UnspentOutputViewItem>()
    private var selectedUnspentOutputs = listOf<String>()
    private var coinRate = xRateService.getRate(token.coin.uid)
    private var availableBalanceInfo = UtxoExpertModeModule.InfoItem(
        value = App.numberFormatter.formatCoinFull(BigDecimal.ZERO, token.coin.code, token.decimals),
        subValue = "",
    )

    val customOutputs: List<UnspentOutputInfo>
        get() = adapter.unspentOutputs.filter { selectedUnspentOutputs.contains(getUnspentId(it)) }

    init {
        initialCustomUnspentOutputs?.forEach {
            selectedUnspentOutputs = selectedUnspentOutputs + getUnspentId(it)
        }
        xRateService.getRateFlow(token.coin.uid).collectWith(viewModelScope) {
            coinRate = it
            setUnspentOutputViewItems()
        }
        setAvailableBalanceInfo()
        setUnspentOutputViewItems()
        emitState()
    }

    override fun createState() = UtxoExpertModeModule.UiState(
        availableBalanceInfo = availableBalanceInfo,
        utxoItems = unspentOutputViewItems,
        unselectAllIsEnabled = selectedUnspentOutputs.isNotEmpty(),
    )

    private fun getUnspentId(unspentOutputInfo: UnspentOutputInfo) = "${unspentOutputInfo.transactionHash.toHexString()}-${unspentOutputInfo.outputIndex}"

    private fun setAvailableBalanceInfo() {
        var totalCoinValue = BigDecimal.ZERO
        adapter.unspentOutputs.map { utxo ->
            val utxoId = getUnspentId(utxo)
            if (selectedUnspentOutputs.isEmpty() || selectedUnspentOutputs.contains(utxoId)) {
                totalCoinValue += utxo.value.toBigDecimal().movePointLeft(token.decimals)
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
        unspentOutputViewItems = adapter.unspentOutputs.map { utxo ->
            val coinValue = utxo.value.toBigDecimal().movePointLeft(token.decimals)
            val id = getUnspentId(utxo)
            UtxoExpertModeModule.UnspentOutputViewItem(
                id = id,
                outputIndex = utxo.outputIndex,
                date = DateHelper.shortDate(Date(utxo.timestamp * 1000), "MMM d", "MM/dd/yyyy"),
                amountToken = App.numberFormatter.formatCoinFull(coinValue, token.coin.code, token.decimals),
                amountFiat = coinRate?.let { rate ->
                    rate.copy(value = coinValue.times(rate.value)).getFormattedFull()
                } ?: "",
                address = utxo.address ?: "",
                selected = selectedUnspentOutputs.contains(id),
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

}

object UtxoExpertModeModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val adapter: ISendBitcoinAdapter,
        private val token: Token,
        private val customUnspentOutputs: List<UnspentOutputInfo>?,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UtxoExpertModeViewModel(
                adapter = adapter,
                token = token,
                initialCustomUnspentOutputs = customUnspentOutputs,
                xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            ) as T
        }
    }

    data class UiState(
        val availableBalanceInfo: InfoItem,
        val utxoItems: List<UnspentOutputViewItem>,
        val unselectAllIsEnabled: Boolean,
    )

    data class InfoItem(
        val value: String?,
        val subValue: String?,
    )

    data class UnspentOutputViewItem(
        val id: String,
        val outputIndex: Int,
        val date: String,
        val amountToken: String,
        val amountFiat: String,
        val address: String,
        val selected: Boolean,
    )
}