package io.horizontalsystems.bankwallet.modules.zcashmigration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
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
) : ViewModel() {
    private val logger = AppLogger("Zcash-Ironwood-Migration")

    val coinMaxAllowedDecimals = wallet.token.decimals

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
        private set

    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    // Migration-required Orchard balance shown while the exact proposal is loading
    var amount by mutableStateOf(adapter.ironwoodMigrationRequiredBalance ?: BigDecimal.ZERO)
        private set

    var fee by mutableStateOf<BigDecimal?>(null)
        private set

    var error by mutableStateOf<Throwable?>(null)
        private set

    init {
        xRateService.getRateFlow(wallet.coin.uid).collectWith(viewModelScope) {
            coinRate = it
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

            // TODO: execute the migration via OrchardMigrationSdk:
            //  signAndStoreMigrationSchedule(schedule, usk) + executeNextPendingTransfer(options)
            throw NotImplementedError("Migration execution is not wired yet")

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
