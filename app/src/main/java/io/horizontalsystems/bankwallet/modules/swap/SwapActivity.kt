package io.horizontalsystems.bankwallet.modules.swap

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinModule
import kotlinx.android.synthetic.main.activity_swap.*

class SwapActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(getDrawable(R.drawable.ic_info))
        supportActionBar?.title = getString(R.string.Swap_Title)

        val tokenIn = intent.extras?.getParcelable<Coin>(SwapModule.tokenInKey)
        val viewModel by viewModels<SwapViewModel> { SwapModule.Factory(tokenIn) }

        fromAmount.onTokenButtonClick {
            SelectSwapCoinModule.start(this, viewModel.tokenIn)
        }

        toAmount.onTokenButtonClick {
            SelectSwapCoinModule.start(this, viewModel.tokenOut)
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

}
