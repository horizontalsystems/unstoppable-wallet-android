package io.horizontalsystems.bankwallet.modules.send.zcash.shield

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationData
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

@HiltViewModel(assistedFactory = ShieldZcashViewModel.Factory::class)
class ShieldZcashViewModel @AssistedInject constructor(
    @Assisted private val wallet: Wallet,
    adapterManager: IAdapterManager,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
) : ViewModel() {
    private val adapter: ZcashAdapter = adapterManager.getAdapterForWallet<ZcashAdapter>(wallet)
        ?: throw IllegalStateException("ZcashAdapter is null")
    private val xRateService: XRateService = XRateService(marketKit, currencyManager.baseCurrency)

    private val logger = AppLogger("Shield-Zcash")

    val blockchainType = wallet.token.blockchainType
    val coinMaxAllowedDecimals = wallet.token.decimals

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
        private set

    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    var fee by mutableStateOf<BigDecimal?>(null)
        private set

    init {
        xRateService.getRateFlow(wallet.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }

        viewModelScope.launch {
            fee = adapter.shieldTransactionFee()
        }
    }

    fun getConfirmationData(): SendConfirmationData {
        val unshielded = adapter.balanceData?.unshielded ?: throw IllegalStateException()

        return SendConfirmationData(
            amount = unshielded,
            fee = null,
            address = null,
            contact = null,
            token = wallet.token,
            feeCoin = wallet.coin,
            memo = null
        )
    }

    fun onClickSend() {
        viewModelScope.launch {
            send()
        }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        val logger = logger.getScopedUnique()
        logger.info("click")

        try {
            sendResult = SendResult.Sending

            adapter.sendShieldProposal()

            logger.info("success")
            sendResult = SendResult.Sent()

        } catch (e: Throwable) {
            logger.warning("failed", e)
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    @AssistedFactory
    interface Factory {
        fun create(wallet: Wallet): ShieldZcashViewModel
    }
}
