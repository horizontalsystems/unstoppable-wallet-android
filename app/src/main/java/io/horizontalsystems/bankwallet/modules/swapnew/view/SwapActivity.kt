package io.horizontalsystems.bankwallet.modules.swapnew.view

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
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.UniswapInfoActivity
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinModule
import io.horizontalsystems.bankwallet.modules.swapnew.SwapModuleNew
import io.horizontalsystems.bankwallet.modules.swapnew.view.item.TradeViewItem
import kotlinx.android.synthetic.main.activity_swap.*

class SwapActivity : BaseActivity() {

    lateinit var viewModel: SwapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(getDrawable(R.drawable.ic_info))
        supportActionBar?.title = getString(R.string.Swap_Title)

        val coinSending = intent.extras?.getParcelable<Coin>(SwapModuleNew.tokenInKey)
        viewModel = ViewModelProvider(this, SwapModuleNew.Factory(coinSending!!)).get(SwapViewModel::class.java)

        fromAmount.apply {
            onTokenButtonClick {
                SelectSwapCoinModule.start(this@SwapActivity, requestSelectFromCoin, true, viewModel.coinReceiving.value)
            }

            editText.addTextChangedListener(fromAmountListener)
        }

        toAmount.apply {
            onTokenButtonClick {
                SelectSwapCoinModule.start(this@SwapActivity, requestSelectToCoin, false, viewModel.coinSending.value)
            }

            editText.addTextChangedListener(toAmountListener)
        }

        proceedButton.setOnSingleClickListener {
            // open confirmation module
        }

        approveButton.setOnSingleClickListener {
            // open approve module
        }

        viewModel.proceedButtonVisible.observe(this, Observer { proceedButtonVisible ->
            proceedButton.isVisible = proceedButtonVisible
        })

        viewModel.proceedButtonEnabled.observe(this, Observer { proceedButtonEnabled ->
            proceedButton.isEnabled = proceedButtonEnabled
        })

        viewModel.approveButtonVisible.observe(this, Observer { approveButtonVisible ->
            approveButton.isVisible = approveButtonVisible
        })

        viewModel.coinSending.observe(this, Observer { coinSending ->
            fromAmount.setSelectedCoin(coinSending?.code)
        })

        viewModel.coinReceiving.observe(this, Observer { coinReceiving ->
            toAmount.setSelectedCoin(coinReceiving?.code)
        })

        viewModel.amountSending.observe(this, Observer { amountSending ->
            setAmountSendingIfChanged(amountSending)
        })

        viewModel.amountReceiving.observe(this, Observer { amountReceiving ->
            setAmountReceivingIfChanged(amountReceiving)
        })

        viewModel.balance.observe(this, Observer { balance ->
            availableBalanceValue.text = balance
        })

        viewModel.amountSendingError.observe(this, Observer { amountSendingError ->
            fromAmount.setError(amountSendingError)
        })

        viewModel.amountSendingLabelVisible.observe(this, Observer { isVisible ->
            fromAmountLabel.isVisible = isVisible
        })

        viewModel.amountReceivingLabelVisible.observe(this, Observer { isVisible ->
            toAmountLabel.isVisible = isVisible
        })

        viewModel.tradeViewItem.observe(this, Observer { tradeViewItem ->
            setTradeViewItem(tradeViewItem)
        })

        viewModel.tradeViewItemLoading.observe(this, Observer { isLoading ->
            tradeViewItemProgressBar.isVisible = isLoading
        })

        viewModel.allowance.observe(this, Observer { allowance ->
            setAllowance(allowance)
        })

        viewModel.allowanceLoading.observe(this, Observer { isLoading ->
            setAllowanceLoading(isLoading)
        })

        viewModel.allowanceColor.observe(this, Observer { color ->
            allowanceValue.setTextColor(color)
        })

        viewModel.priceImpactColor.observe(this, Observer { color ->
            priceImpactValue.setTextColor(color)
        })

        viewModel.error.observe(this, Observer { error ->
            commonError.text =  error
            commonError.isVisible = error.isNotBlank()
        })

    }

    private fun setAllowance(allowance: String) {
        allowanceValue.text = allowance
        val isVisible = allowance.isNotBlank()
        allowanceTitle.isVisible = isVisible
        allowanceValue.isVisible = isVisible
    }

    private fun setAllowanceLoading(isLoading: Boolean) {
        allowanceTitle.isVisible = allowanceTitle.isVisible || isLoading
        allowanceProgressBar.isVisible = isLoading
        allowanceValue.isVisible = !isLoading
    }

    private fun setTradeViewItem(tradeViewItem: TradeViewItem) {
        priceValue.text = tradeViewItem.price
        priceImpactValue.text = tradeViewItem.priceImpact
        minMaxTitle.text = tradeViewItem.minMaxTitle
        minMaxValue.text = tradeViewItem.minMaxAmount

        setTradeViewItemVisibility(visible = !tradeViewItem.isEmpty)
    }

    private fun setTradeViewItemVisibility(visible: Boolean) {
        priceTitle.isVisible = visible
        priceValue.isVisible = visible
        priceImpactTitle.isVisible = visible
        priceImpactValue.isVisible = visible
        minMaxTitle.isVisible = visible
        minMaxValue.isVisible = visible
    }

    private val fromAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.setAmountSending(s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private val toAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.setAmountReceiving(s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private fun setAmountSendingIfChanged(amount: String) {
        fromAmount.editText.apply {
            if (text.toString().toBigDecimalOrNull() == amount.toBigDecimalOrNull()) return
            if (text.isNullOrEmpty() && amount == "0") return

            removeTextChangedListener(fromAmountListener)
            setText(amount)
            addTextChangedListener(fromAmountListener)
        }
    }

    private fun setAmountReceivingIfChanged(amount: String) {
        toAmount.editText.apply {
            if (text.toString().toBigDecimalOrNull() == amount.toBigDecimalOrNull()) return
            if (text.isNullOrEmpty() && amount == "0") return

            removeTextChangedListener(toAmountListener)
            setText(amount)
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
                requestSelectFromCoin -> viewModel.setCoinSending(selectedCoin)
                requestSelectToCoin -> viewModel.setCoinReceiving(selectedCoin)
            }
        }
    }

    companion object {
        const val requestSelectFromCoin = 0
        const val requestSelectToCoin = 1
    }

}
