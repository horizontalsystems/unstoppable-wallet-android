package io.horizontalsystems.bankwallet.modules.swap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.ValidationError.*
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinModule
import io.horizontalsystems.uniswapkit.models.TradeType
import io.horizontalsystems.views.R.attr
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.activity_swap.*
import java.math.BigDecimal

class SwapActivity : BaseActivity() {

    lateinit var viewModel: SwapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(getDrawable(R.drawable.ic_info))
        supportActionBar?.title = getString(R.string.Swap_Title)

        val tokenIn = intent.extras?.getParcelable<Coin>(SwapModule.tokenInKey)
        viewModel = ViewModelProvider(this, SwapModule.Factory(tokenIn)).get(SwapViewModel::class.java)

        fromAmount.apply {
            onTokenButtonClick {
                SelectSwapCoinModule.start(this@SwapActivity, requestSelectFromCoin, true, viewModel.toCoinLiveData.value?.coin)
            }

            onMaxButtonClick {
                viewModel.onFromAmountMaxButtonClick()
            }

            editText.addTextChangedListener(fromAmountListener)
        }

        toAmount.apply {
            onTokenButtonClick {
                SelectSwapCoinModule.start(this@SwapActivity, requestSelectToCoin, false, viewModel.fromCoinLiveData.value?.coin)
            }

            editText.addTextChangedListener(toAmountListener)
        }

        proceedButton.bind(onClick = {
            viewModel.onProceedButtonClick()
        })

        viewModel.fromAmountLiveData.observe(this, Observer { amount ->
            fromAmount.editText.setText(amount?.toPlainString())
        })

        viewModel.errorLiveData.observe(this, Observer { error ->
            //reset previous errors
            fromAmount.setError(null)

            when (error) {
                is InsufficientBalance -> {
                    fromAmount.setError(errorText(error))
                }
                is PriceImpactTooHigh, is PriceImpactInvalid, is NoTradeData -> {
                    //todo show error message
                }
                else -> {
                    //todo show error message
                }
            }
        })

        viewModel.tradeTypeLiveData.observe(this, Observer { tradeType ->
            when (tradeType) {
                TradeType.ExactIn -> {
                    fromEstimatedLabel.isVisible = false
                    toEstimatedLabel.isVisible = true
                    setToAmount(null)
                }
                TradeType.ExactOut -> {
                    fromEstimatedLabel.isVisible = true
                    toEstimatedLabel.isVisible = false
                    setFromAmount(null)
                }
            }
        })

        viewModel.fromCoinLiveData.observe(this, Observer { coinWithBalance ->
            coinWithBalance?.let {
                fromAmount.setSelectedCoin(coinWithBalance.coin.code)
                availableBalanceValue.text = formatCoinAmount(coinWithBalance.balance, coinWithBalance.coin)
            }
        })

        viewModel.toCoinLiveData.observe(this, Observer { coinWithBalance ->
            coinWithBalance?.let {
                toAmount.setSelectedCoin(coinWithBalance.coin.code)
            }
        })

        viewModel.tradeLiveData.observe(this, Observer { tradeData ->
            if (tradeData != null) {
                val fromCoin = viewModel.fromCoinLiveData.value?.coin ?: return@Observer
                val toCoin = viewModel.toCoinLiveData.value?.coin ?: return@Observer

                when (tradeData.type) {
                    TradeType.ExactIn -> {
                        setToAmount(tradeData.amountOut)
                        minMaxTitle.text = getString(R.string.Swap_MinimumReceived)
                        minMaxValue.text = tradeData.amountOutMin?.let { formatCoinAmount(it, toCoin) }
                    }
                    TradeType.ExactOut -> {
                        setFromAmount(tradeData.amountIn)
                        minMaxTitle.text = getString(R.string.Swap_MaximiumSold)
                        minMaxValue.text = tradeData.amountInMax?.let { formatCoinAmount(it, fromCoin) }
                    }
                }

                priceValue.text = tradeData.executionPrice?.let {
                    "${formatCoinAmount(it, toCoin)} / ${fromCoin.code} "
                }

                priceImpactValue.text = tradeData.priceImpact?.let {
                    getString(R.string.Swap_Percent, it.toPlainString())
                }
                priceImpactValue.setTextColor(colorForPriceImpact(tradeData.priceImpact))
            } else {
                clearTradeData()
            }
        })

        viewModel.proceedButtonEnabledLiveData.observe(this, Observer { enabled ->
            proceedButton.updateState(enabled)
        })
    }

    private fun colorForPriceImpact(priceImpact: BigDecimal?) = when {
        priceImpact == null -> LayoutHelper.getAttr(attr.ColorLeah, theme)
                ?: R.color.steel_light
        priceImpact < viewModel.priceImpactDesirableThreshold -> LayoutHelper.getAttr(attr.ColorRemus, theme)
                ?: R.color.green_d
        priceImpact < viewModel.priceImpactAllowedThreshold -> LayoutHelper.getAttr(attr.ColorJacob, theme)
                ?: R.color.yellow_d
        else -> LayoutHelper.getAttr(attr.ColorLucian, theme) ?: R.color.red_d
    }

    private fun errorText(error: Throwable): String = when (error) {
        is InsufficientBalance -> getString(R.string.Swap_ErrorInsufficientBalance)
        else -> ""
    }

    private fun clearTradeData() {
        when (viewModel.tradeTypeLiveData.value) {
            TradeType.ExactIn -> {
                setToAmount(null)
            }
            TradeType.ExactOut -> {
                setFromAmount(null)
            }
        }
        minMaxValue.text = null
        priceValue.text = null
        priceImpactValue.text = null
    }

    private fun formatCoinAmount(amount: BigDecimal, coin: Coin): String {
        val maxFraction = if (coin.decimal < 8) coin.decimal else 8
        return App.numberFormatter.formatCoin(amount, coin.code, 0, maxFraction)
    }

    private val fromAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.onFromAmountChange(s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private val toAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.onToAmountChange(s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private fun setFromAmount(amount: BigDecimal?) {
        fromAmount.editText.apply {
            removeTextChangedListener(fromAmountListener)
            setText(amount?.toPlainString())
            addTextChangedListener(fromAmountListener)
        }
    }

    private fun setToAmount(amount: BigDecimal?) {
        toAmount.editText.apply {
            removeTextChangedListener(toAmountListener)
            setText(amount?.toPlainString())
            addTextChangedListener(toAmountListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.swap_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuCancel -> {
                finish()
                return true
            }
            android.R.id.home -> {
                UniswapInfoActivity.start(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val selectedCoin = data?.getParcelableExtra<Coin>(SelectSwapCoinModule.selectedCoinKey)
                    ?: return
            when (requestCode) {
                requestSelectFromCoin -> viewModel.onSelectFromCoin(selectedCoin)
                requestSelectToCoin -> viewModel.onSelectToCoin(selectedCoin)
            }
        }
    }

    companion object {
        const val requestSelectFromCoin = 0
        const val requestSelectToCoin = 1
    }

}
