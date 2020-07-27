package io.horizontalsystems.bankwallet.modules.swap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinModule
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

        val tokenIn = intent.extras?.getParcelable<Coin>(SwapModule.tokenInKey)
        viewModel = ViewModelProvider(this, SwapModule.Factory(tokenIn)).get(SwapViewModel::class.java)

        fromAmount.onTokenButtonClick {
            SelectSwapCoinModule.start(this, requestSelectFromCoin, true, viewModel.toCoinLiveData.value)
        }

        toAmount.onTokenButtonClick {
            SelectSwapCoinModule.start(this, requestSelectToCoin, false, viewModel.fromCoinLiveData.value)
        }

        viewModel.fromCoinLiveData.observe(this, Observer { coin ->
            coin?.let {
                fromAmount.setSelectedCoin(it.code)
            }
        })

        viewModel.toCoinLiveData.observe(this, Observer { coin ->
            coin?.let {
                toAmount.setSelectedCoin(it.code)
            }
        })
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
