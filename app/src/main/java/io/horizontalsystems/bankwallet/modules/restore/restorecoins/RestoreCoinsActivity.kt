package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinItemsAdapter
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.select_coins.*

class RestoreCoinsActivity : BaseActivity(), CoinItemsAdapter.Listener {
    private lateinit var presenter: RestoreCoinsPresenter
    private lateinit var coinItemsAdapter: CoinItemsAdapter
    private var buttonEnabled = false
    private var proceedButtonTitle: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_coins)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val predefinedAccountType: PredefinedAccountType = intent.getParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE)
                ?: run { finish(); return }

        presenter = ViewModelProvider(this, RestoreCoinsModule.Factory(predefinedAccountType)).get(RestoreCoinsPresenter::class.java)

        observeView(presenter.view as RestoreCoinsView)
        observeRouter(presenter.router as RestoreCoinsRouter)

        coinItemsAdapter = CoinItemsAdapter(this)
        coins.adapter = coinItemsAdapter

        presenter.onLoad()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restore_coins_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menuRestore)?.apply {
            proceedButtonTitle?.let {
                setTitle(it)
            }
            isEnabled = buttonEnabled
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuRestore -> {
                presenter.onProceedButtonClick()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //CoinItemsAdapter.Listener

    override fun enable(coin: Coin) {
        presenter.onEnable(coin)
    }

    override fun disable(coin: Coin) {
        presenter.onDisable(coin)
    }

    override fun select(coin: Coin) {
        //not used here
    }

    private fun observeView(view: RestoreCoinsView) {
        view.coinsLiveData.observe(this, Observer { viewItems ->
            coinItemsAdapter.viewItems = viewItems
            coinItemsAdapter.notifyDataSetChanged()
        })

        view.proceedButtonEnabled.observe(this, Observer { enabled ->
            buttonEnabled = enabled
            invalidateOptionsMenu()
        })

        view.showDerivationSelectorDialog.observe(this, Observer { (items, selected, coin) ->
            BottomSheetSelectorDialog.show(
                    supportFragmentManager,
                    getString(R.string.AddressFormatSettings_Title),
                    coin.title,
                    LayoutHelper.getCoinDrawableResource(this, coin.code),
                    items.map { derivation -> Pair(derivation.longTitle(), getString(derivation.description())) },
                    items.indexOf(selected),
                    onItemSelected = { position ->
                        presenter.onSelectDerivationSetting(coin, items[position])
                    },
                    onCancelled = {
                        presenter.onCancelDerivationSelectorDialog(coin)
                    }
            )
        })

    }

    private fun observeRouter(router: RestoreCoinsRouter) {
        router.closeWithSelectedCoins.observe(this, Observer { coins ->
            setResult(RESULT_OK, Intent().apply {
                putParcelableArrayListExtra(ModuleField.COINS, ArrayList(coins))
            })
            finish()
        })
    }

}
