package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.base.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.PresentationMode
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinItemsAdapter
import io.horizontalsystems.bankwallet.modules.main.MainModule
import kotlinx.android.synthetic.main.activity_create_wallet.*

class RestoreCoinsActivity : BaseActivity(), CoinItemsAdapter.Listener {
    private lateinit var presenter: RestoreCoinsPresenter
    private lateinit var coinItemsAdapter: CoinItemsAdapter
    private var buttonEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_coins)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val presentationMode: PresentationMode = intent.getParcelableExtra(ModuleField.PRESENTATION_MODE) ?: PresentationMode.Initial
        val predefinedAccountType: PredefinedAccountType? = intent.getParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE)
        val accountType: AccountType? = intent.getParcelableExtra(ModuleField.ACCOUNT_TYPE)

        if (predefinedAccountType != null && accountType != null) {
            presenter = ViewModelProvider(this, RestoreCoinsModule.Factory(presentationMode, predefinedAccountType, accountType)).get(RestoreCoinsPresenter::class.java)
        } else {
            //predefinedAccountType and accountTyoe must not be null
            finish()
        }

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
        menu?.findItem(R.id.menuNext)?.apply {
            isEnabled = buttonEnabled
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuNext -> {
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
        view.coinsLiveData.observe(this, Observer {viewItems ->
            coinItemsAdapter.viewItems = viewItems
            coinItemsAdapter.notifyDataSetChanged()
        })

        view.proceedButtonEnabled.observe(this, Observer { enabled ->
            buttonEnabled = enabled
            invalidateOptionsMenu()
        })

    }

    private fun observeRouter(router: RestoreCoinsRouter) {
        router.startMainModuleLiveEvent.observe(this, Observer {
            MainModule.startAsNewTask(this)
        })

        router.close.observe(this, Observer {
            finish()
        })
    }
}
