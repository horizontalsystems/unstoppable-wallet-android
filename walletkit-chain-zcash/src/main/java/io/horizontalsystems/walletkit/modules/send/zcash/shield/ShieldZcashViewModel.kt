package io.horizontalsystems.walletkit.modules.send.zcash.shield

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.LocalizedException
import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.send.SendConfirmationData
import io.horizontalsystems.walletkit.modules.send.SendResult
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class ShieldZcashViewModel(
    private val adapter: ZcashAdapter,
    private val wallet: Wallet,
    private val xRateService: XRateService,
) : ViewModel() {
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
        viewModelScope.launch {
            xRateService.getRateFlow(wallet.coin.uid).collect {
                coinRate = it
            }
        }

        viewModelScope.launch {
            fee = adapter.shieldTransactionFee()
        }
    }

    /**
     * Confirmation data for the current input, or null when it isn't available.
     *
     * The pieces are filled in asynchronously and none of them survive process death, so a
     * confirmation screen restored from the saved back stack sees empty state. Reporting that as
     * null lets the caller send the user back to the form instead of crashing on composition.
     */
    fun getConfirmationData(): SendConfirmationData? {
        val unshielded = adapter.balanceData?.unshielded ?: return null

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
}
