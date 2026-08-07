package io.horizontalsystems.walletkit.modules.send.bitcoin.utxoexpert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.toHexString
import io.horizontalsystems.walletkit.helpers.DateHelper
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.util.Date
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            xRateService.getRateFlow(token.coin.uid).collect {
                coinRate = it
                setUnspentOutputViewItems()
            }
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


@Suppress("UNCHECKED_CAST")
class UtxoExpertModeFactory(
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
