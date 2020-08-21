package io.horizontalsystems.bankwallet.modules.swapnew

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.EthereumKitNotCreated
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.swapnew.model.AmountType
import io.horizontalsystems.bankwallet.modules.swapnew.model.Trade
import io.horizontalsystems.bankwallet.modules.swapnew.repository.AllowanceRepository
import io.horizontalsystems.bankwallet.modules.swapnew.repository.UniswapRepository
import io.horizontalsystems.bankwallet.modules.swapnew.service.UniswapService
import io.horizontalsystems.bankwallet.modules.swapnew.view.SwapActivity
import io.horizontalsystems.bankwallet.modules.swapnew.view.SwapViewModel
import io.horizontalsystems.uniswapkit.UniswapKit
import io.reactivex.Observable
import java.math.BigDecimal

object SwapModuleNew {
    const val tokenInKey = "tokenInKey"

    interface ISwapService {
        val coinSending: Observable<Coin>
        val coinReceiving: Observable<Coin>
        val amountSending: Observable<BigDecimal>
        val amountReceiving: Observable<BigDecimal>
        val amountType: Observable<AmountType>
        val balance: Observable<CoinValue>
        val allowance: Observable<DataState<CoinValue?>>
        val trade: Observable<DataState<Trade?>>
        val errors: Observable<List<SwapError>>
        val state: Observable<SwapState>

        fun setCoinSending(coin: Coin)
        fun setCoinReceiving(coin: Coin)
        fun setAmountSending(amount: BigDecimal)
        fun setAmountReceiving(amount: BigDecimal)
    }

    sealed class SwapError {
        object InsufficientBalance : SwapError()
        object InsufficientAllowance : SwapError()
        object TooHighPriceImpact : SwapError()
        object NoLiquidity : SwapError()
    }

    sealed class SwapState {
        object Idle : SwapState()
        object ApproveRequired : SwapState()
        object WaitingForApprove : SwapState()
        object SwapAllowed : SwapState()
    }

    fun start(context: Context, tokenIn: Coin) {
        val intent = Intent(context, SwapActivity::class.java)
        intent.putExtra(tokenInKey, tokenIn)

        context.startActivity(intent)
    }

    class Factory(private val coinSending: Coin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val ethereumKit = App.ethereumKitManager.ethereumKit ?: throw EthereumKitNotCreated()
            val uniswapKit = UniswapKit.getInstance(ethereumKit)
            val swapRepository = UniswapRepository(uniswapKit)
            val allowanceRepository = AllowanceRepository(ethereumKit, ethereumKit.receiveAddress, uniswapKit.routerAddress)
            val swapService = UniswapService(coinSending, swapRepository, allowanceRepository, App.walletManager, App.adapterManager)
            val stringProvider = ResourceProvider(App.instance)

            return SwapViewModel(swapService, stringProvider) as T
        }
    }
}
