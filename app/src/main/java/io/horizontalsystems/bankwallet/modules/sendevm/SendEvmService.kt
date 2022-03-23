package io.horizontalsystems.bankwallet.modules.sendevm

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData.AdditionalInfo
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.min
import io.horizontalsystems.ethereumkit.models.Address as EvmAddress

class SendEvmService(
    val sendCoin: PlatformCoin,
    val adapter: ISendEthereumAdapter
) {
    val coinDecimal = min(sendCoin.decimals, App.appConfigProvider.maxDecimal)
    val fiatDecimal = App.appConfigProvider.fiatDecimal
    val availableBalance: BigDecimal
        get() = adapter.balanceData.available.setScale(coinDecimal, RoundingMode.DOWN)

    private var _sendingAvailable = MutableSharedFlow<Boolean>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sendingAvailable = _sendingAvailable.asSharedFlow()

    private var evmAmount: BigInteger? = null
    private var addressData: AddressData? = null

    fun getSendData(): SendEvmData? {
        val tmpEvmAmount = evmAmount ?: return null
        val tmpAddressData = addressData ?: return null

        val transactionData = adapter.getTransactionData(tmpEvmAmount, tmpAddressData.evmAddress)
        val additionalInfo = AdditionalInfo.Send(SendEvmData.SendInfo(tmpAddressData.domain))

        return SendEvmData(transactionData, additionalInfo)
    }

    fun setAmount(amount: BigDecimal?) {
        evmAmount = if (amount != null && amount > BigDecimal.ZERO) {
            amount.movePointRight(sendCoin.decimals).toBigInteger()
        } else {
            null
        }

        syncState()
    }

    fun setRecipientAddress(address: Address?) {
        addressData = address?.let {
            AddressData(evmAddress = EvmAddress(it.hex), domain = it.domain)
        }
        syncState()
    }

    private fun syncState() {
        _sendingAvailable.tryEmit(evmAmount != null && addressData != null)
    }

    fun validateAmount(amount: BigDecimal?): Caution? {
        if (amount != availableBalance) return null
        if (
            sendCoin.coinType !is CoinType.Ethereum
            && sendCoin.coinType !is CoinType.BinanceSmartChain
            && sendCoin.coinType !is CoinType.Polygon
            && sendCoin.coinType !is CoinType.EthereumOptimism
            && sendCoin.coinType !is CoinType.EthereumArbitrumOne
        ) return null


        return Caution(
            Translator.getString(R.string.EthereumTransaction_Warning_CoinNeededForFee, sendCoin.coin.code),
            Caution.Type.Warning
        )
    }

    data class AddressData(val evmAddress: EvmAddress, val domain: String?)
}
