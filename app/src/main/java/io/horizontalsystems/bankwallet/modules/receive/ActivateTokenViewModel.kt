package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.StellarAssetAdapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.EnablingAssetError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivateTokenViewModel(
    wallet: Wallet,
    feeToken: Token,
    adapterManager: IAdapterManager,
    xRateService: XRateService,
) : ViewModelUiState<ActivateTokenUiState>() {
    private val token = wallet.token
    private val adapter = adapterManager.getAdapterForWallet<StellarAssetAdapter>(wallet)
    private var activateEnabled = false
    private var error: ActivateTokenError? = null
    private val feeAmount = adapter?.activationFee
    private var feeCoinValue: CoinValue? = null
    private var feeFiatValue: CurrencyValue? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val tmpAdapter = adapter

            if (tmpAdapter == null) {
                activateEnabled = false
                error = ActivateTokenError.NullAdapter()
            } else if (tmpAdapter.isTrustlineEstablished()) {
                activateEnabled = false
                error = ActivateTokenError.AlreadyActive()
            } else try {
                tmpAdapter.validateActivation()

                activateEnabled = true
                error = null
            } catch (e: EnablingAssetError.InsufficientBalance) {
                activateEnabled = false
                error = ActivateTokenError.InsufficientBalance()
            }

            feeAmount?.let { feeAmount ->
                feeCoinValue = CoinValue(feeToken, feeAmount)
                feeFiatValue = xRateService.getRate(feeToken.coin.uid)?.let { rate ->
                    rate.copy(value = rate.value * feeAmount)
                }
            }

            emitState()
        }
    }

    override fun createState() = ActivateTokenUiState(
        token = token,
        currency = App.currencyManager.baseCurrency,
        activateEnabled = activateEnabled,
        error = error,
        feeCoinValue = feeCoinValue,
        feeFiatValue = feeFiatValue
    )

    suspend fun activate() = withContext(Dispatchers.Default) {
        adapter?.activate()
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val feeToken =
                App.coinManager.getToken(TokenQuery(wallet.token.blockchainType, TokenType.Native)) ?: throw IllegalArgumentException()

            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

            return ActivateTokenViewModel(
                wallet,
                feeToken,
                App.adapterManager,
                xRateService
            ) as T
        }
    }
}

sealed class ActivateTokenError : Throwable() {
    class NullAdapter : ActivateTokenError()
    class AlreadyActive : ActivateTokenError()
    class InsufficientBalance : ActivateTokenError()
}

data class ActivateTokenUiState(
    val token: Token,
    val currency: Currency,
    val activateEnabled: Boolean,
    val error: ActivateTokenError?,
    val feeCoinValue: CoinValue?,
    val feeFiatValue: CurrencyValue?
)
