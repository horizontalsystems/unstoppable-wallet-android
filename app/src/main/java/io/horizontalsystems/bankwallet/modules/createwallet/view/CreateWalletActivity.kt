package io.horizontalsystems.bankwallet.modules.createwallet.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.PresentationMode
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsModule
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsWrapped
import io.horizontalsystems.bankwallet.modules.coinsettings.SettingsMode
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletModule
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletPresenter
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletRouter
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletView
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.dialogs.AlertDialogFragment
import kotlinx.android.synthetic.main.activity_create_wallet.*

class CreateWalletActivity : BaseActivity(), CoinItemsAdapter.Listener {
    private lateinit var presenter: CreateWalletPresenter
    private lateinit var coinItemsAdapter: CoinItemsAdapter
    private var buttonEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val presentationMode: PresentationMode = intent.getParcelableExtra(ModuleField.PRESENTATION_MODE)
                ?: PresentationMode.Initial
        val predefinedAccountTypeString = intent.extras?.getString(ModuleField.PREDEFINED_ACCOUNT_TYPE)
        val predefinedAccountType = PredefinedAccountType.fromString(predefinedAccountTypeString)

        presenter = ViewModelProvider(this, CreateWalletModule.Factory(presentationMode, predefinedAccountType)).get(CreateWalletPresenter::class.java)

        observeView(presenter.view as CreateWalletView)
        observeRouter(presenter.router as CreateWalletRouter)

        coinItemsAdapter = CoinItemsAdapter(this)
        coins.adapter = coinItemsAdapter

        presenter.onLoad()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.create_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menuCreate)?.apply {
            isEnabled = buttonEnabled
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menuCreate ->  {
                presenter.onCreateButtonClick()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ModuleCode.COIN_SETTINGS -> {
                if (resultCode == Activity.RESULT_CANCELED) {
                    presenter.onCancelSelectingCoinSettings()
                } else if (resultCode == Activity.RESULT_OK && data != null) {
                    val coin = data.getParcelableExtra<Coin>(ModuleField.COIN) ?: return
                    val coinSettings = data.getParcelableExtra<CoinSettingsWrapped>(ModuleField.COIN_SETTINGS)
                            ?: return

                    presenter.onSelectCoinSettings(coinSettings.settings, coin)
                }
            }
        }
    }

    // CoinItemsAdapter.Listener

    override fun enable(coin: Coin) {
        presenter.onEnable(coin)
    }

    override fun disable(coin: Coin) {
        presenter.onDisable(coin)
    }

    override fun select(coin: Coin) {
        presenter.onSelect(coin)
    }

    private fun observeView(view: CreateWalletView) {
        view.coinsLiveData.observe(this, Observer { coins ->
            coinItemsAdapter.viewItems = coins
            coinItemsAdapter.notifyDataSetChanged()
        })

        view.createButtonEnabled.observe(this, Observer { enabled ->
            buttonEnabled = enabled
            invalidateOptionsMenu()
        })

        view.showNotSupported.observe(this, Observer {predefinedAccountType ->
            AlertDialogFragment.newInstance(
                    getString(R.string.ManageCoins_Alert_CantCreateTitle, getString(predefinedAccountType.title)),
                    getString(R.string.ManageCoins_Alert_CantCreateDescription, getString(predefinedAccountType.title)),
                    R.string.Alert_Ok
            ).show(supportFragmentManager, "alert_dialog")
        })
    }

    private fun observeRouter(router: CreateWalletRouter) {
        router.startMainModuleLiveEvent.observe(this, Observer {
            MainModule.startAsNewTask(this)
            finish()
        })
        router.showCoinSettings.observe(this, Observer { (coin, coinSettings) ->
            CoinSettingsModule.startForResult(coin, coinSettings, SettingsMode.Creating, this)
        })
        router.close.observe(this, Observer {
            finish()
        })
    }
}
