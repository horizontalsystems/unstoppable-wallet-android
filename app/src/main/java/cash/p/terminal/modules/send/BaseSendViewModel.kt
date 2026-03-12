package cash.p.terminal.modules.send

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.isNative
import cash.p.terminal.entities.CoinValue
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.modules.send.fee.NetworkFeeWarningData
import cash.p.terminal.modules.send.fee.buildNetworkFeeWarningData
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import cash.p.terminal.ui_compose.components.HudHelper
import io.horizontalsystems.core.ViewModelUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal

abstract class BaseSendViewModel<T>(
    val wallet: Wallet,
    adapterManager: IAdapterManager
) : ViewModelUiState<T>() {
    private val _adapterManager = adapterManager
    private val marketKit: MarketKitWrapper by inject(MarketKitWrapper::class.java)
    private val balanceHiddenManager: IBalanceHiddenManager by inject(IBalanceHiddenManager::class.java)

    var isSynced by mutableStateOf(true)
        private set

    open val feeToken: Token? by lazy {
        val token = wallet.token
        if (token.type.isNative) {
            token
        } else {
            marketKit.token(TokenQuery(token.blockchainType, TokenType.Native)) ?: token
        }
    }

    var feeCoinBalance: BigDecimal? by mutableStateOf(null)
        private set

    var feeWarningData by mutableStateOf<NetworkFeeWarningData?>(null)
        private set

    var balanceHidden by mutableStateOf(balanceHiddenManager.balanceHiddenFlow.value)
        private set

    val displayBalance: BigDecimal?
        get() = _adapterManager.getAdjustedBalanceDataForToken(wallet.token)?.available

    fun toggleHideBalance() {
        HudHelper.vibrate(App.instance)
        balanceHiddenManager.toggleBalanceHidden()
    }

    protected open fun getEstimatedFee(): BigDecimal? = null
    protected open fun onSendRequested() {}

    private fun currentFeeWarningData(): NetworkFeeWarningData? {
        val ft = feeToken ?: return null
        return buildNetworkFeeWarningData(
            blockchainType = wallet.token.blockchainType,
            tokenType = wallet.token.type,
            feeTokenBalance = feeCoinBalance,
            estimatedFee = getEstimatedFee(),
            feeToken = ft,
        )
    }

    val inlineFeeWarningData: NetworkFeeWarningData?
        get() = currentFeeWarningData()

    fun onClickSendWithWarningCheck() {
        val data = currentFeeWarningData()
        if (data != null) {
            feeWarningData = data
            return
        }
        onSendRequested()
    }

    fun onFeeWarningConfirmed() {
        feeWarningData = null
        onSendRequested()
    }

    fun onFeeWarningCancelled() {
        feeWarningData = null
    }

    private fun resolveFeeBalanceAdapter(): IBalanceAdapter? {
        if (wallet.token.type.isNative) return null
        return feeToken?.let { _adapterManager.getAdapterForToken<IBalanceAdapter>(it) }
    }

    fun isInsufficientFeeBalance(fee: BigDecimal?): Boolean {
        if (wallet.token.type.isNative) return false
        val currentFee = fee ?: return false
        val balance = feeCoinBalance ?: BigDecimal.ZERO
        return currentFee > balance
    }

    fun formatFeePrimary(fee: BigDecimal?): String {
        if (fee == null) return "---"
        return feeToken?.let { CoinValue(it, fee).getFormattedFull() } ?: "---"
    }

    fun formatFeeSecondary(fee: BigDecimal?, rate: CurrencyValue?): String {
        val f = fee ?: return ""
        return rate?.copy(value = f.times(rate.value))?.getFormattedFull() ?: ""
    }

    init {
        _adapterManager.getBalanceAdapterForWallet(wallet)?.let { adapter ->
            isSynced = adapter.balanceState is AdapterState.Synced
            viewModelScope.launch {
                adapter.balanceStateUpdatedFlow.collect {
                    isSynced = adapter.balanceState is AdapterState.Synced
                }
            }
        }

        // feeToken may be overridden in subclasses whose property initializers
        // run after base init. yield() suspends so the constructor finishes first.
        viewModelScope.launch {
            yield()
            val ft = feeToken
            if (!wallet.token.type.isNative && ft != null) {
                feeCoinBalance = _adapterManager.getAdjustedBalanceDataForToken(ft)?.available
                val adapter = resolveFeeBalanceAdapter()
                adapter?.balanceUpdatedFlow?.collect {
                    feeCoinBalance = _adapterManager.getAdjustedBalanceDataForToken(ft)?.available
                }
                return@launch
            }
            val adapter = resolveFeeBalanceAdapter()
            feeCoinBalance = adapter?.balanceData?.total
            adapter?.balanceUpdatedFlow?.collect {
                feeCoinBalance = adapter.balanceData.total
            }
        }

        viewModelScope.launch {
            balanceHiddenManager.balanceHiddenFlow.collect {
                balanceHidden = it
            }
        }
    }
}
