package io.horizontalsystems.bankwallet.modules.send.bitcoin.utxoexpert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.util.Date

class UtxoExpertModeViewModel(
    private val adapter: ISendBitcoinAdapter,
    private val token: Token,
    private val address: Address?,
    private val feeRate: Int?,
    initialValue: BigDecimal?,
    initialCustomUnspentOutputs: List<UnspentOutputInfo>?,
    xRateService: XRateService,
) : ViewModelUiState<UtxoExpertModeModule.UiState>() {

    private var value = initialValue ?: BigDecimal.ZERO
    private var unspentOutputViewItems = listOf<UtxoExpertModeModule.UnspentOutputViewItem>()
    private var selectedUnspentOutputs = listOf<String>()
    private var coinRate = xRateService.getRate(token.coin.uid)
    private var sendToInfo = UtxoExpertModeModule.InfoItem(
        subTitle = address?.hex?.shorten(),
        value = App.numberFormatter.formatCoinFull(value, token.coin.code, token.decimals),
        subValue = coinRate?.let { rate ->
            rate.copy(value = value.times(rate.value)).getFormattedFull()
        } ?: "",
    )
    private var changeInfo: UtxoExpertModeModule.InfoItem? = null
    private var feeInfo = UtxoExpertModeModule.InfoItem(
        subTitle = null,
        value = null,
        subValue = null,
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
        setUnspentOutputViewItems()
        if (selectedUnspentOutputs.isNotEmpty()) {
            updateFee()
        }
        emitState()
    }

    override fun createState() = UtxoExpertModeModule.UiState(
        sendToInfo = sendToInfo,
        changeInfo = changeInfo,
        feeInfo = feeInfo,
        utxoItems = unspentOutputViewItems,
    )

    private fun getUnspentId(unspentOutputInfo: UnspentOutputInfo) = "${unspentOutputInfo.transactionHash.toHexString()}-${unspentOutputInfo.outputIndex}"

    fun onUnspentOutputClicked(id: String) {
        selectedUnspentOutputs = if (selectedUnspentOutputs.contains(id)) {
            selectedUnspentOutputs.filter { it != id }
        } else {
            selectedUnspentOutputs + id
        }
        unspentOutputViewItems = unspentOutputViewItems.map { utxo ->
            utxo.copy(selected = selectedUnspentOutputs.contains(utxo.id))
        }
        updateFee()
        emitState()
    }

    private fun updateFee() {
        val feeRateNonNull = feeRate ?: return
        val bitcoinFeeInfo = adapter.bitcoinFeeInfo(
            amount = value,
            feeRate = feeRateNonNull,
            address = address?.hex,
            unspentOutputs = customOutputs,
            pluginData = null,
        )
        val fee = bitcoinFeeInfo?.fee
        if (fee == null) {
            feeInfo = feeInfo.copy(
                value = null,
                subValue = null,
            )
            changeInfo = null
            return
        }
        feeInfo = feeInfo.copy(
            value = App.numberFormatter.formatCoinFull(fee, token.coin.code, token.decimals),
            subValue = coinRate?.let { rate ->
                rate.copy(value = fee.times(rate.value)).getFormattedFull()
            } ?: "",
        )
        changeInfo = if (bitcoinFeeInfo.changeAddress != null && bitcoinFeeInfo.changeValue != null) {
            val value = bitcoinFeeInfo.changeValue
            UtxoExpertModeModule.InfoItem(
                subTitle = bitcoinFeeInfo.changeAddress.stringValue.shorten(),
                value = App.numberFormatter.formatCoinFull(value, token.coin.code, token.decimals),
                subValue = coinRate?.let { rate ->
                    rate.copy(value = value.times(rate.value)).getFormattedFull()
                } ?: "",
            )
        } else {
            null
        }
        emitState()
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

}

object UtxoExpertModeModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val adapter: ISendBitcoinAdapter,
        private val token: Token,
        private val address: Address?,
        private val value: BigDecimal?,
        private val feeRate: Int?,
        private val customUnspentOutputs: List<UnspentOutputInfo>?,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UtxoExpertModeViewModel(
                adapter = adapter,
                token = token,
                address = address,
                feeRate = feeRate,
                initialValue = value,
                initialCustomUnspentOutputs = customUnspentOutputs,
                xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            ) as T
        }
    }

    data class UiState(
        val sendToInfo: InfoItem,
        val changeInfo: InfoItem?,
        val feeInfo: InfoItem,
        val utxoItems: List<UnspentOutputViewItem>,
    )

    data class InfoItem(
        val subTitle: String?,
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