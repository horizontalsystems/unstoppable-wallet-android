package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.activity_swap_select_token.*

class SelectSwapCoinActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap_select_token)

        val excludedCoin = intent.extras?.getParcelable<Coin>(SelectSwapCoinModule.excludedCoinKey)
        val hideZeroBalance = intent.extras?.getBoolean(SelectSwapCoinModule.hideZeroBalanceKey)
        val viewModel by viewModels<SelectSwapCoinViewModel> { SelectSwapCoinModule.Factory(excludedCoin, hideZeroBalance) }

        val adapter = SelectSwapCoinAdapter(onClickItem = { coinItem ->
            setResult(RESULT_OK, Intent().apply {
                putExtra(SelectSwapCoinModule.selectedCoinKey, coinItem.coin)
            })
            finish()
        })

        shadowlessToolbar.bind(
                getString(R.string.ManageCoins_title),
                rightBtnItem = TopMenuItem(text = R.string.Button_Close, onClick = {
                    finish()
                })
        )

        searchView.bind(
                hint = getString(R.string.ManageCoins_Search),
                onTextChanged = { query ->
                    viewModel.updateFilter(query)
                })

        recyclerView.adapter = adapter

        viewModel.coinItemsLivedData.observe(this, Observer { items ->
            adapter.items = items
            adapter.notifyDataSetChanged()
        })

    }

}
