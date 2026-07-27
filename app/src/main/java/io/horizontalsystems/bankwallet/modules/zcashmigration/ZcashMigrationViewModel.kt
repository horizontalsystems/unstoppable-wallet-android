package io.horizontalsystems.bankwallet.modules.zcashmigration

import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class ZcashMigrationViewModel(
    private val adapter: ZcashAdapter,
    val wallet: Wallet,
    xRateService: XRateService,
) : ViewModelUiState<ZcashMigrationUiState>() {
    private val logger = AppLogger("Zcash-Ironwood-Migration")

    val coinMaxAllowedDecimals = wallet.token.decimals

    private var coinRate = xRateService.getRate(wallet.coin.uid)
    private var sendResult: SendResult? = null
    // Migration-required Orchard balance shown while the exact proposal is loading
    private var amount = adapter.ironwoodMigrationRequiredBalance ?: BigDecimal.ZERO
    private var fee: BigDecimal? = null
    private var error: Throwable? = null

    override fun createState() = ZcashMigrationUiState(
        coinRate = coinRate,
        sendResult = sendResult,
        amount = amount,
        fee = fee,
        error = error,
    )

    init {
        xRateService.getRateFlow(wallet.coin.uid).collectWith(viewModelScope) {
            coinRate = it
            emitState()
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val proposal = adapter.proposeIronwoodMigration()
                amount = proposal.amount
                fee = proposal.fee
            } catch (e: Throwable) {
                logger.warning("propose failed", e)
                error = e
            }
            emitState()
        }
    }

    fun onClickMigrate() {
        viewModelScope.launch {
            migrate()
        }
    }

    private suspend fun migrate() = withContext(Dispatchers.IO) {
        val logger = logger.getScopedUnique()
        logger.info("click")

        try {
            sendResult = SendResult.Sending
            emitState()

            val txId = adapter.executeIronwoodMigration()

            logger.info("success, txId=$txId")
            sendResult = SendResult.Sent()

        } catch (e: Throwable) {
            logger.warning("failed", e)
            sendResult = SendResult.Failed(createCaution(e))
        }
        emitState()
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }
}

data class ZcashMigrationUiState(
    val coinRate: CurrencyValue?,
    val sendResult: SendResult?,
    val amount: BigDecimal,
    val fee: BigDecimal?,
    val error: Throwable?,
)
