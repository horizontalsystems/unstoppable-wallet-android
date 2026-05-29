package io.horizontalsystems.bankwallet.modules.send.bitcoin.utxoexpert

import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.util.Date

@HiltViewModel(assistedFactory = UtxoExpertModeViewModel.Factory::class)
class UtxoExpertModeViewModel @AssistedInject constructor(
    @Assisted private val adapter: ISendBitcoinAdapter,
    @Assisted private val token: Token,
    @Assisted("initialCustomUnspentOutputs") initialCustomUnspentOutputs: List<UnspentOutputInfo>?,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    private val numberFormatter: IAppNumberFormatter,
) : ViewModelUiState<UtxoExpertModeModule.UiState>() {

    private val xRateService = XRateService(marketKit, currencyManager.baseCurrency)

    private var unspentOutputViewItems = listOf<UtxoExpertModeModule.UnspentOutputViewItem>()
    private var selectedUnspentOutputs = listOf<String>()
    private var coinRate = xRateService.getRate(token.coin.uid)
    private var availableBalanceInfo = UtxoExpertModeModule.InfoItem(
        value = numberFormatter.formatCoinFull(BigDecimal.ZERO, token.coin.code, token.decimals),
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
            value = numberFormatter.formatCoinFull(totalCoinValue, token.coin.code, token.decimals),
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
                amountToken = numberFormatter.formatCoinFull(coinValue, token.coin.code, token.decimals),
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

    @AssistedFactory
    interface Factory {
        fun create(
            adapter: ISendBitcoinAdapter,
            token: Token,
            @Assisted("initialCustomUnspentOutputs") initialCustomUnspentOutputs: List<UnspentOutputInfo>?,
        ): UtxoExpertModeViewModel
    }
}

object UtxoExpertModeModule {

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
