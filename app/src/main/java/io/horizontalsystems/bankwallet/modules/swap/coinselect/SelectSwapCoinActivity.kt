package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import kotlinx.android.synthetic.main.activity_swap_select_token.*

class SelectSwapCoinActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap_select_token)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.SwapCoinSelect_Title)
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
