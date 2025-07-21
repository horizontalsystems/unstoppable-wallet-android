package cash.p.terminal.modules.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.adapters.stellar.StellarAssetAdapter
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.entities.CurrencyValue
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
                App.coinManager.getToken(TokenQuery(wallet.token.blockchainType, TokenType.Native))
                    ?: throw kotlin.IllegalArgumentException()

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
