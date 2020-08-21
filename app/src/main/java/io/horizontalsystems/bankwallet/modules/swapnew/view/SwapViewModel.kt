package io.horizontalsystems.bankwallet.modules.swapnew.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swapnew.DataState
import io.horizontalsystems.bankwallet.modules.swapnew.ResourceProvider
import io.horizontalsystems.bankwallet.modules.swapnew.SwapModuleNew.ISwapService
import io.horizontalsystems.bankwallet.modules.swapnew.SwapModuleNew.SwapError
import io.horizontalsystems.bankwallet.modules.swapnew.SwapModuleNew.SwapState
import io.horizontalsystems.bankwallet.modules.swapnew.model.AmountType
import io.horizontalsystems.bankwallet.modules.swapnew.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swapnew.model.Trade
import io.horizontalsystems.bankwallet.modules.swapnew.view.item.TradeViewItem
import io.reactivex.BackpressureStrategy
import java.math.BigDecimal
import java.util.logging.Logger

class SwapViewModel(
        private val swapService: ISwapService,
        private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val logger = Logger.getLogger("SwapViewModel")

    // region Outputs
    val coinSending: LiveData<Coin> = LiveDataReactiveStreams.fromPublisher(swapService.coinSending.toFlowable(BackpressureStrategy.BUFFER))
    val coinReceiving: LiveData<Coin> = LiveDataReactiveStreams.fromPublisher(swapService.coinReceiving.toFlowable(BackpressureStrategy.BUFFER))

    val allowance: LiveData<String>
    val allowanceLoading: LiveData<Boolean>

    val balance: LiveData<String> = LiveDataReactiveStreams.fromPublisher(
            swapService.balance
                    .map { formatCoinAmount(it.value, it.coin) }
                    .toFlowable(BackpressureStrategy.BUFFER)
    )
    val amountSending: LiveData<String> = LiveDataReactiveStreams.fromPublisher(
            swapService.amountSending
                    .map { it.toPlainString() }
                    .toFlowable(BackpressureStrategy.BUFFER)
    )
    val amountReceiving: LiveData<String> = LiveDataReactiveStreams.fromPublisher(
            swapService.amountReceiving
                    .map { it.toPlainString() }
                    .toFlowable(BackpressureStrategy.BUFFER)
    )
    val amountSendingLabelVisible: LiveData<Boolean>
    val amountReceivingLabelVisible: LiveData<Boolean>

    val tradeViewItem: LiveData<TradeViewItem>
    val tradeViewItemLoading: LiveData<Boolean>

    val amountSendingError: LiveData<String>
    val allowanceColor: LiveData<Int>
    val priceImpactColor: LiveData<Int>

    val approveButtonVisible: LiveData<Boolean>
    val proceedButtonVisible: LiveData<Boolean>
    val proceedButtonEnabled: LiveData<Boolean>

    val error: LiveData<String>
    // endregion

    init {
        val allowanceObservable = swapService.allowance
        allowance = LiveDataReactiveStreams.fromPublisher(
                allowanceObservable
                        .filter { it is DataState.Success }
                        .map {
                            (it as? DataState.Success)?.data?.let { coinValue ->
                                formatCoinAmount(coinValue.value, coinValue.coin)
                            } ?: ""
                        }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )
        allowanceLoading = LiveDataReactiveStreams.fromPublisher(
                allowanceObservable
                        .map { it is DataState.Loading }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )

        val tradeObservable = swapService.trade
        tradeViewItem = LiveDataReactiveStreams.fromPublisher(
                tradeObservable
                        .map { tradeViewItem(it.dataOrNull) }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )
        tradeViewItemLoading = LiveDataReactiveStreams.fromPublisher(
                tradeObservable
                        .map { it is DataState.Loading }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )
        priceImpactColor = LiveDataReactiveStreams.fromPublisher(
                tradeObservable
                        .map { priceImpactColor(it.dataOrNull?.priceImpact) }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )

        val errorsObservable = swapService.errors
        amountSendingError = LiveDataReactiveStreams.fromPublisher(
                errorsObservable
                        .map { errors ->
                            when {
                                errors.contains(SwapError.InsufficientBalance) -> resourceProvider.string(R.string.Swap_ErrorInsufficientBalance)
                                else -> ""
                            }
                        }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )
        allowanceColor = LiveDataReactiveStreams.fromPublisher(
                errorsObservable
                        .map { errors ->
                            when {
                                errors.contains(SwapError.InsufficientAllowance) -> resourceProvider.colorLucian()
                                else -> resourceProvider.color(R.color.grey)
                            }
                        }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )
        error = LiveDataReactiveStreams.fromPublisher(
                errorsObservable
                        .map { errors ->
                            when {
                                errors.contains(SwapError.NoLiquidity) -> resourceProvider.string(R.string.Swap_ErrorNoLiquidity)
                                else -> ""
                            }
                        }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )

        val amountTypeObservable = swapService.amountType
        amountSendingLabelVisible = LiveDataReactiveStreams.fromPublisher(
                amountTypeObservable
                        .map {
                            it == AmountType.ExactReceiving
                        }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )
        amountReceivingLabelVisible = LiveDataReactiveStreams.fromPublisher(
                amountTypeObservable
                        .map {
                            it == AmountType.ExactSending
                        }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )

        val stateObservable = swapService.state
        approveButtonVisible = LiveDataReactiveStreams.fromPublisher(
                stateObservable
                        .map {
                            it == SwapState.ApproveRequired
                        }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )
        proceedButtonVisible = LiveDataReactiveStreams.fromPublisher(
                stateObservable
                        .map {
                            it == SwapState.SwapAllowed || it == SwapState.Idle
                        }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )
        proceedButtonEnabled = LiveDataReactiveStreams.fromPublisher(
                stateObservable
                        .map {
                            it == SwapState.SwapAllowed
                        }
                        .toFlowable(BackpressureStrategy.BUFFER)
        )
    }


    // region Inputs
    fun setCoinSending(coin: Coin) {
        swapService.setCoinSending(coin)
    }

    fun setCoinReceiving(coin: Coin) {
        swapService.setCoinReceiving(coin)
    }

    fun setAmountSending(amount: String?) {
        swapService.setAmountSending(nonZeroAmountOrNull(amount) ?: BigDecimal.ZERO)
    }

    fun setAmountReceiving(amount: String?) {
        swapService.setAmountReceiving(nonZeroAmountOrNull(amount) ?: BigDecimal.ZERO)
    }
// endregion

    private fun nonZeroAmountOrNull(amount: String?): BigDecimal? {
        return if (amount?.trimEnd { it == '0' || it == '.' }.isNullOrBlank()) null else BigDecimal(amount)
    }

    private fun formatCoinAmount(amount: BigDecimal, coin: Coin): String {
        val maxFraction = if (coin.decimal < 8) coin.decimal else 8
        return App.numberFormatter.formatCoin(amount, coin.code, 0, maxFraction)
    }

    private fun tradeViewItem(trade: Trade?): TradeViewItem {
        if (trade == null) {
            return TradeViewItem()
        }

        val minMaxTitle: String?
        val minMaxAmount: String?

        when (trade.amountType) {
            AmountType.ExactSending -> {
                minMaxTitle = resourceProvider.string(R.string.Swap_MinimumReceived)
                minMaxAmount = trade.minMaxAmount?.let { formatCoinAmount(it, trade.coinReceiving) }
            }
            AmountType.ExactReceiving -> {
                minMaxTitle = resourceProvider.string(R.string.Swap_MaximiumSold)
                minMaxAmount = trade.minMaxAmount?.let { formatCoinAmount(it, trade.coinSending) }
            }
        }

        return TradeViewItem(
                trade.executionPrice?.let { "${formatCoinAmount(it, trade.coinReceiving)} / ${trade.coinSending.code} " },
                trade.priceImpact?.value?.toPlainString(),
                minMaxTitle,
                minMaxAmount
        )
    }

    private fun priceImpactColor(priceImpact: PriceImpact?): Int {
        return when (priceImpact?.level) {
            PriceImpact.Level.Normal -> resourceProvider.colorRemus()
            PriceImpact.Level.Warning -> resourceProvider.colorJacob()
            PriceImpact.Level.Forbidden -> resourceProvider.colorLucian()
            else -> resourceProvider.color(R.color.grey)
        }
    }

}
