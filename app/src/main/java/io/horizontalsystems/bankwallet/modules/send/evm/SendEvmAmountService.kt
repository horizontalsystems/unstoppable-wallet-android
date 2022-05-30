package io.horizontalsystems.bankwallet.modules.send.evm

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class SendEvmAmountService(
    private val adapter: ISendEthereumAdapter,
    private val coin: PlatformCoin,
    private val amountValidator: AmountValidator,
    private val coinMaxAllowedDecimals: Int
) {
    private var amount: BigDecimal? = null
    private var amountCaution: HSCaution? = null
    private var evmAmount: BigInteger? = null

    private var availableBalance: BigDecimal = adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN)

    private val _stateFlow = MutableStateFlow(
        State(
            amountCaution = amountCaution,
            availableBalance = availableBalance,
            canBeSend = false,
            evmAmount = evmAmount,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        validateAmount()
        refreshEvmAmount()

        emitState()
    }

    private fun emitState() {
        val tmpAmountCaution = amountCaution

        val canBeSend = evmAmount != null
            && (tmpAmountCaution == null || tmpAmountCaution.isWarning())

        _stateFlow.update {
            State(
                evmAmount = evmAmount,
                amountCaution = amountCaution,
                availableBalance = availableBalance,
                canBeSend = canBeSend
            )
        }
    }

    private fun validateAmount() {
        amountCaution = amountValidator.validate(
            amount,
            coin.code,
            availableBalance
        )

        if (amountCaution == null && amount == availableBalance && isCoinUsedForFee()) {
            amountCaution = HSCaution(
                TranslatableString.ResString(R.string.EthereumTransaction_Warning_CoinNeededForFee, coin.code),
                HSCaution.Type.Warning
            )
        }
    }

    private fun refreshEvmAmount() {
        val tmpAmount = amount
        evmAmount = if (tmpAmount != null && tmpAmount > BigDecimal.ZERO) {
            tmpAmount.movePointRight(coin.decimals).toBigInteger()
        } else {
            null
        }
    }

    private fun isCoinUsedForFee() = when (coin.coinType) {
        is CoinType.Ethereum,
        is CoinType.BinanceSmartChain,
        is CoinType.Polygon,
        is CoinType.EthereumOptimism,
        is CoinType.EthereumArbitrumOne -> true
        else -> false
    }

    data class State(
        val amountCaution: HSCaution?,
        val availableBalance: BigDecimal,
        val canBeSend: Boolean,
        val evmAmount: BigInteger?
    )
}
