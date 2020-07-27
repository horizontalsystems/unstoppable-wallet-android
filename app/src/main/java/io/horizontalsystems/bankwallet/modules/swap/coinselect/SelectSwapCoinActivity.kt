package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Coin
import kotlinx.android.synthetic.main.activity_swap_select_token.*

class SelectSwapCoinActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap_select_token)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.SwapCoinSelect_Title)

        val excludedCoin = intent.extras?.getParcelable<Coin>(SelectSwapCoinModule.excludedCoinKey)
        val hideZeroBalance = intent.extras?.getBoolean(SelectSwapCoinModule.hideZeroBalanceKey)
        val viewModel by viewModels<SelectSwapCoinViewModel> { SelectSwapCoinModule.Factory(excludedCoin, hideZeroBalance) }

        val adapter = SelectSwapCoinAdapter(onClickItem = { coinItem ->
            setResult(RESULT_OK, Intent().apply {
                putExtra(SelectSwapCoinModule.selectedCoinKey, coinItem.coin)
            })
            finish()
        })

        recyclerCoins.adapter = adapter

        viewModel.coinItemsLivedData.observe(this, Observer { items ->
            adapter.items = items
            adapter.notifyDataSetChanged()
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.select_coin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuClose -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
